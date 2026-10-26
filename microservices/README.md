# CameraHub microservices

Six independent Spring Boot applications, five PostgreSQL databases and one HTTP gateway.
The root `pom.xml` builds this platform. Obsolete monolith source and scripts have been
removed from the active tree. A verified local backup is retained in
`.local/backups/legacy-before-cleanup-20261026.zip`, outside version control.

| Application | Responsibility | Port | Database |
| --- | --- | --- | --- |
| api-gateway | Browser API routing, header filtering, request bounds | 8080 (staging 8086) | none |
| identity-service | Accounts, login, profile, Google linking, addresses, sessions | 8081 | camerahub_identity |
| marketplace-service | Catalog, drafts, photos, inventory reservations | 8082 | camerahub_marketplace |
| moderation-service | AI review, duplicate index, human review queue | 8083 | camerahub_moderation |
| commerce-service | Checkout saga, orders, simulated payments, reviews, buyback | 8084 | camerahub_commerce |
| ai-advisor-service | AI consultation, HTTP catalog client, persistent chat and usage | 8085 | camerahub_advisor |

Each application has its own JAR, process, configuration, schema migration and database
credentials. `service-support` shares only transport, authentication integration and
operational infrastructure; it does not contain business entities or repositories.

## Layers

```text
microservices/
  identity-service/
    src/main/java/vn/camerahub/identity/
      IdentityApplication.java
      controller/    # HTTP endpoints and input validation
      service/       # Business rules and transaction boundaries
      repository/    # JpaRepository + custom fragments for atomic PostgreSQL operations
      entity/        # JPA @Entity models and composite IDs
      mapper/        # MapStruct entity -> response DTO
      dto/           # Registration, profile and address API contracts
      config/        # Security, sessions and Google login
      util/
    src/main/resources/db/migration/
  marketplace-service/   # same layer structure; entity/Product
  moderation-service/    # same layer structure; entity/ReviewJob
  commerce-service/      # same layer structure; entity/Order
  ai-advisor-service/    # same layer structure; entity/AdvisorConversation
  api-gateway/           # controller + service; no business database
  service-support/
  tools/ImportLegacy.java
  compose.yaml
  Dockerfile
```

Flow: `Controller -> Service -> JpaRepository -> PostgreSQL`; mappers produce response
DTOs. JPA/Hibernate validates the existing schema (`ddl-auto=validate`); Flyway owns
migrations. Lombok supplies entity accessors and constructor injection. MapStruct
generates mapping implementations with unmapped target fields treated as build errors.
Request DTO records use Jakarta Bean Validation. Atomic leases, snapshots and upserts
remain native PostgreSQL queries in Spring Data custom fragments through EntityManager.
Outbox/inbox and Spring Session use infrastructure JDBC within the local transaction.
See [the Java structure and API contract](../docs/BACKEND_STRUCTURE.md).
Each business service owns its `exception/` package and domain error enum. Shared
`AppException`/`ErrorCode` contracts and centralized response handling keep HTTP status,
stable `errorCode`, backend `message` and field `errors` consistent across services.
See [response and exception conventions](../docs/API_RESPONSE_CONTRACT.md).

## Run on Windows

Requires Java 21 and Docker Desktop with Linux containers. From the repository root:

In Windows CMD, run `backend.cmd Start`, `backend.cmd Status`,
`backend.cmd Logs marketplace` or `backend.cmd Stop`. The launcher uses bundled Windows
PowerShell 5.1; PowerShell 7 is optional. After Java edits, use `backend.cmd Build`
followed by `backend.cmd Start`.

```powershell
./scripts/Invoke-Microservices.ps1 -Task Build
./scripts/Invoke-Microservices.ps1 -Task Start
./scripts/Invoke-Microservices.ps1 -Task Status
./scripts/Invoke-Microservices.ps1 -Task Logs -Service marketplace
./scripts/Invoke-Microservices.ps1 -Task Stop
```

`Build` verifies Maven modules and builds images. `Start` launches those images;
run `Build` after editing Java. Docker named volumes persist service databases.
Scripts create random, service-specific passwords and the internal key in ignored
`.local/microservices.env`. Existing `.env` supplies OpenAI and Google settings.
`GATEWAY_PORT` in the platform environment chooses 8086 for staging or 8080 for the
frontend's normal `/api` proxy. Service and database host ports bind to loopback.

The five database host ports are 15431–15435. Each database has its own container,
credentials and volume. Service URLs inside Docker use container DNS names. Outside
Docker, override each application's `<SERVICE>_DB_URL`, `_DB_USER`, `_DB_PASSWORD`,
`SERVICE_INTERNAL_KEY` and service URLs to run its JAR directly.

## Existing data and cutover

With the service schemas initialized, import a read-only snapshot of the original DB:

