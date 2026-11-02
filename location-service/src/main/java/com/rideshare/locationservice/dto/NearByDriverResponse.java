// send out data(including distance) to the macting service
package com.rideshare.locationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NearByDriverResponse {
    private String driverId;
    private double longitude;
    private double latitude;
    private double distanceInKM;
}
