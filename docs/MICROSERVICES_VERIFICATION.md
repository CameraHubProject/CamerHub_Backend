# Microservice conversion verification

Date: 26 October 2026 (Asia/Bangkok).

- Root Maven reactor: `clean verify` passed, including 24 Java tests (JWT, validation,
  domain error codes, cross-service error propagation, backend success messages,
  MapStruct projections, boundaries, Gateway, brands and AI parsing).
- Architecture boundary test: controller/service/repository/entity/dto packages exist;
  mapper packages exist and business modules have no JdbcTemplate imports;
  controllers do not import repositories;
  business modules do not import another service's business code.
- Gateway tests: route ownership, internal route rejection, header filtering, CSRF/cookie
  forwarding and byte-preserving HTTP proxying.
- Frontend: production build passed; 60 tests passed in 13 files, including ApiError,
  field messages, backend notifications and Google status messages.
- Docker runtime: six application containers and five PostgreSQL containers started;
  each application's HTTP health endpoint returned UP.
- JPA API smoke suite: 23 checks passed, including account login/profile edits, invalid CSRF,
  internal-route/key spoof rejection, photo upload, AI fallback queue, revision-bound
  Admin decision, duplicate photo rejection across sellers, draft privacy, checkout
  idempotency, reserved-inventory conflict, cancellation compensation, addresses and
  independent advisor history. Submission was retained while Moderation was offline
  and delivered after restart (`-ExerciseRestart`). Fixture photos differ perceptually
  between runs, while duplicate-photo checks reuse the exact same photo within one run.
- Authentication smoke suite: 14 checks passed for field validation, JPA registration,
  safe DTOs, signed JWT, HttpOnly cookies, Bearer requests, downstream verification,
  tamper rejection, Admin authorization, refresh rotation and cross-service revocation;
  duplicate email/phone and password confirmation return distinct Identity error codes.
- All five Hibernate persistence units validated their Flyway-managed schemas. Existing
  V1 migrations were preserved; Identity V2 adds refresh-session metadata.
- A live AI consultation returned two conversation messages using the HTTP catalog.
  After restarting only Advisor, the same conversation was loaded from its database.
- Original data import: 8 users, 12 role memberships, 1 address, 1 external identity,
  22 audit records, 35 brands, 8 models, 9 products, 3 media records, 12 product history
  records, 2 orders, 4 payment records and 2 deposits. Original supported rows were
  compared field by field against the new databases with zero mismatches. Derived
  fingerprint fields and saga batch mappings are excluded from that legacy comparison.
- Test fixture accounts were closed, their temporary Admin roles removed and their
  listings hidden. Existing business rows were preserved.

The deterministic moderation smoke suite used AI disabled to exercise human fallback;
AI was restored afterward. Live catalog/chat and durable conversation storage were
verified separately. This is local development verification, not production load,
disaster recovery or complete image-manipulation coverage. No claim is made that image
similarity catches every edit. HTTP event transport, JPA repositories and Identity JWT
issuance/revocation are implemented. RabbitMQ, Redis and a full OIDC server are not deployed.

Reproduce using the commands in [microservices/README.md](../microservices/README.md).
Temporary logs, generated migration scripts and smoke result files were removed during
project cleanup. The obsolete monolith source was archived with verified SHA-256 hashes
in `.local/backups/legacy-before-cleanup-20261026.zip`; active runtime configuration,
uploaded media and service databases were retained.