```powershell
./scripts/Invoke-Microservices.ps1 -Task Import
./scripts/Invoke-Microservices.ps1 -Task Index
./scripts/Invoke-Microservices.ps1 -Task Audit
```

Import copies supported application data and preserves IDs, password hashes, financial
amounts and payment stages. It never updates the original DB. Existing product photos
remain in `storage/product-media`, mounted only by Marketplace. Moderation retrieves
image bytes over an authenticated private API. Accounts project only public identity
fields into Marketplace. Inspection results become product read-model JSON. Tables
for unimplemented legacy business flows remain available in the original database.

Import is for initial migration, before new traffic. It skips existing rows and is
not a continuous synchronizer. Pause writes on the old application for a final
snapshot. Never run the monolith and new Gateway against live user traffic in parallel.
Moderation workers remain paused until index bootstrap finishes, preventing approval
against an empty historical duplicate index. After testing, stop the old backend,
change `GATEWAY_PORT` to 8080, start the platform and reload the frontend/login.

Rollback: restore the previous monolith source and build configuration from the local
backup before stopping the platform and launching the fallback application. Because the
databases are now independent, new platform writes must be reconciled before switching
back; the original DB does not automatically receive them. Do not remove Docker volumes
to restart an application. Back up each service database and media before deletion.

## Service communication and security

This implementation uses **HTTP outbox/inbox**, not RabbitMQ. Events are written in the
same local transaction as business changes, delivered with leases and retries, and
deduplicated in the receiving DB. Failed records remain available under private
`GET /internal/outbox` and `POST /internal/outbox/{eventId}/retry` endpoints. An event
retained after 12 failed attempts requires operator retry. There is no cross-database
join or foreign key. Checkout uses a persisted saga with reservation reconciliation;
a transport timeout never releases inventory without Commerce confirming cancellation.

Identity issues RS256 JWT access tokens (15 minutes) and rotates opaque refresh tokens
(7 days). Each service verifies signature, issuer, audience and token type, then checks
revocation/current roles at Identity. Browser cookies are HttpOnly/SameSite=Lax;
cookie-authenticated mutations require CSRF. Bearer API clients use JWT authentication.
Refresh is `POST /api/auth/refresh` with the refresh cookie and CSRF token. Logout revokes
the session across services. Old browser sessions must log in again after the JWT cutover.
Private keys are mounted only in Identity; other services receive the public key.
`Build`/`Start` create the key pair once in ignored `.local/jwt/`; preserve these files.
Google OAuth login remains available. This is an application JWT issuer, not a complete
OIDC authorization server. No Redis is required.
Private endpoints require `X-CameraHub-Key`; Gateway strips visitor-supplied internal
and forwarding headers, rejects internal routes and does not attach internal keys to
public requests. Production still needs TLS, managed secrets, backups, monitoring and
private ingress rather than publishing service/database ports.

## Moderation

Submitting a photographed draft sends an immutable revision to Moderation. It checks
normalized text, an HMAC serial token and image SHA/perceptual fingerprints, then asks
the configured AI to review the content and compare candidate photos. High-confidence
approval publishes; clear duplicates or violations are rejected. Unavailable AI or
uncertain results go to Admin. Admin decisions require the displayed revision; stale
decisions cannot update newer content. Rejected duplicates cannot be approved manually.
An Admin request for additional information returns the draft to `NEEDS_REVISION`.

Image fingerprints catch reused and mildly transformed photos; AI comparison is an
additional signal, not a guarantee of recognizing every crop, edit or identical device.

## Verify

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress clean verify
./scripts/Test-Authentication.ps1
./scripts/Test-Microservices.ps1 -BaseUrl http://127.0.0.1:8080 -ExerciseRestart
./scripts/Invoke-Microservices.ps1 -Task CleanupSmoke
```

The API smoke suite creates explicitly named test accounts in the new databases. The
test scripts require PowerShell 7 (`-Form` and `-SkipHttpErrorCheck`); normal service
Build/Start/Stop commands work with Windows PowerShell 5.1 through `backend.cmd`.
Deterministic human-fallback checks expect `AI_ENABLED=false` in the platform env;
restore `AI_ENABLED=true` and restart after running it. It checks authentication,
CSRF, internal-route filtering, profile edits, media duplicate rejection, Admin review,
checkout idempotency, inventory conflicts, cancellation, addresses and chat history.
Results are stored in ignored `.local/microservices-smoke-result.json`.
`CleanupSmoke` closes only the explicitly named `micro-…@test.invalid` fixture accounts,
removes their temporary Admin role and hides their listings. `Audit` compares supported
original rows with the new databases without writing either side. `-ExerciseRestart`
temporarily stops Moderation and verifies pending submission delivery after restart.
