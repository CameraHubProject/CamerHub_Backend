# CameraHub Marketplace MVP

EXE CP3 prototype: catalog, accounts, seller listings, cart, simulated checkout, orders, addresses and buyback requests. All money is **SIMULATED**. See [marketplace API and remaining integrations](docs/PROTOTYPE_MARKETPLACE.md) and [F01 identity](docs/F01_IDENTITY.md).

Optional demo fixtures: [accounts, passwords and eight sample listings](docs/DEMO.md). Run `scripts/Seed-Demo.ps1` manually against the dedicated development database; startup and migrations never seed demo accounts.

## Layout

- `D:\CamerHub_Backend`: this backend Git repository; Java 21, Spring Boot 3, Maven Wrapper, `pom.xml` and `src/` at the root.
- `D:\CameraHub_Frontend`: separate frontend Git repository; React 19, Vite, TypeScript, Tailwind 4, Router, TanStack Query.
- `D:\DB_Of_CameraHub`: original database source repository, unchanged.
- This backend repository retains `db/` (unchanged reference copies), `design/stitch_cp3/`, `docs/`, and optional Docker Compose.

## Requirements

Java 21 JDK and Node 22.12+ (Node 24 also works). Maven is downloaded by the wrapper.
Use a dedicated **empty** Neon PostgreSQL 16+ development database, or optional local PostgreSQL 16 Docker.
[Spring Boot requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
This workspace uses a separate Neon `camerahub_mvp_dev` database on PostgreSQL 18.6; the existing `neondb` is preserved.
Flyway is pinned to 11.20.3 for PostgreSQL 18 support; the unchanged source schema targets PostgreSQL 16+.

## Neon configuration (PowerShell, repository root)

1. If `.env` does not exist: `Copy-Item .env.example .env`.
2. Fill `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` locally. Keep credentials separate; never paste them into chat.
   URL format: `jdbc:postgresql://YOUR_DIRECT_HOST/YOUR_EMPTY_DATABASE?sslmode=require&channelBinding=require`.
   Use the direct endpoint (no `-pooler`) because migration safety uses session advisory locks.
   [Neon connection guidance](https://neon.com/docs/get-started/connect-neon).
3. Set `$env:JAVA_HOME` to your Java 21 JDK folder if it is not detected under `Program Files\Java`.

`.env`, `application-local.properties`, build output, and storage are ignored by Git. Scripts read `.env` without evaluating shell expressions or printing values.

## Build and test

```powershell
.\scripts\Invoke-Backend.ps1 Verify
npm.cmd --prefix D:\CameraHub_Frontend ci
npm.cmd --prefix D:\CameraHub_Frontend test
npm.cmd --prefix D:\CameraHub_Frontend run build
.\scripts\Invoke-Backend.ps1 Integration
```

`Verify` builds the backend and runs unit/source-integrity tests without a database.
`Integration` explicitly connects to the configured database, performs the guarded Flyway V1 migration,
checks all 46 tables and original seeds (4 roles, 8 inspection checks, MOCK shipping), checks repeat migration, and tests HTTP health.
It does not drop/reset the database. Use the dedicated development database; F01 test accounts and evidence are rolled back automatically.

## Run (two PowerShell terminals)

```powershell
Set-Location D:\CamerHub_Backend
.\scripts\Invoke-Backend.ps1 Run
```

```powershell
Set-Location D:\CameraHub_Frontend
npm.cmd run dev
```

Frontend: `http://127.0.0.1:5173`; backend: `http://127.0.0.1:8080/api/health`;
Swagger: `http://127.0.0.1:8080/swagger-ui.html`. Vite proxies `/api` to port 8080.
Spring Boot imports the root `.env` as a properties file, so direct IDE runs also load the local credentials.
For IntelliJ, run `CameraHubApplication` with Java 21 and working directory `D:\CamerHub_Backend`.
An optional ignored root `application-local.properties` can override Spring properties locally.
The helper script also exports `.env` for the standalone database utilities.

## Migration safety

V1 is byte-identical to `db/001_full_schema.sql`, including its transaction wrapper.
Its adjacent Flyway config disables Flyway's extra transaction wrapper. SQL is never mounted as Docker init SQL.
Flyway history is in `public`; the 43 original application tables and three V2 additions remain in `camerahub`.
Startup serializes cooperating migrators using a PostgreSQL advisory lock, rejects PostgreSQL older than 16,
and refuses V1 when unrelated application objects or an existing CameraHub schema are present.
An existing successful V1 is validated using Flyway checksums. Failed SQL must be reviewed with the user; do not edit the source or repair history automatically.
Hibernate uses `ddl-auto=validate`, SQL auto-init is off, Flyway baseline is off, and clean is disabled.
No JPA entities are mapped yet; complete entity-to-column validation is deferred to their implementation phases.

## Optional local PostgreSQL 16

Set a local `POSTGRES_PASSWORD` in `.env`, then `docker compose up -d --wait postgres`.
Set `DB_URL=jdbc:postgresql://127.0.0.1:5432/camerahub_dev`, `DB_USERNAME=camerahub`, and the matching `DB_PASSWORD`.
Run `Integration` or `Run` above. A persistent named volume keeps data. Stop using `docker compose stop`; do not remove the volume.

The supplied business-flow Word source and a readable Markdown transcription are retained in `docs/`. Current scope and test results are documented in `docs/PHASES.md` and `docs/F01_IDENTITY.md`.
