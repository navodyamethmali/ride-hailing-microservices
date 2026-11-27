# Ride-Hailing Microservices 

An event-driven ride-hailing backend built with **Spring Boot**, **Redis Geospatial**, **Apache Kafka**, **MySQL**, and **Spring Cloud OpenFeign**.

The platform separates high-frequency driver GPS tracking, nearby-driver matching, and ride lifecycle persistence into three independent microservices communicating over REST (OpenFeign) and Apache Kafka.

---

## Table of Contents

- [Architecture & Event Flow](#architecture--event-flow)
- [Microservices Overview](#microservices-overview)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [1. Start Infrastructure (Docker Compose)](#1-start-infrastructure-docker-compose)
  - [2. Create the MySQL Database](#2-create-the-mysql-database)
  - [3. Run the Microservices](#3-run-the-microservices)
- [REST API Reference](#rest-api-reference)
  - [Location Service (`:8082`)](#location-service-8082)
  - [Ride Service (`:8083`)](#ride-service-8083)
  - [Matching Service (`:8084`)](#matching-service-8084)
- [Kafka Topics & Events](#kafka-topics--events)
- [Ride Lifecycle & Matching Logic](#ride-lifecycle--matching-logic)
- [End-to-End Verification (`curl`)](#end-to-end-verification-curl)
- [Current Scope & Future Improvements](#current-scope--future-improvements)

---

## Architecture & Event Flow

```text
  Driver Client                                         Rider Client
        |                                                     |
        | POST /api/v1/locations/driver/update                | POST /api/v1/rides/request
        v                                                     v
+-------------------+     GET /drivers/nearby         +------------------+
| Location Service  | <------- (OpenFeign) ---------- |   Ride Service   |
|    (port 8082)    |                                 |   (port 8083)    |
+-------------------+                                 +------------------+
        |                                               |       ^      |
        | GEOADD / GEORADIUS / ZREM                     |       |      | JPA / Hibernate
        v                                ride.requested |       |      v
+-------------------+                                   |       |  +---------+
|       Redis       |                                   v       |  |  MySQL  |
| key:              |                        +------------------+  +---------+
| drivers:locations |                        |   Apache Kafka   |
+-------------------+                        +------------------+
                                                |              ^
                                 ride.requested |              | ride.matched
                                                v              |
                                             +--------------------+
                                             |  Matching Service  |
                                             |    (port 8084)     |
                                             +--------------------+
```

> **Note:** Clients currently communicate directly with `location-service` (`8082`) and `ride-service` (`8083`). No API Gateway service is present in this repository yet.

---

## Microservices Overview

| Service | Port | Directory | Database / Store | Role |
|---|---:|---|---|---|
| **Location Service** | `8082` | `location-service/` | Redis (`drivers:locations`) | Stores driver GPS coordinates, removes offline drivers, and queries nearby drivers by radius. |
| **Ride Service** | `8083` | `ride-service/` | MySQL (`uberapp`) | Manages ride creation, fare estimation, ride status transitions, and Kafka `ride.requested` / `ride.matched` integration. |
| **Matching Service** | `8084` | `matching-service/` | Stateless (Kafka + Feign) | Consumes `ride.requested`, fetches nearby drivers from Location Service via OpenFeign, scores candidates, and publishes `ride.matched`. |

---

## Technology Stack

- **Java:** Java 21 for `location-service` (configured with `--enable-preview` in `maven-compiler-plugin`); Java 17 configured in `ride-service` and `matching-service`
- **Spring Boot:** `4.1.1` (`location-service`, `matching-service`), `4.1.2` (`ride-service`)
- **Spring Cloud:** `2025.1.3` (`spring-cloud-starter-openfeign` in `matching-service`)
- **Data & Messaging:**
  - **Redis** (`redis:latest`) with Spring Data Redis Geospatial commands
  - **MySQL** (`mysql:8.0`) with Spring Data JPA / Hibernate (`ddl-auto: update`)
  - **Apache Kafka** & **Zookeeper** (`confluentinc/cp-kafka:7.4.0`, `confluentinc/cp-zookeeper:7.4.0`)
- **Utilities & Observability:** Lombok, Spring Validation, Spring Boot Actuator (`health`, `info`), Jackson, Maven Wrapper (`mvnw` / `mvnw.cmd`)

---

## Project Structure

```text
Uber-app/
├── docker-compose.yml          # Redis, MySQL 8.0, Zookeeper, and Kafka
├── README.md
├── location-service/           # Port 8082 - Redis geospatial driver tracking
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/rideshare/locationservice/
│       │   ├── config/RedisConfig.java
│       │   ├── controller/LocationController.java
│       │   ├── dto/
│       │   └── service/LocationService.java
│       └── resources/application.yaml
├── matching-service/           # Port 8084 - Kafka consumer/producer + Feign matching
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/rideshare/matchingservice/
│       │   ├── client/LocationServiceClient.java
│       │   ├── dto/NearByDriverResponse.java
│       │   ├── event/
│       │   └── service/
│       └── resources/application.yaml
└── ride-service/               # Port 8083 - Ride lifecycle REST API + MySQL + Kafka
    ├── pom.xml
    └── src/main/
        ├── java/com/rideshare/rideservice/
        │   ├── config/KafkaConfig.java
        │   ├── controller/
        │   ├── dto/
        │   ├── event/
        │   ├── model/
        │   ├── repository/RideRepository.java
        │   └── service/
        └── resources/application.yaml
```

---

## Getting Started

### Prerequisites

- **JDK 21+** (required to compile `location-service`; also runs the Java 17 services)
- **Docker Desktop** with Docker Compose
- **Maven** (optional — each service includes `mvnw` / `mvnw.cmd`)

### 1. Start Infrastructure (Docker Compose)

Start Redis, MySQL, Zookeeper, and Kafka from the repository root:

```bash
docker compose up -d
```

Verify that all containers (`redis-geo`, `mysql-rideshare`, `zookeeper`, `kafka`) are running:

```bash
docker compose ps
```

Default exposed ports from `docker-compose.yml`:
- **Redis:** `localhost:6379`
- **MySQL:** `localhost:3306` (`USERNAME` / `PASSWORD`)
- **Zookeeper:** `localhost:2181`
- **Kafka:** `localhost:9092` (external host listener; `kafka:29092` inside the Docker network)

### 2. Create the MySQL Database

`docker-compose.yml` creates a default database named `ride_db`, while `ride-service/src/main/resources/application.yaml` connects to `jdbc:mysql://localhost:3306/uberapp`.

Create the `uberapp` database before starting `ride-service`:

```bash
docker exec -it mysql-rideshare mysql -uroot -p1234 -e "CREATE DATABASE IF NOT EXISTS uberapp;"
```

*(Alternatively, update `spring.datasource.url` in `ride-service/src/main/resources/application.yaml` to point to `ride_db`.)*

### 3. Run the Microservices

Start each service in a separate terminal from its module directory.

**On Windows (PowerShell / CMD):**

```powershell
# Terminal 1 - Location Service (port 8082)
cd location-service
.\mvnw.cmd spring-boot:run

# Terminal 2 - Ride Service (port 8083)
cd ride-service
.\mvnw.cmd spring-boot:run

# Terminal 3 - Matching Service (port 8084)
cd matching-service
.\mvnw.cmd spring-boot:run
```

**On macOS / Linux:**

```bash
# Terminal 1 - Location Service (port 8082)
cd location-service && ./mvnw spring-boot:run

# Terminal 2 - Ride Service (port 8083)
cd ride-service && ./mvnw spring-boot:run

# Terminal 3 - Matching Service (port 8084)
cd matching-service && ./mvnw spring-boot:run
```

To build/package or run tests for a service:

```bash
./mvnw clean test        # or .\mvnw.cmd clean test on Windows
./mvnw clean package     # or .\mvnw.cmd clean package on Windows
```

---

## REST API Reference

### Location Service (`:8082`)

Base path: `/api/v1/locations`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/locations/driver/update` | Adds or updates a driver's GPS position in Redis (`drivers:locations`). |
| `GET` | `/api/v1/locations/drivers/nearby` | Returns up to 10 drivers within `radius` km (default `5.0`), sorted nearest-first. |
| `DELETE` | `/api/v1/locations/driver/{driverId}` | Removes a driver from `drivers:locations` when going offline. |
| `GET` | `/actuator/health`, `/actuator/info` | Spring Boot Actuator endpoints. |

**`POST /api/v1/locations/driver/update`**

```json
{
  "driverId": "driver-101",
  "longitude": 77.5946,
  "latitude": 12.9716
}
```

**`GET /api/v1/locations/drivers/nearby?longitude=77.5946&latitude=12.9716&radius=5.0`**

```json
[
  {
    "driverId": "driver-101",
    "longitude": 77.5946,
    "latitude": 12.9716,
    "distanceInKM": 0.0
  }
]
```

---

### Ride Service (`:8083`)

Base path: `/api/v1/rides`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/rides/request` | Creates a ride (`201 Created`), computes estimated fare, and publishes `ride.requested`. |
| `GET` | `/api/v1/rides/{rideId}` | Fetches a ride by its UUID. |
| `GET` | `/api/v1/rides/rider/{riderId}` | Lists all rides for a rider ordered by `createdAt` descending. |
| `PUT` | `/api/v1/rides/{rideId}/start` | Starts an `ACCEPTED` ride (`ACCEPTED` -> `RIDE_STARTED`). |
| `PUT` | `/api/v1/rides/{rideId}/complete` | Completes a started ride (`RIDE_STARTED` -> `COMPLETED`) and records `actualFare`. |
| `PUT` | `/api/v1/rides/{rideId}/cancel` | Cancels an `ACCEPTED` ride (`ACCEPTED` -> `CANCELLED`). |

**`POST /api/v1/rides/request`** (all fields validated with `@NotBlank` / `@NotNull`)

```json
{
  "riderId": "rider-001",
  "pickupLatitude": 12.9716,
  "pickupLongitude": 77.5946,
  "pickupAddress": "MG Road, Bengaluru",
  "dropLatitude": 12.9352,
  "dropLongitude": 77.6245,
  "dropAddress": "Koramangala, Bengaluru"
}
```

---

### Matching Service (`:8084`)

`matching-service` is an event-driven consumer/producer with no business REST endpoints. It exposes Actuator endpoints:
- `GET /actuator/health`
- `GET /actuator/info`

---

## Kafka Topics & Events

`ride-service` (`KafkaConfig`) declares two topics (3 partitions, 1 replica each):

| Topic | Producer | Consumer (`group-id`) | Event Class / Fields |
|---|---|---|---|
| `ride.requested` | `ride-service` | `matching-service` (`matching-service-group`) | `rideId`, `riderId`, `pickupLatitude`, `pickupLongitude`, `pickupAddress`, `dropLatitude`, `dropLongitude`, `dropAddress` |
| `ride.matched` | `matching-service` | `ride-service` (`ride-service-group`) | `rideId`, `riderId`, `driverId`, `driverLatitude`, `driverLongitude`, `distanceToPickupKm` |

---

## Ride Lifecycle & Matching Logic

1. **Ride Status Enum (`RideStatus`):**
   `REQUESTED` -> `MATCHING` -> `ACCEPTED` -> `RIDE_STARTED` -> `COMPLETED` (or `CANCELLED` from `ACCEPTED`).
   *(Note: `DRIVER_ARRIVING` is also defined in `RideStatus` for future use.)*
2. **Driver Matching (`MatchingService`):**
   - Queries `location-service` within `5.0` km (`DEFAULT_SEARCH_RADIUS_KM`) of the pickup coordinates.
   - Scores each candidate driver using:
     ```text
     score = 0.7 * (1.0 / (distanceInKM + 0.1)) + 0.3 * simulatedRating
     ```
     where `simulatedRating` is currently a placeholder random value in `[4.0, 5.0)`.
3. **Fare Estimation (`RideService`):**
   - Computes an estimated fare (`estimateFare`) from pickup and drop coordinates using a base fare of `50` + `12` per km, and assigns `actualFare = estimateFare` upon completion.

---

## End-to-End Verification (`curl`)

1. **Register a driver's location:**
   ```bash
   curl -X POST http://localhost:8082/api/v1/locations/driver/update \
     -H "Content-Type: application/json" \
     -d '{"driverId":"driver-101","longitude":77.5946,"latitude":12.9716}'
   ```

2. **Check nearby drivers:**
   ```bash
   curl "http://localhost:8082/api/v1/locations/drivers/nearby?longitude=77.5950&latitude=12.9720&radius=5.0"
   ```

3. **Request a ride (copy the returned `id`):**
   ```bash
   curl -X POST http://localhost:8083/api/v1/rides/request \
     -H "Content-Type: application/json" \
     -d '{
       "riderId": "rider-001",
       "pickupLatitude": 12.9716,
       "pickupLongitude": 77.5946,
       "pickupAddress": "MG Road, Bengaluru",
       "dropLatitude": 12.9352,
       "dropLongitude": 77.6245,
       "dropAddress": "Koramangala, Bengaluru"
     }'
   ```

4. **Verify async driver assignment (`status` becomes `ACCEPTED` with `driverId` populated):**
   ```bash
   curl http://localhost:8083/api/v1/rides/<RIDE_ID>
   ```

5. **Start and complete the ride:**
   ```bash
   curl -X PUT http://localhost:8083/api/v1/rides/<RIDE_ID>/start
   curl -X PUT http://localhost:8083/api/v1/rides/<RIDE_ID>/complete
   ```

---

## Current Scope & Future Improvements

- Add an API Gateway (e.g., Spring Cloud Gateway) as a single entry point.
- Replace simulated driver ratings with a persistent Driver Service and track driver availability/locking to prevent concurrent double-assignment.
- Add TTL/expiration for stale driver locations in Redis.
- Add Kafka Dead Letter Queue (DLQ) and retry policies for failed event processing.
- Expand unit and integration tests (e.g., Testcontainers for Redis, MySQL, and Kafka).

