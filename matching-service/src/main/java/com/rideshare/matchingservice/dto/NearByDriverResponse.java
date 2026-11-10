package com.rideshare.matchingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response received from location service when matching service needs to find
 * when querying for nearby drivers.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearByDriverResponse {
    private String driverId;
    private double longitude;
    private double latitude;
    private double distanceInKM;
}
