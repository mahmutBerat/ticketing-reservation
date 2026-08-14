# Ticketing Reservation API

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
