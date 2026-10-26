# F01 Registration login and seller verification

> Tài liệu tham khảo bản monolith trước khi tách. Mã nguồn và công cụ cũ đã được nén vào `.local/backups/legacy-before-cleanup-20261026.zip`. Lệnh chạy và kết quả kiểm thử hiện tại: [microservices](../microservices/README.md). Xác minh người bán đã được bỏ khỏi ứng dụng hiện tại.

Implemented against `CameraHub_12_Business_Flows_v3_2.md`, transcribed from the supplied Word source retained next to it. All 12 flow definitions are available; F01 is the current implementation stage. The original schema and V1 migration remain unchanged.

## Try the flow

Start the backend with `scripts/Invoke-Backend.ps1 Run` and the frontend with `npm.cmd run dev` in `D:\CameraHub_Frontend`.

- Home: `http://127.0.0.1:5173/`
- Register: `/dang-ky`
- Login: `/dang-nhap`
- Account and seller verification: `/tai-khoan`
- Admin review queue: `/quan-tri/xac-minh`

Register with a valid email, full name and password of 12–72 characters (maximum 72 UTF-8 bytes). Phone is optional. New users receive the database `BUYER` role, exposed as `USER` through the API. Registration cannot grant privileged roles. It does not mark the email as verified or send an email.

Authenticated users can upload one JPG, PNG or PDF of at most 5 MiB as their seller verification evidence. The server checks file signatures rather than trusting names or MIME types. Pending or verified applications block duplicate submission. NEEDS_REVISION permits replacing the evidence on the existing application; REJECTED permits a new application. The previous review note remains visible when resubmitting. The server serializes submissions and reviews on the seller row.

An Admin downloads the evidence and chooses VERIFIED, NEEDS_REVISION or REJECTED with a required reason. Only pending applications can be reviewed. Admins cannot approve their own applications. VERIFIED grants SELLER while retaining BUYER. Accounts expose SELLER authority only while an approved verification exists. All decisions and submissions produce audit records. Queue responses are bounded to the oldest 100 pending/revision applications.

## Provision an initial development Admin

Register the intended account first. No default Admin, fixed password or public role-grant endpoint is created. A database owner can run the following parameterized pattern in their SQL client, replacing the example address with the intended registered account:

```sql
BEGIN;
INSERT INTO camerahub.user_roles(user_id, role_code)
SELECT id, 'ADMIN' FROM camerahub.users
WHERE lower(email) = lower('YOUR_REGISTERED_EMAIL') AND account_status = 'ACTIVE'
ON CONFLICT DO NOTHING;
COMMIT;
```

The person executing this statement must verify the selected account first. The application refreshes roles on every request, so signing out is unnecessary. No account was promoted automatically during implementation. Integration fixtures grant test-only Admin authority inside rolled-back transactions.

## Session and API contract

Authentication uses server-side sessions with a 30-minute inactivity timeout. The cookie is HttpOnly, SameSite=Lax, and restricted to cookie tracking. Set `SESSION_COOKIE_SECURE=true` with HTTPS deployment. Restarting this single-instance development server invalidates sessions. Passwords use BCrypt with cost 12. Browser storage never receives the password hash or an authentication token.

All mutations require CSRF protection, including registration, login and logout. First GET `/api/auth/csrf`; send the returned token under its returned `headerName`. Fetch a fresh token after login/logout. The frontend fetches it before each mutation. Session IDs rotate on login. Suspended/closed accounts and revoked roles take effect on the next request.

| Method | Endpoint | Access / payload |
| --- | --- | --- |
| GET | `/api/auth/csrf` | Public masked CSRF token |
| POST | `/api/auth/register` | JSON email, password, fullName, optional phone; returns 201 |
| POST | `/api/auth/login` | Form-urlencoded email and password; returns 204 or 401 |
| POST | `/api/auth/logout` | CSRF-protected session invalidation; returns 204 |
| GET | `/api/auth/me` | Current private profile; 401 when unauthenticated |
| GET | `/api/seller-verification` | Current account's applications |
| POST | `/api/seller-verification` | Multipart file field; returns 201 |
| GET | `/api/seller-verification/{id}/evidence` | Owner or Admin; attachment, no-store; other users receive 404 |
| GET | `/api/admin/seller-verifications` | Admin review queue |
| POST | `/api/admin/seller-verifications/{id}/review` | Admin; JSON decision and note |

Session and verification JSON never exposes password hashes or private storage keys. Evidence downloads use attachment/octet-stream, no-store and Spring's nosniff header. The framework's form authentication handles security-context persistence and session fixation protection; CSRF remains enabled. References: [Spring Security form login](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/form.html), [CSRF](https://docs.spring.io/spring-security/reference/6.5/servlet/exploits/csrf.html).

## Development storage and remaining scope

Evidence uses the private local development adapter in the ignored `storage/private` directory. `camerahub.storage.private-root` overrides the directory. It has no static/public route; files are downloaded only after checking identity and ownership. Failed database transactions delete newly uploaded files, and replacement evidence is removed only after commit. The upload adapter is not connected to a remote Object Storage provider. Configure that provider and its access controls before a hosted deployment; scanning uploaded content, email verification, password recovery, login rate limiting and shared durable session storage are not implemented in this stage. Do not present these features as complete.

F02 listing submission and F03–F12 marketplace operations remain pending. No product creation, checkout or payment behavior is added by F01. The Word source includes conceptual status names that differ from schema enums; future phases must explicitly map these without changing the preserved V1 file. Unresolved payment deadlines, forfeiture rules, inspection holds and inconclusive inspection fee rules remain unresolved.

## Verification

`scripts/Invoke-Backend.ps1 Integration` runs eight unit/source tests and ten database/HTTP integration tests, including six F01 integration cases. F01 tests use random identities and roll back all application records and uploaded evidence. The database seed test permits real registered users now.

Covered: case-insensitive duplicate email, role escalation attempts, BCrypt hashing, invalid and multibyte passwords, real CSRF bootstrap/rotation, session-ID rotation, logout, suspended sessions, private evidence ownership, invalid uploads, revision/resubmission, approval/rejection, no self-approval, repeated-review prevention and immediate role revocation. The unchanged SQL and V1 are checked byte-for-byte. Frontend tests cover CSRF request sequencing, multipart handling and failures; production build checks TypeScript.
