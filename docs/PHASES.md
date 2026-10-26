# CameraHub implementation status

> Tài liệu tham khảo bản monolith trước khi tách. Mã nguồn và công cụ cũ đã được nén vào `.local/backups/legacy-before-cleanup-20261026.zip`. Lệnh chạy và kết quả kiểm thử hiện tại: [microservices](../microservices/README.md). Xác minh người bán đã được bỏ khỏi ứng dụng hiện tại.

- Phase 0: implemented; project setup, unchanged V1 schema, guarded migration and infrastructure checks.
- Repository layout: backend at `D:\CamerHub_Backend`, frontend at `D:\CameraHub_Frontend`; each keeps its original independent Git repository.
- Phase 1 / F01: registration, session login/logout, private seller verification and Admin review implemented for development. See `F01_IDENTITY.md` for scope and validation. Remote Object Storage is still pending configuration.
- User-requested demo fixtures: seven accounts, four synthetic seller verification files and eight published sample products. `scripts/Seed-Demo.ps1` runs manually; `docs/DEMO.md` lists credentials and demo scenarios.
- Current authorization: implement the Figma prototype faithfully, including its business behavior and missing APIs. This supersedes the older phase-by-phase `next` gate and conflicting rules from the 12-flow source. See `FIGMA_SOURCE_OF_TRUTH.md`.
- Source supplied: `docs/CameraHub_12_Business_Flows_v3.2.docx`, with all 12 flow definitions transcribed to `CameraHub_12_Business_Flows_v3_2.md`.
- The older contract is retained as `db/openapi.yaml`; v3.2 is scheduled for phase 8.
- DB invariants and undecided policy settings remain authoritative in `db/README.md`.
- The schema stores the buyer role as `BUYER`; map API `USER` semantics explicitly in phase 1 without changing SQL.
- Design files are preserved under `design/stitch_cp3`; full theme and shared UI begin in phase 1.

## Home UI delivery

- User requested the marketplace UI after viewing the phase-0 connectivity screen.
- Replaced that screen with the Stitch-based home layout and shared UI components in the separate frontend repository.
- Added a bounded read-only home feed of the newest 24 PUBLISHED listings; no source SQL or demo data was changed.
- Browser checks cover 360px and desktop layout, mobile menu and inspection accordion. Filters use real returned data and integer VND strings.
- F01 now adds account pages and links to the home header; its source flow document is available. Listing creation and other marketplace flows remain scheduled work.
- Frontend now follows the EXE CP3 Figma prototype (home 44:903, catalog 44:897, login/register 44:898/44:899). `/may-anh` adds combined URL filters, sorting and a desktop sidebar/mobile filter panel against the existing feed. The backend/schema are unchanged by the visual update; this does not complete F02 listing creation or full F03/AI functionality.

## Phase 0 verification

- Target: dedicated Neon `camerahub_mvp_dev`, PostgreSQL 18.6 (the source supports 16+); original `neondb` was not modified.
- Flyway V1 succeeded on the new empty database; 43 CameraHub tables, 4 roles, 8 inspection checks and MOCK shipping verified.
- Six backend unit/source tests, three PostgreSQL/HTTP integration tests and three frontend tests passed.
- Backend packaging and frontend production build succeeded; npm audit reported zero vulnerabilities.
- Non-empty database protection was also observed against the existing `neondb`; it refused migration before creating Flyway metadata.
- Original SQL, repository source copy and V1 copy have identical SHA-256 hashes.
- Run instructions and explicit database integration checks are documented in the root README; no destructive database operations are used.
