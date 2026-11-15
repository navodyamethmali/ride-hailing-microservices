package com.rideshare.rideservice.model;

/*
* flow of the ride
*  requested -> matched -> accepted -> driver arrived
*            -> ride started -> ride completed
*            -> ride canceled (can happen at multiple stages)
* */
public enum RideStatus {
    REQUESTED,
    MATCHING,
    ACCEPTED,
    DRIVER_ARRIVING,
    RIDE_STARTED,
    COMPLETED,
    CANCELLED
}
