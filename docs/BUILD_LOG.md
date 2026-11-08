# Build log

What was added to each service, step by step, and why. Newest steps go at the bottom.

---

## Step 0 — Fix the build (whole project)

**Files:** `settings.gradle.kts`, `build.gradle.kts`, `*/build.gradle.kts`

- `settings.gradle.kts` listed each module twice. Now it's listed once.
- Dependencies used by both services (web, actuator, validation, **spring-kafka**, Lombok) moved to the root `build.gradle.kts`, inside `subprojects { }`.
- The Testcontainers version (the "BOM") is now set once at the root, not separately in each service.

**Concept:** Gradle multi-module builds. The root file holds shared setup and each module adds only what it needs.

---

## Step 1 — Fix the database migration (supply-service)

**File:** `src/main/resources/db/migration/V1__initial_schema.sql`

The SQL file sat in a folder whose name was a single space (`db/migration/ /`). It now sits directly in `db/migration/`.

**Concept:** **Flyway** runs `V1__…`, `V2__…` and so on in order, once each, and records what it has run in the `flyway_schema_history` table. Hibernate is set to `ddl-auto: validate`, so it only *checks* that the entities match the tables and never changes the tables itself.

**Try it:** start the service, then look at the table `flyway_schema_history` in Postgres.

---

## Step 2 — JPA entities (supply-service)

**Files:** `brand/Brand.java`, `partner/MobilityPartner.java`, `line/Line.java`, `ride/Ride.java`, `ride/RideStop.java`, `ride/RideStatus.java`

Each class maps to one table in the SQL file:

| Entity | Table | Notable |
|---|---|---|
| `Brand` | `brand` | `code` is unique (FLIXBUS, …) |
| `MobilityPartner` | `mobility_partner` | the bus company |
| `Line` | `line` | `@ManyToOne Brand brand`, the old line-level brand |
| `Ride` | `ride` | links to `Line`, `MobilityPartner` and (optionally) `Brand`; has a list of `RideStop` |
| `RideStop` | `ride_stop` | `@ManyToOne Ride ride` |

The bug that was fixed:
```java
// WRONG — @ManyToOne must point at an entity, not at an id number
@ManyToOne private Long lineId;

// RIGHT
@ManyToOne(fetch = FetchType.LAZY, optional = false)
@JoinColumn(name = "line_id")
private Line line;
```

The brand rule (`Ride.java`):
```java
public Brand effectiveBrand() {
    return brand != null ? brand : line.getBrand();   // ride-level wins, else line-level
}
```

**Concepts:** `@ManyToOne` / `@OneToMany(mappedBy=…)`, `FetchType.LAZY` (the related row is loaded only when you first touch it), `cascade = ALL` (saving a ride also saves its stops), `@Enumerated(STRING)`.

---

## Step 3 — Repositories with fetch joins (supply-service)

**Files:** `*/…Repository.java`

```java
@Query("""
  select r from Ride r
    join fetch r.line l
    join fetch r.partner
    left join fetch r.brand rb
    left join fetch l.brand lb
  where (:lineCode is null or l.code = :lineCode) ...
""")
List<Ride> search(String lineCode, String brandCode);
```

**Concept: the N+1 problem.** Without `join fetch`, loading 100 rides takes 1 query, and then 1 more query per ride the first time you touch `ride.getLine()`. That's 101 queries. `join fetch` loads everything in **one** query. `@EntityGraph(attributePaths = "brand")` does the same thing for the derived queries in `LineRepository`.

**Try it:** `org.hibernate.SQL: DEBUG` is on, so every SQL query is printed in the console. Call `GET /rides` and count the queries (it should be one). To see N+1 happen, swap the query for `findAll()` and call it again.

---

## Step 4 — Services, DTOs, controllers (supply-service)

**Files:** `*/…Service.java`, `*/…Controller.java`, `*Dto.java`, `ride/dto/CreateRideRequest.java`

- **Controller**: handles HTTP and nothing else.
- **Service**: the business rules, inside `@Transactional`. For example: a partner must be active, and you can't cancel a ride twice.
- **DTO** (`record`): what the API returns. Entities never go straight out as JSON, because of lazy loading problems, endless loops between linked entities, and leaking internal fields.

Endpoints: `GET /brands`, `/partners`, `/lines`, `/rides?lineCode=&brandCode=`, `/rides/{id}`, `POST /rides`, `POST /rides/{id}/cancel`.

Input checks (`CreateRideRequest`):
```java
@NotBlank String lineCode, @Positive int capacity, @Future Instant departureAt ...
@AssertTrue(message = "arrivalAt must be after departureAt")
public boolean isArrivalAfterDeparture() { ... }
```

