# URL Shortener Backend

Production-style URL shortener backend built with Java 21 and Spring Boot. The project is intentionally more advanced than a basic CRUD API, while staying within a clean modular monolith scope.

It includes JWT authentication, user-owned short URLs, custom aliases, expiration, redirects, Redis cache-aside, basic analytics, Redis-based rate limiting, Liquibase migrations, Swagger/OpenAPI documentation, and Docker Compose for local infrastructure.

## Features

- User registration and login with BCrypt password hashing.
- JWT access-token authentication.
- Authenticated URL creation and management.
- Public redirect endpoint: `GET /r/{shortCode}`.
- Generated Base62-compatible short codes.
- Optional custom aliases with uniqueness enforced by PostgreSQL.
- Optional expiration via `expiresAt`.
- Logical disable instead of physical delete.
- Redis cache-aside for redirect resolution.
- After-commit Redis cache invalidation for URL updates and disable operations.
- Basic analytics: `clickCount` and `lastAccessedAt`.
- Atomic PostgreSQL click increment query.
- Best-effort analytics during redirects so analytics failure does not block a valid redirect.
- Redis-based create-URL rate limiting.
- Atomic Redis Lua script for rate-limit counter + TTL.
- Consistent JSON error responses.
- Swagger UI with JWT bearer authorization support.
- PostgreSQL schema management through Liquibase.
- Docker Compose for PostgreSQL, Redis, and RedisInsight.
- Focused tests for critical behavior.

## Tech Stack

- Java 21
- Spring Boot 4.1
- Spring MVC
- Spring Data JPA
- PostgreSQL
- Liquibase
- Redis
- Spring Security
- JWT
- BCrypt
- Bean Validation
- Lombok
- Gradle Kotlin DSL
- Docker Compose
- Springdoc OpenAPI / Swagger UI

## Architecture

The application is a feature-based modular monolith under:

```text
com.example.urlshortener
```

Main packages:

```text
src/main/java/com/example/urlshortener
|-- analytics
|   |-- controller
|   |-- dto
|   `-- service
|-- auth
|   |-- controller
|   |-- dto
|   |-- entity
|   |-- repository
|   |-- security
|   `-- service
|-- common
|   |-- exception
|   `-- response
|-- config
|-- ratelimit
|-- redirect
|   |-- cache
|   |-- controller
|   `-- service
`-- shorturl
    |-- controller
    |-- dto
    |-- entity
    |-- generator
    |-- mapper
    |-- repository
    `-- service
```

Design notes:

- PostgreSQL is the source of truth.
- Redis is only a cache and rate-limit store.
- URL ownership is always resolved from the authenticated JWT principal, not request body user IDs.
- `ShortUrl -> User` is lazy-loaded to avoid unnecessary user queries.
- Expiration is calculated from `expiresAt`; it is not stored as a separate persistent status.
- Persistent URL status is intentionally simple: `ACTIVE` or `DISABLED`.
- Database unique constraints are the final guarantee for alias/short-code uniqueness.

## Data Model

### User

Approximate fields:

| Field | Description |
| --- | --- |
| `id` | Database identifier |
| `email` | Unique user email |
| `password` | BCrypt-hashed password |
| `createdAt` | Creation timestamp |

### ShortUrl

Approximate fields:

| Field | Description |
| --- | --- |
| `id` | Database identifier |
| `shortCode` | Generated code or custom alias |
| `originalUrl` | Destination URL |
| `user` | Owning user |
| `status` | `ACTIVE` or `DISABLED` |
| `createdAt` | Creation timestamp |
| `expiresAt` | Optional expiration timestamp |
| `clickCount` | Successful redirect count |
| `lastAccessedAt` | Most recent successful redirect timestamp |

## API Overview

Base URL for local development:

```text
http://localhost:8081
```

If `8081` is busy, run with another port:

```powershell
$env:SERVER_PORT="8090"
$env:APP_BASE_URL="http://localhost:8090"
.\gradlew.bat bootRun
```

