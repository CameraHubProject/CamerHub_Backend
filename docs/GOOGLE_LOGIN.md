# Đăng nhập và đăng ký Google

> Tài liệu tham khảo bản monolith trước khi tách. Mã nguồn và công cụ cũ đã được nén vào `.local/backups/legacy-before-cleanup-20261026.zip`. Lệnh chạy và kết quả kiểm thử hiện tại: [microservices](../README.md). Xác minh người bán đã được bỏ khỏi ứng dụng hiện tại.

## Cấu hình phiên local

Backend đọc cấu hình Google từ `.env` bị Git bỏ qua. Client secret không nằm trong source, response API hoặc frontend bundle. Chỉ yêu cầu `openid email profile`, không xin quyền đọc/gửi Gmail.

```properties
GOOGLE_LOGIN_ENABLED=true
GOOGLE_CLIENT_ID=YOUR_WEB_CLIENT_ID
GOOGLE_CLIENT_SECRET=YOUR_CLIENT_SECRET
GOOGLE_REDIRECT_URI=http://127.0.0.1:5173/api/auth/google/callback
FRONTEND_URL=http://127.0.0.1:5173
```

Phiên này đã ghi các thông tin người dùng cung cấp vào `.env` local và chạy backend ở 8081. Vite ở 5173 proxy `/api` sang backend, vì vậy callback dùng cổng **5173**, không phải 8081.

Trong Google Cloud, chọn đúng project → Google Auth Platform → Clients → OAuth client kiểu **Web application**. Trong **Authorized redirect URIs**, thêm chính xác:

```text
http://127.0.0.1:5173/api/auth/google/callback
```

Nhấn Save. Nếu ứng dụng ở chế độ Testing và Google yêu cầu test user, thêm email thử trong Audience → Test users. Không cần bật Gmail API cho tính năng đăng nhập. Luôn mở web bằng `127.0.0.1:5173` trong cấu hình này; cookie của `localhost` là phiên khác. Khi đổi domain, thay cả redirect URI phía Google và biến môi trường backend, cấu hình HTTPS/cookie secure và reverse proxy cùng origin cho `/api`.

**Kết quả kiểm tra Google thật:** sau khi chủ project thêm URI, Google đã chấp nhận callback và hiển thị trang **Sign in to continue to CameraHub**, không còn redirect_uri_mismatch. Người dùng cần nhập tài khoản Google của mình để hoàn tất lần đăng nhập thật. Chưa xác nhận token exchange bằng Google thật; các kiểm thử callback/token dùng provider local có JWT ký số. Client secret không cấp quyền sửa Google Cloud.

## Luồng chức năng

1. `/dang-nhap` và `/dang-ky` đều có nút Google, cùng dùng authorization-code + OIDC do Spring Security xử lý.
2. Google mới chưa có email trong CameraHub: tạo user với quyền BUYER (API hiển thị USER), email đã xác minh và mật khẩu ngẫu nhiên không cung cấp cho người dùng. Không tự cấp SELLER/ADMIN.
3. Google đã liên kết: tra bằng `provider + sub` ổn định, không dùng email để đổi sang tài khoản khác.
4. Email đã tồn tại nhưng chưa liên kết: không tự đăng nhập/gộp. Giữ bằng chứng Google đã xác minh trong session tối đa 5 phút; yêu cầu mật khẩu CameraHub cũ. Tối đa 5 lần sai; hết hạn phải bắt đầu Google lại. Giữ nguyên user ID, mật khẩu, vai trò, tin đăng và đơn hàng.
5. Sau khi thành công, lưu principal local UUID vào session hiện có, xoay session ID và CSRF, chuyển `/tai-khoan`. Đăng xuất, khóa tài khoản và refresh quyền vẫn dùng cơ chế cũ.
6. Hủy/ID token không hợp lệ/email chưa xác minh/tài khoản bị khóa: trả trang đăng nhập với thông báo tiếng Việt cố định; không đưa token hoặc chi tiết lỗi Google vào query frontend.

