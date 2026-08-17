# Ticketing Reservation API

[![CI](https://github.com/mahmutBerat/ticketing-reservation/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/mahmutBerat/ticketing-reservation/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-25-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.0-6DB33F)
![Gradle](https://img.shields.io/badge/Gradle-9.5.1-02303A)

A Spring Boot API for publishing events and creating capacity-safe ticket reservations. The project demonstrates
role-based JWT security, transactional consistency, idempotent request handling, auditability, and overselling
prevention in a compact modular application.

## Key Capabilities

- Access and refresh token authentication with `ADMIN`, `ORGANIZER`, and `CUSTOMER` roles
- Event creation, update, publication, owner-based management, and public discovery
- Reservation creation, confirmation, and cancellation
- UUIDv4 idempotency keys for reservation creation
- Capacity enforcement using active `PENDING` and `CONFIRMED` reservations
- Pessimistic event-row locking during reservation capacity checks without updating the event
- Optimistic version locking for actual event updates
- Transactional audit records for event and reservation changes
- Health checks, trace correlation, and optional OTLP trace/metrics export
- Liquibase-managed schema and development seed data

## Architecture

The application is a feature-first modular monolith. Each business capability keeps its API, application logic, domain
model, and persistence code together.

```text
com.mbi.ticketingreservation
├── auth
├── event
├── reservation
├── idempotency
├── audit
└── common
```

Within a feature:

| Package       | Responsibility                                         |
|---------------|--------------------------------------------------------|
| `api`         | HTTP controllers, request/response models, and mapping |
| `application` | Use-case orchestration and transaction boundaries      |
| `domain`      | Entities, state transitions, and domain rules          |
| `persistence` | Spring Data repositories and query specifications      |

## Technology Stack

| Area                | Technology                                         |
|---------------------|----------------------------------------------------|
| Runtime             | Java 25, Spring Boot 4.1                           |
| Web and validation  | Spring Web MVC, Jakarta Validation                 |
| Security            | Spring Security, OAuth2 Resource Server, JWT HS256 |
| Persistence         | Spring Data JPA, Hibernate, H2 (local and tests)   |
| Database migrations | Liquibase                                          |
| Mapping             | MapStruct                                          |
| Observability       | Spring Boot Actuator, Micrometer, OpenTelemetry    |
| Testing             | JUnit 5, Mockito, Spring Boot Test                 |
| Coverage            | JaCoCo 0.8.15                                      |
| Static analysis     | SpotBugs 4.10.2 through Gradle plugin 6.5.6        |
| Build               | Gradle 9.5.1                                       |

## API Overview

| Method  | Endpoint                                    | Access                 |
|---------|---------------------------------------------|------------------------|
| `POST`  | `/api/auth/register`                        | Public                 |
| `POST`  | `/api/auth/login`                           | Public                 |
| `POST`  | `/api/auth/refresh`                         | Public                 |
| `GET`   | `/api/auth/users`                           | Admin                  |
| `PATCH` | `/api/auth/users/{userId}/roles`            | Admin                  |
| `GET`   | `/api/events/public`                        | Public                 |
| `POST`  | `/api/events`                               | Organizer, Admin       |
| `PUT`   | `/api/events/{id}`                          | Owner organizer, Admin |
| `POST`  | `/api/events/{id}/publish`                  | Owner organizer, Admin |
| `GET`   | `/api/events`                               | Organizer, Admin       |
| `GET`   | `/api/events/{id}`                          | Owner organizer, Admin |
| `POST`  | `/api/events/{eventId}/reservations`        | Customer, Admin        |
| `POST`  | `/api/reservations/{reservationId}/confirm` | Owner customer, Admin  |
| `POST`  | `/api/reservations/{reservationId}/cancel`  | Owner customer, Admin  |

## Authentication and Authorization

Registration creates a `CUSTOMER`. Login returns an HS256 access token and refresh token; protected endpoints use the
access token as `Authorization: Bearer <token>`. Organizers can manage only their own events, customers can manage only
their own reservations, and admins can perform the documented administrative operations.

## Reservation Consistency

A new reservation starts as `PENDING` and immediately holds its seats. Confirming it changes only the state to
`CONFIRMED`; cancelling a pending or confirmed reservation releases its seats from the active capacity calculation.

For concurrent creation requests, the event row is held with a pessimistic write lock until the reservation transaction
commits or rolls back. Capacity counts only `PENDING` and `CONFIRMED` reservations. to see test
cases: [OversellingPreventionIntegrationTest.java](src/test/java/com/mbi/ticketingreservation/dataintegrationtests/OversellingPreventionIntegrationTest.java)

## Idempotency

Reservation creation requires an `Idempotency-Key` header containing a valid UUIDv4.
The first request claims the key in the same transaction as the reservation and audit record. Reusing a non-expired
key returns `409 IDEMPOTENCY_KEY_REUSED`, including when the payload is identical. Expired records are physically
replaced; response bodies are not stored or replayed. too see
tests: [IdempotencyIntegrationTest.java](src/test/java/com/mbi/ticketingreservation/dataintegrationtests/IdempotencyIntegrationTest.java)

## API Docs

Interactive API documentation is available after startup:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

Use the access token returned by login with Swagger UI's **Authorize** button when calling protected endpoints.

### Postman Collection

Import [Ticketing&Reservation.postman_collection.json](Ticketing&Reservation.postman_collection.json) into Postman to
exercise the complete API workflow. Run its folders in order: **Health**, **Authentication**, **Events**, and then
**Reservations**. 

### Run locally

```bash
./gradlew bootRun
```

The application starts on <http://localhost:8080> and uses a file-backed H2 database under `./data` by default. No
external database or Docker runtime is required.

## Development Seed Users

Liquibase creates the following users when the `dev` or `test` context is active. All three accounts use the password
`ChangeMe123!`.

| Email                       | Password       | Role        |
|-----------------------------|----------------|-------------|
| `admin@ticketing.local`     | `ChangeMe123!` | `ADMIN`     |
| `organizer@ticketing.local` | `ChangeMe123!` | `ORGANIZER` |
| `customer@ticketing.local`  | `ChangeMe123!` | `CUSTOMER`  |

Obtain access and refresh tokens:

```bash
curl --request POST 'http://localhost:8080/api/auth/login' \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "admin@ticketing.local",
    "password": "ChangeMe123!"
  }'
```

Use the returned access token for protected endpoints:

```bash
curl --request GET 'http://localhost:8080/api/auth/users' \
  --header 'Authorization: Bearer <access-token>'
```

## Testing, Coverage, and Static Analysis

Run all checks:

```bash
./gradlew clean build
```

Run tests and generate the JaCoCo report:

```bash
./gradlew test
```

The `test` task automatically finalizes with `jacocoTestReport`.

Tests use an in-memory H2 database. The suite includes domain, API integration, idempotency, and concurrent overselling
checks; it does not currently use PostgreSQL Testcontainers.

- Test report: `build/reports/tests/test/index.html`
- Coverage report: `build/reports/jacoco/test/html/index.html`
- Coverage XML: `build/reports/jacoco/test/jacocoTestReport.xml`

Run static analysis separately:

```bash
./gradlew spotbugsMain
```

Spotbugs HTML report is generated at `build/reports/spotbugs/main/spotbugs.html`.

## Continuous Integration

The GitHub Actions CI workflow https://github.com/mahmutBerat/ticketing-reservation/actions/workflows/ci.yml runs for:

- pushes to `main`;
- pull requests targeting `main`;
- manual workflow dispatches.

Sample CI workflow output: https://github.com/mahmutBerat/ticketing-reservation/actions/runs/32005732543

## Observability

Only `/actuator/health` is exposed over HTTP. Application logs include trace and span correlation, responses expose
`X-Trace-Id` and `X-Span-Id` when a span is active, and OTLP trace/metrics export is disabled by default. Custom
business metrics are not currently implemented.

## Configuration

| Environment variable          | Default                         | Purpose                                      |
|-------------------------------|---------------------------------|----------------------------------------------|
| `DB_URL`                      | `jdbc:h2:file:./data/ticketing` | JDBC connection URL                          |
| `DB_USERNAME`                 | `sa`                            | Database username                            |
| `DB_PASSWORD`                 | Empty                           | Database password                            |
| `LIQUIBASE_CONTEXTS`          | `dev`                           | Active Liquibase contexts                    |
| `JWT_SECRET`                  | Local development value         | HS256 signing secret; required in production |
| `OTLP_TRACING_EXPORT_ENABLED` | `false`                         | Enable OTLP trace export                     |
| `OTLP_METRICS_EXPORT_ENABLED` | `false`                         | Enable OTLP metrics export                   |

The idempotency-key lifetime is currently configured as `10m` in `application.properties`.

## Architectural Decisions Records and Production Evolution

The current implementation favors a compact local setup: H2, Liquibase migrations, event-row pessimistic locking, and
non-replaying idempotency keys. There is no dedicated production profile or PostgreSQL driver yet. A production setup
should add the target database driver and profile, set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a strong
`JWT_SECRET`, and use a non-seeding Liquibase context such as `prod`.

### ADR-001: Modular Monolith

- **Context:** The features share transactional data and are deployed together.
- **Decision:** Use one Spring Boot application with feature-first packages.
- **Alternatives:** Microservices or a technical-layer monolith.
- **Trade-offs:** Simple deployment and transactions while having compact code base and easier to divide in the future;
  features cannot scale independently.

### ADR-002: Relational Database as Consistency Source

- **Context:** Capacity and idempotency must remain correct under concurrency.
- **Decision:** Keep authoritative state in the relational database using transactions, constraints, and Liquibase.
- **Alternatives:** Cache-backed inventory or event sourcing.
- **Trade-offs:** Strong consistency; the database can become a bottleneck for hot events.

### ADR-003: Reservation Concurrency Control

- **Context:** Concurrent requests must not reserve the same remaining capacity.
- **Decision:** Lock the event with `PESSIMISTIC_WRITE` during reservation creation and use `@Version` for event
  updates.
- **Alternatives:** Optimistic retries, an inventory counter, or a distributed lock.
- **Trade-offs:** Prevents overselling; popular events can experience lock contention and timeouts.

### ADR-004: Database-backed Idempotency

- **Context:** Client retries must not create duplicate reservations.
- **Decision:** Store UUIDv4 keys and request hashes transactional; reject active key reuse with `409` and do not
  replay responses: process at most once.
- **Alternatives:** Response replay, in-memory keys, or Redis.
- **Trade-offs:** Durable at-most-once behavior; a lost successful response cannot be recovered by retrying the key.

### ADR-005: JWT, RBAC, and Ownership

- **Context:** Roles alone cannot stop users with the same role from modifying each other's resources.
- **Decision:** Use stateless JWT authentication, endpoint role checks, and service-level ownership checks with admin
  bypass.
- **Alternatives:** Server sessions, RBAC alone, or controller-only ownership checks.
- **Trade-offs:** Scalable authentication and explicit authorization; role changes apply after existing access tokens
  expire.

## Open Issues and Future Improvements

- Support idempotent response replay instead of returning HTTP 409 for repeated identical requests. This would simplify
  client-side error handling and allow clients to recover safely when a successful response is lost.
- Improve API security by adding rate limiting for API endpoints.
- Migrate the system's persistence layer to PostgreSQL and run integration and concurrency tests against it using
  Testcontainers.
- Improve observability by adding business metrics for reservation outcomes, capacity usage, and lock contention.
- Load-test popular events and evaluate alternatives to event-row locking if contention becomes a bottleneck.