**Concepts:** layered architecture, Bean Validation (`@Valid`), `@Transactional(readOnly = true)`, `open-in-view: false`.

---

## Step 5 — Error handling (supply-service)

**Files:** `web/NotFoundException.java`, `web/GlobalExceptionHandler.java`

| Exception | HTTP |
|---|---|
| `…NotFoundException` (all subclasses) | 404 |
| validation failure | 400 |
| `IllegalArgumentException` | 400 |
| `IllegalStateException` (e.g. cancelling twice) | 409 |

Responses use the standard **ProblemDetail** format (RFC 7807): `{"status":404,"detail":"Ride not found: 999",...}`.

**Concept:** `@RestControllerAdvice` keeps all error handling in one place instead of a try/catch in every controller.

---

## Step 6 — Publishing ride events to Kafka (supply-service)

**Files:** `events/RideEvent.java`, `events/KafkaTopics.java`, `events/RideEventPublisher.java`, `application.yml`

How it flows:
```
RideService.create()  ──publishEvent(RideEvent)──►  (Spring, in memory)
        │ transaction COMMITS
        ▼
RideEventPublisher.onRideEvent()  ──►  Kafka topic "supply.ride-events"  (key = rideId)
```

```java
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onRideEvent(RideEvent event) {
    kafka.send(KafkaTopics.RIDE_EVENTS, String.valueOf(event.rideId()), event);
}
```

Why **after commit**: if the database save fails and is rolled back, Kafka must never hear about a ride that doesn't exist.

Why **key = rideId**: every event for the same ride lands on the same partition, so they stay in order. The topic is also **compacted**, meaning Kafka eventually keeps only the latest event per key.

The event carries the **full current state** of the ride (not just "something changed"), so a consumer can treat the topic as a table.

`POST /rides/events/replay` sends one event per existing ride. That's needed because the seed rides were inserted by SQL, not through the service.

**Concepts:** domain events, `@TransactionalEventListener`, JSON serialization, topic compaction. A more robust version of this pattern is the **transactional outbox**, a good next step to learn.

**Try it:** open Kafka UI at http://localhost:8090 → Topics → `supply.ride-events`, then create a ride and watch the message arrive.

---

## Step 7 — capacity-service with Kafka Streams

**Files:** `CapacityApplication.java`, `RideEvent.java`, `LineCapacity.java`, `CapacityTopology.java`, `KafkaTopicsConfig.java`, `application.yml`

The processing pipeline (called a **topology**):
```
supply.ride-events (key rideId)
   │ builder.table(...)            → KTable: latest state of each ride
   │ .groupBy(→ key = lineCode)    → re-key by line
   │ .aggregate(add, subtract)     → LineCapacity per line, kept in a local store
   ▼
capacity.line-capacity (key lineCode)
```

```java
.aggregate(LineCapacity::empty,
    (line, ride, agg) -> agg.add(line, ride),        // new value of a ride
    (line, ride, agg) -> agg.subtract(line, ride),   // OLD value of the same ride, removed first
    Materialized.as(STORE))
```

**Key idea:** because the input is read as a **table** (`KTable`), when ride 2 changes from SCHEDULED to CANCELLED, Kafka Streams first *subtracts* the old value (49 seats) and then *adds* the new one (CANCELLED counts as 0). That's why cancellations and duplicate messages come out right.

`KafkaTopicsConfig` creates the topics, because Kafka Streams refuses to start if its input topic doesn't exist yet.

**Concepts:** KStream vs KTable, re-keying (`groupBy`), aggregation with an adder and a subtractor, state stores, `@EnableKafkaStreams`.

---

## Step 8 — REST over the state store (capacity-service)

**File:** `CapacityController.java`

`GET /capacity` and `GET /capacity/{lineCode}` read directly from the Kafka Streams local store, with no database involved. It returns 503 until the stream processing is `RUNNING`.

**Concept:** **interactive queries**, meaning you look up the result of a stream's work directly instead of copying it into a database first.

---

## Step 9 — Tests

| Test | Service | Needs Docker? | What it proves |
|---|---|---|---|
| `CapacityTopologyTest` | capacity | no | adding seats, cancellations, idempotent replay (`TopologyTestDriver`, a fake Kafka in memory) |
| `SupplyApplicationIT` | supply | yes | full app against real Postgres and Kafka (Testcontainers): seeded data, 404, 400, 409, events published |

**Concepts:** `TopologyTestDriver`, Testcontainers with `@ServiceConnection`.

**Try it:** `./gradlew test`, then open `*/build/reports/tests/test/index.html`.

---

<!-- New steps go below this line -->
