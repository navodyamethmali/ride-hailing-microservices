package com.rideshare.matchingservice.service;

import com.rideshare.matchingservice.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {
    private final MatchingService matchingService;

    /*
    * listen to ride.requested kafka topic
    * triggered when ride service published a new ride request
    *
    * flow :
    * ride service -> kafka (ride.requested)  ->this consumer -> matching service
    * */

    @KafkaListener(
            topics = "ride.requested",
            groupId = "matching-service-group"
    )
    public void consumeRideRequestEvent(RideRequestedEvent event){
        try {
            matchingService.matchDriverForRide(event);
        } catch (Exception e) {
            log.error("Error occurred while consuming ride request event", e.getMessage());

            // in production : send a dead letter queue for retry
        }
    }


}
