# Fiscal Reporting Platform

ZIMRA-style invoicing and VAT return service. Security is fully delegated to
**SecureShield** — this service holds no users table and no private keys; it
only verifies RS256 JWTs issued by SecureShield.

## Required environment variables (Railway)

| Variable | Purpose |
|---|---|
| `JDBC_DATABASE_URL` | Postgres connection string — append `?sslmode=disable` (same Railway quirk as SecureShield) |
| `JDBC_DATABASE_USERNAME` / `JDBC_DATABASE_PASSWORD` | DB credentials |
| `SHIELD_JWT_PUBLIC_KEY_B64` | Base64-encoded PEM of SecureShield's RSA public key. Preferred over the classpath file for production. |
| `SHIELD_JWT_ISSUER` | Expected `iss` claim, e.g. `secureshield-api` |
| `SECURESHIELD_BASE_URL` | Base URL of the SecureShield API, e.g. `https://shield-api-production.up.railway.app` |
| `HASH_CHAIN_SECRET` | HMAC secret for this service's own invoice/audit hash chain. **Must not** be the same secret as SecureShield's — keep the two chains cryptographically independent. |

## Expected JWT claims

```json
{
  "sub": "user-uuid-or-identifier",
  "org_id": "org-uuid",
  "roles": ["ADMIN" | "ACCOUNTANT" | "VIEWER"],
  "iss": "secureshield-api"
}
```

`org_id` scopes every query in this service — customers, invoices, and VAT
returns are always filtered by the org_id in the token, never a path
parameter alone (BOLA defense, same pattern as SecureShield).

## Local development

```bash
docker compose up -d postgres   # or point JDBC_DATABASE_URL at any local Postgres
mvn spring-boot:run
```

Flyway runs `V1__init_schema.sql` automatically on startup and seeds the
three ZIMRA tax codes (`STD` 15%, `ZERO` 0%, `EXEMPT`).

## API surface (phase 1)

- `POST /api/v1/customers` / `GET /api/v1/customers`
- `POST /api/v1/invoices` — issues a fiscalized, hash-chained invoice
- `GET /api/v1/invoices/{id}`
- `GET /api/v1/fiscal-days/current` / `POST /api/v1/fiscal-days/{id}/close`
- `POST /api/v1/vat-returns/generate` — aggregates issued invoices in a period by tax code
- `POST /api/v1/vat-returns/{id}/submit`
- `GET /api/v1/vat-returns/{id}`

## Not yet in scope (phase 2 candidates)

- Purchase invoices / input VAT tracking (fields already exist, unused)
- Payments reconciliation UI
- Credit notes / invoice cancellation flow
- Direct FDMS device signing (current QR/verification code is a local stand-in)
