# E-Commerce API

A Spring Boot REST API for an e-commerce platform: JWT authentication, product catalog
with search, shopping cart, and Stripe-powered checkout. Built as a learning project to
practice JWT/Spring Security, complex relational data models, and third-party payment
integration.

## Stack

Java 17 · Spring Boot 3.3 · Spring Security · Spring Data JPA · PostgreSQL (H2 for local dev)
· JWT (jjwt) · Stripe Java SDK · Lombok · Maven

## Quick start (no Postgres install needed)

Run against an in-memory H2 database with the `dev` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The API comes up on `http://localhost:8080`. H2 console (if you want to peek at the
tables) is at `http://localhost:8080/h2-console` — JDBC URL `jdbc:h2:mem:ecommerce`, user `sa`, no password.

## Running against real Postgres

```bash
docker compose up -d          # starts Postgres on localhost:5432
mvn spring-boot:run
```

Override any of these env vars as needed (defaults shown target the docker-compose service):

| Variable              | Default                                      |
|-----------------------|-----------------------------------------------|
| `DB_URL`              | `jdbc:postgresql://localhost:5432/ecommerce` |
| `DB_USERNAME`         | `postgres`                                   |
| `DB_PASSWORD`         | `postgres`                                   |
| `JWT_SECRET`          | dev placeholder — **set a real one in prod** |
| `JWT_EXPIRATION_MS`   | `86400000` (24h)                             |
| `STRIPE_SECRET_KEY`   | placeholder — get from Stripe Dashboard      |
| `STRIPE_WEBHOOK_SECRET` | placeholder — from `stripe listen` or Dashboard |
| `STRIPE_CURRENCY`     | `usd`                                        |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | unset — see "Becoming an admin" below |

## Becoming an admin

There's deliberately no public "make me an admin" endpoint. Two options:

1. **Seeded on startup** (recommended): set `ADMIN_EMAIL` and `ADMIN_PASSWORD` env vars
   before first run — an admin account is created automatically if it doesn't exist yet.
2. **Manually**: register a normal account, then update its role directly in the DB:
   `UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';`

## Testing Stripe checkout locally

1. Get a test secret key from the [Stripe Dashboard](https://dashboard.stripe.com/test/apikeys) and set `STRIPE_SECRET_KEY`.
2. Install the [Stripe CLI](https://stripe.com/docs/stripe-cli) and forward webhooks to your local server:
   ```bash
   stripe listen --forward-to localhost:8080/api/orders/webhook
   ```
   This prints a `whsec_...` value — set it as `STRIPE_WEBHOOK_SECRET`.
3. Call `POST /api/orders/checkout` — you'll get back a `clientSecret`. Confirm it with a
   [test card](https://stripe.com/docs/testing) (`4242 4242 4242 4242`, any future date/CVC)
   using Stripe.js on a frontend, or trigger it directly via `stripe trigger payment_intent.succeeded`
   for a quick end-to-end check without building a frontend first.

## API reference

All request/response bodies are JSON. Authenticated routes expect `Authorization: Bearer <token>`.

### Auth (public)
| Method | Path | Body |
|---|---|---|
| POST | `/api/auth/register` | `{ name, email, password }` |
| POST | `/api/auth/login` | `{ email, password }` |

### Products (browsing is public; writes are admin-only)
| Method | Path | Auth | Notes |
|---|---|---|---|
| GET | `/api/products?keyword=&category=&page=&size=` | none | paginated search |
| GET | `/api/products/{id}` | none | |
| POST | `/api/admin/products` | ADMIN | create |
| PUT | `/api/admin/products/{id}` | ADMIN | full update |
| PATCH | `/api/admin/products/{id}/stock` | ADMIN | `{ stockQuantity }` |
| DELETE | `/api/admin/products/{id}` | ADMIN | |

### Cart (per authenticated user)
| Method | Path | Body |
|---|---|---|
| GET | `/api/cart` | |
| POST | `/api/cart/items` | `{ productId, quantity }` |
| PUT | `/api/cart/items/{itemId}` | `{ quantity }` |
| DELETE | `/api/cart/items/{itemId}` | |
| DELETE | `/api/cart` | clears the whole cart |

### Orders / checkout
| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/api/orders/checkout` | user | creates an order + Stripe PaymentIntent from the current cart |
| GET | `/api/orders` | user | current user's order history |
| GET | `/api/orders/{id}` | user | |
| POST | `/api/orders/webhook` | none (Stripe-signed) | marks orders PAID/FAILED |
| GET | `/api/admin/orders` | ADMIN | all orders, paginated |
| PATCH | `/api/admin/orders/{id}/status` | ADMIN | `{ status }` |

## Design notes

- **Stock is only decremented on confirmed payment** (via the webhook), not at checkout
  time — this avoids reserving inventory for abandoned/failed payments.
- **Order line items snapshot the product price** (`priceAtPurchase`) so a later price
  change never rewrites order history.
- **Webhook handling is idempotent** — Stripe can and will redeliver events; re-marking
  an already-PAID order as PAID is a no-op.
- Passwords are hashed with BCrypt; JWTs are HMAC-SHA256 signed and stateless (no server
  session), so the API scales horizontally without sticky sessions.

## Not included yet (natural next steps)

- Refresh tokens (current JWTs simply expire after `JWT_EXPIRATION_MS`)
- Rate limiting on `/api/auth/**`
- A frontend — this was built API-first per the project brief; pair it with Postman or
  a simple Jinja/EJS/React client next
- Refund handling (`charge.refunded` webhook event)
