package com.rideshare.rideservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequest {
    @NotBlank( message = "Rider id is required")
    private String riderId;

    @NotNull( message = "Pickup location is required")
    private Double pickupLatitude;

    @NotNull( message = "Pickup location is required")
    private Double pickupLongitude;

    @NotNull( message = "Pickup address is required")
    private String pickupAddress;

    @NotNull( message = "Drop location is required")
    private Double dropLatitude;

    @NotNull( message = "Drop location is required")
    private Double dropLongitude;

    @NotNull( message = "Drop address is required")
    private String dropAddress;
}
