package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.client.LocationServiceClient;
import com.rideshare.matchingservice.dto.NearByDriverResponse;
import com.rideshare.matchingservice.event.RideMatchedEvent;
import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {
    private final LocationServiceClient locationServiceClient;
    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";
    private static final double DEFAULT_SEARCH_RADIUS_KM = 5.0;

    /**
     * Main matching algorithm
     * called when rideRequest event is consumed from kafka
     * @param event
     *
     * step1: ask loaction service for nearby drivers
     * step 2: score each driver and pick the best one to match with the ride
     */


    public void matchDriverForRide(RideRequestedEvent event){
        log.info("Received ride request event: {}", event);

        List<NearByDriverResponse> nearbyDrivers = locationServiceClient.getNearByDrivers(
                event.getPickupLatitude(),
                event.getPickupLongitude(),
                DEFAULT_SEARCH_RADIUS_KM
        );

        if (nearbyDrivers.isEmpty()) {
            log.warn("No nearby drivers found for ride request: {}", event);
            return;
        }

        //step 2: score each driver and pick the best one to match with the ride
        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearbyDrivers);

        if(bestDriver.isEmpty()){
            log.warn("No suitable driver found for ride request: {}", event);
            return;
        }

        NearByDriverResponse assignedDriver = bestDriver.get();

        //step 3: send the matched event to kafka
        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRideId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKM()
        );
        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId() , matchedEvent);
        log.info("Ride matched published");

    }
    /*
    driver score algorithm
    1. calculate distance between driver and ride request

    distance:70%
    rating:30%

    score = (1/ distance)*disatance weight  + 0.3 * rating

    @param drivers
    @return
    */

    private Optional<NearByDriverResponse> findBestDriver(
            List<NearByDriverResponse> drivers) {
        double distanceWeight = 0.7;
        double ratingWeight = 0.3;

        return drivers.stream()
                .max(Comparator.comparingDouble(driver -> {
                    //distance score: closer = higher score
                    //add 0.1 to avoid division by zero
                    double DistanceScore = (1.0 / (driver.getDistanceInKM() + 0.1));

                    //simulated rating between 4.0 and 5.0
                    // in production: fetch from driver service
                    double simulatedRating = 4.0 +  Math.random();

                    //final score
                    return (DistanceScore * distanceWeight)
                            + (simulatedRating * ratingWeight);

                }));
    }

}
