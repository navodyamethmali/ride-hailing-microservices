package com.rideshare.rideservice.service;

import com.rideshare.rideservice.dto.RideRequest;
import com.rideshare.rideservice.dto.RideResponse;
import com.rideshare.rideservice.event.RideRequestEvent;
import com.rideshare.rideservice.model.Ride;
import com.rideshare.rideservice.model.RideStatus;
import com.rideshare.rideservice.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideService {
    private final RideRepository rideRepository;
    private final KafkaTemplate<String, RideRequestEvent> kafkaTemplate;

    private static final String RIDE_REQUEST_TOPIC = "ride.requested";

    /*
    * create a ride in DB with REQUESTED status
    * send a ride request event to Kafka
    * */

    public RideResponse requestRide(RideRequest request) {
        log.info("Received new ride request event from rider: {}", request.getRiderId());

        //step 1: save ride to a database
        Ride ride = new Ride();
        ride.setRiderId(request.getRiderId());
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setPickupAddress(request.getPickupAddress());
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setEstimateFare(calculateEstimateFare(request));

        Ride savedRide = rideRepository.save(ride);


        //step 2: send ride request event to Kafka
        // matching service will consume this and find nearest driver
        RideRequestEvent event = new RideRequestEvent(
                savedRide.getId(),
                savedRide.getRiderId(),
                savedRide.getPickupLatitude(),
                savedRide.getPickupLongitude(),
                savedRide.getPickupAddress(),
                savedRide.getDropLatitude(),
                savedRide.getDropLongitude(),
                savedRide.getDropAddress()
        );

        kafkaTemplate.send(RIDE_REQUEST_TOPIC,savedRide.getId(), event);
        log.info("Ride request event sent to Kafka for ride: {}", savedRide.getId());

        savedRide.setStatus(RideStatus.MATCHING);
        rideRepository.save(savedRide);

        return mapToRideResponse(savedRide);
    }

    //to update the status of the ride
    public void updateRideWithDriver (String rideId, String driverId){
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        ride.setDriverId(driverId);
        ride.setStatus(RideStatus.ACCEPTED);
        rideRepository.save(ride);
    }

    public RideResponse startRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        if(ride.getStatus() != RideStatus.ACCEPTED){
            throw new RuntimeException("Ride cannot be started. Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.RIDE_STARTED);
        ride.setStartedAt(LocalDateTime.now());
        rideRepository.save(ride);
        return mapToRideResponse(ride);
    }

    public RideResponse completeRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        if(ride.getStatus() != RideStatus.RIDE_STARTED){
            throw new RuntimeException("Ride cannot be completed. Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.COMPLETED);
        ride.setCompletedAt(LocalDateTime.now());
        ride.setActualFare(ride.getEstimateFare());
        rideRepository.save(ride);
        return mapToRideResponse(ride);
    }

    public RideResponse cancelRide(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        if(ride.getStatus() != RideStatus.ACCEPTED){
            throw new RuntimeException("Ride cannot be cancelled. Current status: " + ride.getStatus());
        }
        ride.setStatus(RideStatus.CANCELLED);
        rideRepository.save(ride);
        return mapToRideResponse(ride);
    }

    public RideResponse getRideById(String rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new RuntimeException("Ride not found"));
        return mapToRideResponse(ride);
    }

    public List<RideResponse> getRidesByRider(String riderId) {
        return rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId)
                .stream()
                .map(this::mapToRideResponse)
                .collect(Collectors.toList());
    }

    private double calculateEstimateFare(RideRequest request) {
        //simplified Haversine distance calculation
        double lat1 = Math.toRadians(request.getPickupLatitude());
        double lat2= Math.toRadians(request.getDropLatitude());

        double lon1 = Math.toRadians(request.getPickupLongitude());
        double lon2 = Math.toRadians(request.getDropLongitude());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.pow(Math.sin(dLat / 2),2)
                *  Math.cos(lat1) * Math.cos(lat2)
                * Math.pow(Math.sin(dLon / 2) ,2);

        double c = 2 * Math.asin(Math.sqrt(a));

        double distance = 6371 * c; // Radius of earth in kilometers

        //base fare = 50Rs + 12Rs per km
        double fare = 50 + (12 * distance);
        return Math.round(fare * 100) / 100.0;
    }


    private RideResponse mapToRideResponse(Ride ride) {
        RideResponse response = new RideResponse();
        response.setId(ride.getId());
        response.setRideId(ride.getId());
        response.setDriverId(ride.getDriverId());
        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setPickupAddress(ride.getPickupAddress());
        response.setDropLatitude(ride.getDropLatitude());
        response.setDropLongitude(ride.getDropLongitude());
        response.setDropAddress(ride.getDropAddress());
        response.setStatus(ride.getStatus());
        response.setEstimateFare(ride.getEstimateFare());
        response.setActualFare(ride.getActualFare());
        response.setCreatedAt(ride.getCreatedAt());
        response.setStartedAt(ride.getStartedAt());
        response.setCompletedAt(ride.getCompletedAt());
        return response;
    }
}
