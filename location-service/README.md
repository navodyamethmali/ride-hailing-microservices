# Location Service

The Location Service is responsible for receiving and maintaining the latest GPS locations of drivers in the ride-hailing platform.

It uses Spring Boot to expose location APIs and Redis Geospatial to store and search driver coordinates.

## Why This Service Exists

In a ride-hailing system, drivers continuously move and report their locations.

For example, with 500 active drivers sending updates every 3 seconds, the backend receives approximately 167 updates per second.

Writing every GPS update to a relational database and running repeated distance calculations can create unnecessary database work.

Instead, this service updates the latest driver location in Redis, allowing the system to search for nearby drivers using geospatial commands.

## Responsibilities

- Receive driver GPS coordinates.
- Validate driver identifiers and coordinate values.
- Update the latest location of each driver in Redis.
- Support proximity searches when implemented.
- Expose location-related REST endpoints.
- Provide location data to the matching workflow.
- Handle invalid or unavailable location data appropriately.

## Technology Stack

- Java 17 (if configured for this project)
- Spring Boot
- Spring Web
- Redis
- Redis Geospatial commands
- Docker Compose

## How It Works

```text
Driver Mobile App
       |
       | GPS update every ~3 seconds
       v
Location Service
       |
       | GEOADD
       v
Redis Geospatial Index
       |
       | GEOSEARCH
       v
Nearby Driver Candidates
       |
       v
Matching Service
```

## Redis Data Model

The service can maintain a Redis geospatial index named `drivers`.

Each member represents a driver, and its coordinates represent the driver's latest known location.

Example Redis commands:

```redis
GEOADD drivers 79.8612 6.9271 "driver:1"
GEOADD drivers 79.8560 6.9140 "driver:2"
```

**Important:** Redis expects longitude first and latitude second.

Search for drivers within 5 kilometres of a customer:

```redis
GEOSEARCH drivers FROMLONLAT 79.8600 6.9200 BYRADIUS 5 km ASC COUNT 10 WITHDIST
```

This searches the `drivers` index and returns up to 10 matching members, ordered by distance. Redis geospatial queries are documented in the [official Redis GEOSEARCH reference](https://redis.io/docs/latest/commands/geosearch/).

## Configuration

Check `src/main/resources/application.properties` or `application.yml` for Redis connection settings.

Example:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

These values are examples. Use the configuration appropriate to your environment. When the service runs inside Docker Compose, the Redis hostname will generally be the Compose service name.

## Running the Service

Start Redis using the root Compose file:

```bash
docker compose up -d redis
```

Run the service from its directory:

```bash
cd location-service
mvn spring-boot:run
```

Alternatively, open the service in IntelliJ IDEA and run its Spring Boot application.

## API Documentation

Document the actual endpoints implemented in this service.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | To be confirmed | Submit a driver's location |
| GET | To be confirmed | Retrieve a driver's location or nearby drivers, if implemented |

Check the controller classes before publishing exact endpoint paths or request formats.

## Important Design Considerations

- Update the existing driver's location instead of creating a new geospatial member for every GPS update.
- Validate latitude and longitude.
- Consider how stale locations are detected and removed.
- Treat the latest location as temporary, frequently changing data.
- Keep persistent ride and account records in the appropriate database.
- Avoid treating proximity alone as proof that a driver is available.

## Future Improvements

- Expire stale driver locations.
- Track driver availability separately from GPS coordinates.
- Add request validation and authentication.
- Add integration tests with Redis.
- Measure update latency and throughput.
- Add location update timestamps.

## Project Status

Refer to the implementation for the current endpoints, validation rules, and Redis configuration.