### Public Endpoints

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Register a user and return JWT |
| `POST` | `/api/v1/auth/login` | Log in and return JWT |
| `GET` | `/r/{shortCode}` | Public redirect to original URL |
| `GET` | `/swagger-ui.html` | Swagger UI |
| `GET` | `/v3/api-docs` | OpenAPI JSON |

### Authenticated Endpoints

Use:

```http
Authorization: Bearer <accessToken>
```

| Method | Endpoint | Description |
| --- | --- | --- |
| `POST` | `/api/v1/urls` | Create a short URL |
| `GET` | `/api/v1/urls` | List current user's URLs with pagination |
| `GET` | `/api/v1/urls/{id}` | Get one owned URL |
| `PATCH` | `/api/v1/urls/{id}` | Update editable fields |
| `DELETE` | `/api/v1/urls/{id}` | Disable an owned URL |
| `GET` | `/api/v1/urls/{id}/stats` | Get basic analytics |

Unknown endpoints are protected by default and are not automatically public.

## Swagger

Run the app and open:

```text
http://localhost:8081/swagger-ui.html
```

Flow in Swagger:

1. Call `POST /api/v1/auth/register` or `POST /api/v1/auth/login`.
2. Copy the `accessToken`.
3. Click `Authorize`.
4. Paste the token as:

```text
Bearer <accessToken>
```

5. Use the authenticated URL management endpoints.

## Request Examples