Spring kiểm tra chữ ký ID token, issuer/audience/expiry, state và nonce. Access/refresh token được bỏ sau đăng nhập, không lưu database hoặc session. Không có tùy chọn chuyển hướng do client tùy ý cung cấp.

## Endpoint và schema

| Endpoint | Chức năng |
| --- | --- |
| GET `/api/auth/google/status` | Trạng thái cấu hình và email đang chờ xác nhận trong chính session này; no-store |
| GET `/api/auth/oauth2/google` | Khởi tạo authorization request và chuyển Google |
| GET `/api/auth/google/callback` | Callback do Spring Security xử lý |
| POST `/api/auth/google/link` | Xác nhận mật khẩu tài khoản cũ, cần CSRF; body chỉ gồm password, không nhận email/sub từ frontend |

Migration `V3__google_identities.sql` thêm bảng `camerahub.auth_identities`, khóa chính `(provider, subject)` và unique `(provider, user_id)`. Không sửa/xóa bảng cũ hoặc V1/V2, không lưu Google token. Schema hiện có 47 bảng. V3 đã áp dụng thành công; dữ liệu thử trong integration test được rollback.

## File thay đổi

- `pom.xml`: thêm starter OAuth2 Client của Spring Boot.
- `config/SecurityConfiguration.java`: nối OAuth vào filter chain, giữ CSRF/form login và quyền cũ.
- `config/GoogleLoginConfiguration.java`: đăng ký Google khi enabled, cấu hình redirect và origin cố định.
- `identity/GoogleAccountService.java`: tạo/tra/liên kết tài khoản theo Google sub.
- `identity/GoogleOidcUserService.java`: dùng claims đã được Spring xác minh và ánh xạ principal.
- `identity/GoogleLoginHandlers.java`: thành công/thất bại, session và chuyển hướng.
- `identity/GoogleLoginController.java`: status và xác nhận liên kết bằng mật khẩu.
- `identity/DiscardingGoogleClientRepository.java`: không giữ Google access/refresh token.
- `application.properties`: cấu hình bằng biến môi trường, mặc định tính năng tắt khi chưa cấu hình.
- `V3__google_identities.sql`: migration bổ sung.
- `DatabaseMigrationIT.java`: xác nhận schema 47 bảng.
- `identity/GoogleLoginIT.java`: sáu kiểm thử luồng thật qua filter, local OAuth provider và JWT RSA ký số.

Các đường dẫn Java tương đối với `src/main/java/vn/camerahub` (test tương đối `src/test/java/vn/camerahub`).

## Kiểm tra và chạy

```powershell
# D:/CamerHub_Backend
$env:SERVER_PORT='8081'
.\scripts\Invoke-Backend.ps1 Run

# Terminal khác: toàn bộ unit + integration
.\scripts\Invoke-Backend.ps1 Integration
```

Đã qua **32 kiểm thử backend**: 8 unit + 24 integration. Sáu kiểm thử Google dùng loopback provider và JWT ký RSA thật, không dùng tài khoản Google cá nhân: đăng ký/đăng nhập lại theo sub, session rotation/logout, xác nhận email trùng bằng mật khẩu/CSRF, state sai/callback tự phát, audience/nonce sai, email chưa xác minh, tài khoản bị khóa, TTL và giới hạn thử mật khẩu. Bộ kiểm thử frontend đạt 36; build frontend thành công.

Callback Google Cloud đã cấu hình và được Google chấp nhận. Để thử đăng nhập thật: mở `http://127.0.0.1:5173/dang-nhap`, bấm Google rồi tự nhập/chọn tài khoản; với email mới kiểm tra tài khoản USER, đăng xuất rồi đăng nhập lại; với email cũ kiểm tra bước nhập mật khẩu CameraHub trước khi liên kết.

Tham khảo: [Google OIDC](https://developers.google.com/identity/openid-connect/openid-connect), [redirect URI của Google](https://developers.google.com/identity/protocols/oauth2/web-server#authorization-errors-redirect-uri-mismatch), [Spring OAuth2 Login](https://docs.spring.io/spring-security/reference/servlet/oauth2/login/advanced.html).
