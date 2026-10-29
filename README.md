# CameraHub Backend

Một ứng dụng **Spring Boot 3.5 / Java 21**, dùng **một database Neon PostgreSQL** và triển khai thành **một service Render**. Frontend ở Vercel gọi `/api` qua rewrite đến backend.

## Cấu trúc

```text
src/main/java/vn/camerahub/
├── CameraHubApplication.java
├── identity/        # tài khoản, JWT, Google login, địa chỉ
├── marketplace/     # danh mục, tin đăng, ảnh, giữ chỗ thiết bị
├── moderation/      # AI duyệt bài, chống trùng, hàng chờ Admin
├── commerce/        # đơn hàng, thanh toán mô phỏng, đánh giá
├── ai/              # AI tư vấn, hội thoại, hạn mức
└── shared/          # cấu hình, ApiResponse, exception, sự kiện
src/main/resources/
├── application.properties
├── ValidationMessages.properties
└── db/unified/      # Flyway migration cho backend gộp
```

Các module chia `controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `exception` khi có nghiệp vụ tương ứng. JPA/Hibernate quản lý entity, Lombok giảm mã lặp, MapStruct ánh xạ DTO, Bean Validation kiểm tra đầu vào. Backend trả mã lỗi và thông báo qua `ApiResponse`, `ErrorCode`, `AppException`, `GlobalExceptionHandler`.

Đây là **modular monolith**: module gọi nhau trực tiếp trong cùng JVM và dùng chung datasource. Các sự kiện duyệt bài vẫn lưu outbox để có thể thử lại sau khi ứng dụng khởi động lại.

## Chạy tại máy

Cài Java 21, đặt DB_URL, DB_USERNAME, DB_PASSWORD của Neon vào `.env` theo `.env.example`. Tại CMD:

```bat
backend.cmd Build
backend.cmd Start
backend.cmd Status
backend.cmd Logs
backend.cmd Stop
```

Không bắt buộc Docker hoặc PowerShell 7. API: http://127.0.0.1:8080/api/health. Frontend chạy riêng bằng `npm run dev` tại repo frontend. Sau khi sửa Java, Stop → Build → Start.

## Triển khai

Xem [hướng dẫn Render](docs/DEPLOY_RENDER.md) để cập nhật service **CamerHub_Backend** đang có. Dockerfile `deploy/render/Dockerfile` build một JAR duy nhất. `render.yaml` chỉ mô tả một backend, không cấp thêm database. Vercel giữ rewrite `/api/*` đến https://camerhub-backend.onrender.com.

Dữ liệu và ảnh sản phẩm được lưu bền trong Neon. Các file ảnh gốc tại `storage/product-media` được giữ để sao lưu; backend có thể nhập chúng vào database khi khởi động tại máy chứa file gốc. JWT và khóa dấu vân tay serial được giữ ổn định trong `.local/render/keys`. Không commit `.env`, `.local`, khóa JWT hoặc dữ liệu người dùng.

Project Neon CameraHub chỉ dùng database `camerahub_mvp_dev` cho tất cả module và cho cả local/Render. Không tạo các database `camerahub_identity`, `camerahub_marketplace`, `camerahub_moderation`, `camerahub_commerce`, `camerahub_advisor` hoặc dùng lại `neondb`. Database hệ thống `postgres` do Neon quản lý được giữ nguyên. `.env` và `.local/render/unified-backend.env` phải trỏ đến cùng database chính.

## Kiểm tra

```powershell
./mvnw.cmd --batch-mode --no-transfer-progress verify
./scripts/Test-Authentication.ps1 -BaseUrl http://127.0.0.1:18080
```

Script API tạo tài khoản thử, chỉ chạy trên database thử. `Test-UnifiedBackend.ps1` kiểm tra đăng bài, ảnh trùng, duyệt và checkout trên bản sao Neon; phần tạo Admin bị khóa theo tên database thử, không dùng cho production.

Không chạy smoke test ghi dữ liệu vào database chính. Các cấu hình database thử cũ đã được lưu trong bản sao lưu khi dọn Neon. `Prepare-UnifiedTestDatabase.java` mặc định từ chối tạo database; tùy chọn `--allow-extra-database` chỉ dành cho project Neon thử nghiệm riêng, cần xóa database thử sau khi dùng. `mvnw.cmd verify` vẫn dùng được mà không tạo database Neon.
