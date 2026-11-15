package com.rideshare.rideservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@RequiredArgsConstructor
public class Ride {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    //who requested the ride
    @Column(nullable = false)
    private String riderId;

    //who accepted the ride
    @Column(nullable = false)
    private String driverId;

    @Column(nullable = false)
    private Double pickupLatitude;

    @Column(nullable = false)
    private Double pickupLongitude;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private Double dropLatitude;

    @Column(nullable = false)
    private Double dropLongitude;

    @Column(nullable = false)
    private String dropAddress;

    //ride status= track the lifecycle of the ride
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RideStatus status;

    //fare details
    private Double estimateFare;

    private Double actualFare;

    //timestamps
    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;


}
