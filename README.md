# Ride-Hailing Microservices

A scalable, event-driven ride-hailing backend built with Spring Boot, Redis, and Apache Kafka.

This project explores how a ride-hailing platform can receive frequent driver GPS updates, find nearby drivers efficiently, and manage the ride lifecycle using a microservices architecture.

## The Problem

Imagine a ride-hailing platform with 500 drivers.

- Every driver sends their GPS location to the backend every 3 seconds.
- Customers request rides from their current locations.
- The backend must find nearby available drivers quickly.
- Driver locations continuously change as they move.

With 500 drivers sending updates every 3 seconds, the system receives approximately **167 location updates per second**, or **180,000 updates in 15 minutes**.

Storing every location update as a new MySQL record and repeatedly calculating distances across a large table can create unnecessary database load.

## The Solution

This project separates location tracking, driver matching, and ride management into independent services.

- **Redis:** Stores frequently updated driver locations and supports geospatial proximity searches.
- **Apache Kafka:** Supports asynchronous communication through events such as `ride.requested`.
- **Spring Boot:** Provides the backend services and REST APIs.
- **MySQL:** Stores persistent business data where required.

Redis geospatial commands such as `GEOADD` and `GEOSEARCH` support location updates and nearby-location queries. See the [Redis geospatial documentation](https://redis.io/docs/latest/develop/data-types/geospatial/).

## Architecture

```text
                    Customer / Driver
                           |
                           v
                      API Gateway
                           |
           +---------------+---------------+
           |               |               |
           v               v               v
    Location Service  Matching Service  Ride Service
           |               |               |
           v               |               v
         Redis             |             MySQL
           ^               |
           |               v
           +--------- Kafka / Events
                    ride.requested
```

The diagram represents the intended service responsibilities. The exact event flow depends on the implementation.

## Microservices

### 1. Location Service

Receives driver GPS coordinates and updates their latest locations in Redis.

Responsibilities:
- Accept driver latitude and longitude updates.
- Update driver locations approximately every 3 seconds when new GPS data arrives.
- Store locations in a Redis geospatial index.
- Provide location-related APIs.
- Support nearby-driver queries where implemented.

Directory: `location-service/`

### 2. Matching Service

Finds suitable drivers for a customer's ride request.

Responsibilities:
- Consume ride-request events when configured.
- Search for nearby drivers using location data.
- Rank candidate drivers by proximity.
- Apply driver availability and other matching rules.
- Return or publish matching results according to the implemented event flow.

Directory: `matching-service/`

### 3. Ride Service

Manages the ride lifecycle.

Responsibilities:
- Handle ride requests.
- Track ride status.
- Coordinate ride assignment and lifecycle transitions.
- Publish or consume ride events where implemented.
- Persist ride records using the configured database.

Directory: `ride-service/`

## Technology Stack

- Java
- Spring Boot
- Spring Web / REST APIs
- Redis Geospatial
- Apache Kafka
- MySQL
- Docker and Docker Compose
- Maven

The exact dependencies and infrastructure should match each service's configuration.

## Getting Started

### Prerequisites

Install the following:

- JDK version required by the project (Java 17 if configured in Spring Initializr)
- Maven, or use the project's Maven Wrapper if available
- Docker Desktop with Docker Compose
- Git

### 1. Clone the repository

```bash
git clone <YOUR_REPOSITORY_URL>
cd ride-hailing-microservices
```

### 2. Start infrastructure

Review `docker-compose.yml` and start the configured infrastructure:

```bash
docker compose up -d
```

Check container status:

```bash
docker compose ps
```

### 3. Configure the services

Check each service's `application.properties` or `application.yml`.

Ensure that Redis, Kafka, and MySQL connection settings match the ports, hostnames, credentials, and database configuration in Docker Compose.

When running a service inside Docker Compose, use the appropriate service hostname rather than `localhost` to reach another container.

### 4. Run the microservices

Open each service in IntelliJ IDEA and run its Spring Boot application, or build it with Maven:

```bash
mvn clean package
```

If a service has a Maven Wrapper, you can use `./mvnw clean package` on Unix-like shells or `mvnw.cmd clean package` on Windows.

Start the services using the configuration and startup order required by the project.

## Example Workflow

1. A driver sends their current GPS coordinates to the Location Service.
2. The Location Service updates the driver's location in Redis.
3. A customer requests a ride.
4. The ride request is passed to the matching workflow.
5. The Matching Service searches for nearby drivers.
6. The matching workflow selects a suitable available driver.
7. The Ride Service manages the ride assignment and subsequent ride status changes.

## Project Structure

```text
ride-hailing-microservices/
├── location-service/
├── matching-service/
├── ride-service/
├── docker-compose.yml
└── README.md
```

An API gateway may be included as a separate service or configured externally as the project evolves.

## Learning Objectives

- Design a Spring Boot microservices architecture.
- Handle high-frequency GPS location updates.
- Use Redis for geospatial indexing and proximity search.
- Understand asynchronous event-driven communication with Kafka.
- Separate location tracking, driver matching, and ride management.
- Run supporting infrastructure using Docker Compose.
- Explore the challenges of building scalable, location-aware backend systems.

## Future Improvements

- Driver availability and heartbeat tracking.
- Expiration of stale driver locations.
- Reliable Kafka event processing and retry handling.
- Ride assignment locking to prevent assigning one driver to multiple rides.
- Authentication and authorization.
- Automated unit and integration tests.
- Monitoring, logging, and distributed tracing.
- Load testing with hundreds or thousands of simulated drivers.

## Project Status

This project is being developed incrementally. Features, API endpoints, event flows, and deployment configuration should be verified against the current implementation.
