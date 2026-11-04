package com.rideshare.rideservice.event;

/*
* Event published to kafka when a ride request is received
* maching service consumes this event
*TOPICS: ride-request
* */

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestEvent {
    private String rideId;
    private String riderId;

    //pickup and drop locations
    private Double pickupLatitude;
    private Double pickupLongitude;
    private String pickupAddress;

    private Double dropLatitude;
    private Double dropLongitude;
    private String dropAddress;
}
