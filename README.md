# Ticketing Reservation API

## Local Database

The application uses a file-backed H2 database by default. Start it without any
external database or Docker dependency:

```bash
./gradlew bootRun
```

Local data is stored under `./data`. Set `DB_URL`, `DB_USERNAME`, and
`DB_PASSWORD` to override the connection. Integration tests use an in-memory H2
database and require no container runtime.

## Development Seed Users

Liquibase creates the following users when the `dev` or `test` context is active. All three accounts use the password `ChangeMe123!`.

| Email | Password | Role |
|---|---|---|
| `admin@ticketing.local` | `ChangeMe123!` | `ADMIN` |
| `organizer@ticketing.local` | `ChangeMe123!` | `ORGANIZER` |
| `customer@ticketing.local` | `ChangeMe123!` | `CUSTOMER` |

Use the account to obtain access and refresh tokens:

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@ticketing.local",
  "password": "ChangeMe123!"
}
```

Example request:

```bash
curl --request POST 'http://localhost:8080/api/auth/login' \
  --header 'Content-Type: application/json' \
  --data '{
    "email": "admin@ticketing.local",
    "password": "ChangeMe123!"
  }'
```

> These are local development credentials. The accounts are not seeded when the `prod` Liquibase context is active and must not be used in production.