### Register

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "Secret123!"
}
```

Example response:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "user@example.com"
}
```

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "user@example.com",
  "password": "Secret123!"
}
```

### Create Short URL

```http
POST /api/v1/urls
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "originalUrl": "https://example.com/products/123",
  "customAlias": "my-product",
  "expiresAt": "2026-09-01T12:00:00"
}
```

`customAlias` and `expiresAt` are optional.

Validation rules:

- `originalUrl` is required.
- Only `http` and `https` URLs are allowed.
- URL host must be syntactically valid.
- URL length is limited to 2048 characters.
- `customAlias` must be 3-32 characters.
- `customAlias` may contain letters, numbers, `-`, and `_`.
- `expiresAt` must be in the future.

### List URLs

```http
GET /api/v1/urls?page=0&size=20&sort=createdAt,desc
Authorization: Bearer <accessToken>
```

Response shape:

```json
{
  "content": [
    {
      "id": 1,
      "shortCode": "my-product",
      "shortUrl": "http://localhost:8081/r/my-product",
      "originalUrl": "https://example.com/products/123",
      "status": "ACTIVE",
      "createdAt": "2026-08-12T09:30:00",
      "expiresAt": "2026-09-01T12:00:00"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

Allowed sort properties:

- `createdAt`
- `shortCode`
- `originalUrl`
- `status`
- `expiresAt`
- `clickCount`
- `lastAccessedAt`

### Redirect

```http
GET /r/my-product
```

Successful response:

```http
302 Found
Location: https://example.com/products/123
```

Possible redirect errors:

| Status | Meaning |
| --- | --- |
| `404` | Short code does not exist |
| `410` | Short URL is expired |
| `410` | Short URL is disabled |

### Update URL

```http
PATCH /api/v1/urls/1
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "originalUrl": "https://example.org/updated",
  "expiresAt": "2026-10-01T12:00:00"
}
```

Editable fields:

- `originalUrl`
- `expiresAt`

Internal values such as `id`, `clickCount`, `createdAt`, and `status` are not editable through this endpoint.

### Disable URL

```http
DELETE /api/v1/urls/1
Authorization: Bearer <accessToken>
```

Successful response:

```http
204 No Content
```

The row remains in PostgreSQL with status `DISABLED`.

### Stats

```http
GET /api/v1/urls/1/stats
Authorization: Bearer <accessToken>
```

Example response:

```json
{
  "id": 1,
  "shortCode": "my-product",
  "originalUrl": "https://example.com/products/123",
  "clickCount": 150,
  "createdAt": "2026-08-12T09:30:00",
  "lastAccessedAt": "2026-08-12T10:15:00",
  "expiresAt": "2026-09-01T12:00:00",
  "status": "ACTIVE"
}
```

## Error Response Format

All handled API errors return a consistent JSON shape:

```json
{
  "code": "SHORT_URL_NOT_FOUND",
  "message": "Short URL was not found",
  "timestamp": "2026-08-12T09:30:00"
}
```

Common statuses:

| Status | Examples |
| --- | --- |
| `400` | Validation error, malformed request, invalid URL |
| `401` | Missing or invalid JWT |
| `403` | Accessing another user's URL |
| `404` | Short URL not found |
| `409` | Duplicate custom alias or email |
| `410` | Expired or disabled short URL |
| `429` | Create URL rate limit exceeded |
| `500` | Unexpected internal error |

## Redis Behavior

Redis is used for two scoped responsibilities.

### Redirect Cache

Key format:

```text
short-url:{shortCode}
```

Cached data includes enough information to resolve and validate redirects without always querying PostgreSQL:

- `shortCode`
- `originalUrl`
- `status`
- `expiresAt`

Behavior:

```text
GET /r/{shortCode}
  -> Redis lookup
  -> cache hit: validate cached data, record analytics, redirect
  -> cache miss: read PostgreSQL, validate, cache, record analytics, redirect
```

PostgreSQL remains the source of truth. Redis failures are logged and the application falls back to PostgreSQL where possible.

Cache invalidation:

- URL destination update invalidates cache.
- Expiration update invalidates cache.
- Disable operation invalidates cache.
- Invalidation is performed only after a successful database commit.

The default short URL cache TTL is:

```text
PT1M
```

If a URL expires sooner than the default TTL, it is never cached beyond its expiration time.

### Rate Limiting

URL creation is rate-limited per authenticated user.

Key format:

```text
rate-limit:create-url:user:{userId}
```

Default limit:

```text
20 URL creations per minute
```

The implementation uses a Redis Lua script so counter creation and TTL assignment are atomic:

```lua
local current = redis.call('INCR', KEYS[1])

if current == 1 then
    redis.call('PEXPIRE', KEYS[1], ARGV[1])
end

return current
```

If Redis is unavailable, rate limiting fails open: the error is logged and URL creation is allowed.

## Analytics Behavior

Analytics intentionally stays simple:

- `clickCount`
- `lastAccessedAt`

Each successful redirect attempts an atomic PostgreSQL update:

```sql
UPDATE short_urls
SET click_count = click_count + 1,
    last_accessed_at = ?
WHERE id = ?
```

Analytics is best-effort during redirect resolution. If the target URL is already validly resolved but the analytics update fails, the redirect still returns `302`.

## Security

Public:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `GET /r/**`
- Swagger/OpenAPI paths

Authenticated:

- `/api/v1/urls/**`

The security configuration uses a protected default:

```text
any unknown/new endpoint -> authenticated
```

This avoids accidentally exposing future endpoints.

## Local Development

### Prerequisites

- Java 21
- Docker Desktop
- Git

The Gradle wrapper is included, so a separate Gradle installation is not required.

### Start Infrastructure

```powershell
docker compose up -d
```

Services:

| Service | URL/Port |
| --- | --- |
| PostgreSQL | `localhost:5432` |
| Redis | `localhost:6379` |
| RedisInsight | `http://localhost:5540` |

### Run The Application

Windows PowerShell:

```powershell
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
./gradlew bootRun
```

Default app URL:

```text
http://localhost:8081
```

Run on another port:

```powershell
$env:SERVER_PORT="8090"
$env:APP_BASE_URL="http://localhost:8090"
.\gradlew.bat bootRun
```

### Build

```powershell
.\gradlew.bat build
```

### Run Tests

```powershell
.\gradlew.bat test
```

## Configuration

Configuration is provided through `src/main/resources/application.yaml` and environment variables.

| Environment Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8081` | HTTP server port |
| `APP_BASE_URL` | `http://localhost:8081` | Base URL used when returning short URL links |
| `DB_URL` | `jdbc:postgresql://localhost:5432/url_shortener` | PostgreSQL JDBC URL |
| `DB_USERNAME` | `postgres` | PostgreSQL username |
| `DB_PASSWORD` | `postgres` | PostgreSQL password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_TIMEOUT` | `PT2S` | Redis read timeout |
| `REDIS_CONNECT_TIMEOUT` | `PT2S` | Redis connect timeout |
| `JWT_SECRET` | local development secret | Secret used to sign JWTs |
| `JWT_EXPIRATION` | `PT1H` | Access token lifetime |
| `SHORT_URL_CACHE_TTL` | `PT1M` | Default Redis short URL cache TTL |
| `CREATE_URL_RATE_LIMIT` | `20` | Max URL creations per window |
| `CREATE_URL_RATE_LIMIT_WINDOW` | `PT1M` | Rate-limit window |

For real deployments, always override:

- `JWT_SECRET`
- `DB_USERNAME`
- `DB_PASSWORD`
- `DB_URL`
- `APP_BASE_URL`

## Database Migrations

Liquibase owns schema evolution.

Master changelog:

```text
src/main/resources/db/changelog/db.changelog-master.yaml
```

Current changesets:

```text
001-create-short-urls-table.yaml
002-create-users-table.yaml
003-add-user-relationship-to-short-urls.yaml
```

Hibernate schema generation is disabled for mutation and configured for validation:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

## Testing Scope

Focused tests cover:

- Short code generation.
- URL validation and service behavior.
- Custom alias duplication behavior.
- Expired and disabled URL redirect behavior.
- Redirect resolution through cache and database.
- Analytics failure isolation during redirects.
- After-commit cache invalidation.
- Rollback does not evict cache.
- Redis rate-limiter Lua script usage.
- Redis rate-limiter fail-open behavior.
- Security rules for public and authenticated endpoints.

## Manual Verification Checklist

Use Swagger or curl/Postman to verify:

```text
REGISTER
  -> LOGIN
  -> CREATE URL
  -> GET MY URLS
  -> OPEN SHORT URL
  -> STATS INCREMENT
  -> UPDATE URL
  -> REDIRECT USES NEW DESTINATION
  -> DISABLE URL
  -> REDIRECT RETURNS 410
```

Also verify:

- Duplicate alias returns `409`.
- Invalid URL returns `400`.
- Expired URL returns `410`.
- Missing short code returns `404`.
- Accessing another user's URL returns `403`.
- Unauthenticated management request returns `401`.
- Unknown endpoint returns `401`.
- Rate limit returns `429` after the configured limit.
- Redis outage does not break URL creation.
- Redis outage does not break redirects that can be resolved from PostgreSQL.

## Useful Commands

Start local infrastructure:

```powershell
docker compose up -d
```

Stop local infrastructure:

```powershell
docker compose down
```

Stop and remove local volumes:

```powershell
docker compose down -v
```

Check PostgreSQL:

```powershell
docker exec url-shortener-postgres pg_isready -U postgres -d url_shortener
```

Check Redis:

```powershell
docker exec url-shortener-redis redis-cli PING
```

Inspect a cached short URL:

```powershell
docker exec url-shortener-redis redis-cli GET short-url:<shortCode>
```

Inspect rate-limit TTL:

```powershell
docker exec url-shortener-redis redis-cli TTL rate-limit:create-url:user:<userId>
```

## GitHub Notes

The repository should commit:

- Source code under `src/`.
- Gradle wrapper files.
- `build.gradle.kts`.
- `settings.gradle.kts`.
- `docker-compose.yml`.
- Liquibase migrations.
- `.gitignore`.
- `.gitattributes`.
- `README.md`.

The repository should not commit:

- `.gradle/`
- `build/`
- IDE workspace metadata
- `.env` files
- logs
- generated jars
- local database dumps
- local Docker data directories

## Non-Goals

This project intentionally does not include:

- Microservices
- Kafka or RabbitMQ
- Kubernetes
- OAuth2/social login
- Payment/subscription logic
- QR generation
- Geo analytics
- Browser/device analytics
- External analytics databases

The goal is a focused backend that demonstrates production-style engineering decisions without turning into a distributed URL-shortening platform clone.
