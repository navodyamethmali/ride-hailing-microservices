package com.rideshare.matchingservice.event;

/*
* event published to kafka topic: ride.matched
* consumed by ride service to update ride with assigned driver
* */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideMatchedEvent {
    private String rideId;
    private String riderId;
    private String driverId;
    private Double driverLatitude;
    private Double driverLongitude;
    private double distanceToPickupKm;

}
