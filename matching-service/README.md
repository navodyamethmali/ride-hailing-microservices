# Matching Service

The Matching Service identifies suitable drivers for customers requesting a ride.

It is responsible for finding nearby driver candidates and applying matching rules to select an appropriate driver.

## Why This Service Exists

When a customer requests a ride, the backend needs to answer:

**Which available driver is closest to this customer?**

Searching every driver and calculating distances individually can become inefficient as the platform grows.

The Matching Service uses location data from Redis and can use Redis Geospatial queries to retrieve nearby candidates efficiently.

## Responsibilities

- Receive or process ride requests.
- Identify nearby driver candidates.
- Rank candidates by distance.
- Apply driver availability and matching rules.
- Coordinate with the Ride Service to complete assignment.
- Consume or publish Kafka events where configured.
- Handle cases where no suitable driver is found.

## Technology Stack

- Java 17 (if configured for this project)
- Spring Boot
- Apache Kafka
- Redis integration, directly or through the Location Service
- Maven
- Docker Compose

## How It Works

```text
Customer Requests Ride
          |
          v
     Ride Requested
          |
          v
      Kafka Event
          |
          v
    Matching Service
          |
          v
 Find Nearby Candidates
          |
          v
 Check Availability
          |
          v
 Select Suitable Driver
          |
          v
   Ride Assignment
```

This is the intended workflow. The actual Kafka topics and communication paths should match your implementation.

## Example Matching Logic

Suppose three drivers are located near a customer:

| Driver | Distance | Available |
|---|---:|---|
| driver:1 | 0.4 km | Yes |
| driver:2 | 0.8 km | Yes |
| driver:3 | 0.2 km | No |

Although `driver:3` is the closest, it is unavailable. The service should choose `driver:1`, assuming no other matching rules change the result.

This demonstrates why a matching service must consider both distance and availability.

## Example Redis Query

To retrieve nearby candidates, the matching workflow can use:

```redis
GEOSEARCH drivers FROMLONLAT 79.8600 6.9200 BYRADIUS 5 km ASC COUNT 10
```

This returns up to 10 nearby members in ascending distance order.

The query returns candidate driver identifiers, not proof that those drivers are available. Availability must be checked separately.

## Event-Driven Communication

A possible event flow is:

- `ride.requested` — a customer has requested a ride.
- `driver.match.found` — a suitable candidate has been identified.
- `driver.match.failed` — no suitable candidate was found.

These are example event names, not a claim that all three topics already exist in the repository.

## Configuration

Check the service's application configuration for Kafka bootstrap servers, consumer group settings, topic names, and any Redis or Location Service connection details.

Example:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

This is an example for a locally accessible Kafka broker. Use the actual broker address configured for your environment.

## Running the Service

Start the required infrastructure from the root project:

```bash
docker compose up -d
```

Then run the matching service:

```bash
cd matching-service
mvn spring-boot:run
```

Ensure Kafka and any required location dependencies are available before testing the matching workflow.

## Important Design Considerations

- Verify driver availability before assignment.
- Prevent concurrent requests from assigning the same driver to multiple rides.
- Handle duplicate Kafka events safely.
- Define retry and failure-handling policies.
- Set a maximum search radius and expand it when appropriate.
- Handle cases where no drivers are available.
- Keep matching logic separate from ride lifecycle management.

## Future Improvements

- Add a configurable search radius.
- Implement driver ranking beyond distance.
- Add retry and timeout handling.
- Make event processing idempotent.
- Add metrics for match latency and success rate.
- Add integration tests using Kafka and Redis.

## Project Status

Confirm the actual consumer configuration, topic names, matching rules, and assignment mechanism in the source code.
