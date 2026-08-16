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
- Pessimistic locking around capacity-sensitive event operations
- Optimistic versioning for concurrent entity updates
- Transactional audit records for state-changing operations
- Health checks, correlation IDs, metrics, and optional OTLP export
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

Capacity-sensitive reservation flows lock the event row before calculating active seats. Event capacity cannot be
reduced below the number of seats held by active reservations.

## Technology Stack

| Area                | Technology                                         |
|---------------------|----------------------------------------------------|
| Runtime             | Java 25, Spring Boot 4.1                           |
| Web and validation  | Spring Web MVC, Jakarta Validation                 |
| Security            | Spring Security, OAuth2 Resource Server, JWT HS256 |
| Persistence         | Spring Data JPA, Hibernate, H2                     |
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

Reservation creation requires an `Idempotency-Key` header containing a valid UUIDv4.

Interactive API documentation is available after startup:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Health: <http://localhost:8080/actuator/health>

## Getting Started

### Prerequisites

- Java 25
- The included Gradle Wrapper; a separate Gradle installation is not required

### Run locally

```bash
./gradlew bootRun
```

The application starts on <http://localhost:8080> and uses a file-backed H2 database under `./data` by default. No
external database or Docker runtime is required.

Override the database connection when needed:

```bash
DB_URL=jdbc:h2:file:./data/ticketing \
DB_USERNAME=sa \
DB_PASSWORD= \
./gradlew bootRun
```

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

> These credentials are for local development only. They are not seeded with the `prod` Liquibase context and must not
> be used in production.

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

- Test report: `build/reports/tests/test/index.html`
- Coverage report: `build/reports/jacoco/test/html/index.html`
- Coverage XML: `build/reports/jacoco/test/jacocoTestReport.xml`

Run static analysis separately:

```bash
./gradlew spotbugsMain
```

SpotBugs analyzes production code and reports high-confidence findings without failing the build. Its HTML report is
generated at `build/reports/spotbugs/main/spotbugs.html`.

## Continuous Integration

The GitHub Actions CI workflow runs for:

- pushes to `main`;
- pull requests targeting `main`;
- manual workflow dispatches.

CI installs Java 25, restores the Gradle cache, and runs `./gradlew clean build`.

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

Activate production configuration with `--spring.profiles.active=prod`. Production requires an explicit `DB_URL` and
`JWT_SECRET`.
