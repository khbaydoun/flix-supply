# flix-supply

Two Spring Boot 3 services that model the supply side of a long-distance mobility network.

| Service | Port | What it does |
|---|---|---|
| `supply-service` | 8080 | Stores brands, mobility partners, lines, rides and stops in Postgres (schema owned by Flyway). Publishes a `RideEvent` to Kafka topic `supply.ride-events` after each ride change commits. |
| `capacity-service` | 8081 | A Kafka Streams app. It reads `supply.ride-events` as a table, adds up the seats of scheduled rides per line, writes the totals to `capacity.line-capacity` and serves them over REST. |

**Brands at line level vs. ride level:** `line.brand_id` is the old way of assigning a brand and `ride.brand_id` is the new one. A ride's *effective brand* is its own brand if it has one, otherwise the line's brand (`Ride#effectiveBrand()`).

## Prerequisites

- JDK 21 (Gradle picks it up through the toolchain; Gradle itself can run on 17+)
- Docker (for Postgres, Kafka and Kafka UI)

## Run

```bash
docker compose up -d --wait            # postgres:5432, kafka:9092, kafka-ui:8090
./gradlew :supply-service:bootRun      # terminal 1
./gradlew :capacity-service:bootRun    # terminal 2
```

The seed rides come from the Flyway migration, so no events are published for them. To load them into capacity-service once:

```bash
curl -X POST localhost:8080/rides/events/replay
```

## API

### supply-service (`:8080`)

```bash
curl localhost:8080/brands
curl localhost:8080/brands/FLIXBUS
curl localhost:8080/partners
curl localhost:8080/lines
curl 'localhost:8080/rides?lineCode=N120&brandCode=FLIXBUS'   # both filters optional
curl localhost:8080/rides/1                                  # includes stops

curl -X POST localhost:8080/rides -H 'Content-Type: application/json' -d '{
  "lineCode": "N120", "partnerId": 1, "brandCode": "KAMIL_KOC",
  "departureAt": "2030-01-01T10:00:00Z", "arrivalAt": "2030-01-01T11:00:00Z",
  "capacity": 50,
  "stops": [{"stationName": "Aachen Hbf", "scheduledAt": "2030-01-01T10:00:00Z"}]
}'

curl -X POST localhost:8080/rides/1/cancel
curl -X POST localhost:8080/rides/events/replay
```

Errors come back as RFC 7807 problem details: 404 for unknown resources, 400 for validation errors, 409 when cancelling a ride that's already cancelled.

### capacity-service (`:8081`)

```bash
curl localhost:8081/capacity          # all lines
curl localhost:8081/capacity/N120     # {"lineCode":"N120","scheduledRides":2,"totalSeats":101}
```

Kafka UI is at http://localhost:8090.

## Test

```bash
./gradlew test
```

- `CapacityTopologyTest` uses `TopologyTestDriver` and doesn't need Docker.
- `SupplyApplicationIT` starts Postgres and Kafka with Testcontainers. It is skipped automatically when Docker isn't running.
