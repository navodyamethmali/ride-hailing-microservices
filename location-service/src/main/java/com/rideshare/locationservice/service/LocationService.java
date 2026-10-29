package com.rideshare.locationservice.service;

import com.rideshare.locationservice.dto.DriverLocationRequest;
import com.rideshare.locationservice.dto.NearByDriverResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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

    /**
     *  Find nearby drivers within a specified radius from the given latitude and longitude.
     *  This method is called by the matching service when a ride is requested.
     *  Maps to redis command: GEORADIUS
     */
    public List<NearByDriverResponse> findNearByDrivers(
            double latitude, double longitude, double radiusInKm){
        log.info("Finding drivers within {} KM of {} , {}", radiusInKm, latitude, longitude);

        Circle  searchArea = new Circle(
                new Point(latitude, longitude),
                new Distance(radiusInKm , Metrics.KILOMETERS)
        );
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                redisTemplate.opsForGeo().radius(
                        DRIVER_GEO_KEY,
                        searchArea,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance()
                                .sortAscending()
                                .limit(10)
                );
        // Convert radius to radians
        List<NearByDriverResponse> nearbyDrivers = new ArrayList<>();
        if (results != null) {
            results.forEach(result -> {
                RedisGeoCommands.GeoLocation<String> location = result.getContent();
                nearbyDrivers.add(new NearByDriverResponse(
                        location.getName(),
                        location.getPoint().getX(),
                        location.getPoint().getY(),
                        result.getDistance().getValue()
                ));
            });
        }
        log.info("Found {} drivers within {} KM of {} , {}", nearbyDrivers.size(), radiusInKm, latitude, longitude);
        return nearbyDrivers;
    }

    /* remove driver when they are offline
     * maps to redis command: ZREM
     * */
    public void removeDriver(String driverId) {
        log.info("Removing driver: {}", driverId);
        redisTemplate.opsForGeo().remove(DRIVER_GEO_KEY, driverId);
        log.info("Driver removed successfully: {}", driverId);
    }


}
