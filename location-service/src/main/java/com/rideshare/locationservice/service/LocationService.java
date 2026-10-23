package com.rideshare.locationservice.service;

import com.rideshare.locationservice.dto.DriverLocationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import org.springframework.data.geo.Point;

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationService {

    private final RedisTemplate<String, String> redisTemplate;
    //redis key for all driver locations
    private static final String DRIVER_GEO_KEY = "driverS:lcations";

    //update driver location in redis
    // every 3 seconds,
    // the driver phone calls this API to update its location
    //maps to redis command

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest){
        log.info("Updating location for driver : {}", driverLocationRequest.getDriverId());
        // Implementation for updating driver location in Redis
        //important: latitude first, longitude second - geo Spatial Standed
        Point driverPoint = new Point(
                driverLocationRequest.getLatitude(),
                driverLocationRequest.getLongitude()
        );

        //opsForGeo methods provide access to all geo commands
        redisTemplate.opsForGeo().add(
                DRIVER_GEO_KEY,
                driverPoint,
                driverLocationRequest.getDriverId()
        );
        log.info("Driver location updated successfully for driver : {}", driverLocationRequest.getDriverId());

    }


}
