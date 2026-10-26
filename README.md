# CameraHub Backend

CameraHub chạy bằng **6 ứng dụng Spring Boot độc lập**, với 5 PostgreSQL database riêng.
Frontend React tiếp tục gọi cùng đường dẫn `/api` qua Gateway tại cổng 8080.

| Service | Phạm vi | Cổng |
| --- | --- | --- |
| api-gateway | Định tuyến API, lọc header và giới hạn request | 8080 |
| identity-service | Tài khoản, tên/số điện thoại, đăng nhập, địa chỉ, session | 8081 |
| marketplace-service | Danh mục, tin đăng, ảnh, giữ chỗ thiết bị | 8082 |
| moderation-service | AI duyệt tin, phát hiện trùng, hàng chờ Admin | 8083 |
| commerce-service | Checkout saga, đơn hàng, thanh toán mô phỏng, đánh giá | 8084 |
| ai-advisor-service | AI tư vấn qua API danh mục, lưu hội thoại và hạn mức | 8085 |

Mỗi service nghiệp vụ chia thành `controller/`, `service/`, `repository/`, `entity/`,
`mapper/` và `dto/`. Entity dùng JPA/Hibernate; repository kế thừa `JpaRepository`;
Lombok sinh getter/setter và constructor; MapStruct sinh mapper khi Maven biên dịch.
DTO đầu vào dùng Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`...).
Cơ chế `ErrorCode`/`AppException` tách theo nghiệp vụ; frontend nhận thông báo từ backend.
Cấu hình Spring Security, JWT RSA, OpenAPI và xử lý lỗi nằm trong các file riêng.
`service-support/` chứa hạ tầng chung, không chia sẻ entity nghiệp vụ giữa service.
Xem [cấu trúc và ví dụ code](docs/BACKEND_STRUCTURE.md).

## Chạy và kiểm thử

Cần Java 21 và Docker Desktop đang chạy Linux Engine. Tại thư mục backend:

Trong CMD, dùng Windows PowerShell có sẵn qua file khởi chạy:

```bat
backend.cmd Start
backend.cmd Status
backend.cmd Logs marketplace
backend.cmd Stop
```

Sau khi sửa Java: `backend.cmd Build`, rồi `backend.cmd Start`. Không cần cài PowerShell 7.

Trong PowerShell:

```powershell
./scripts/Invoke-Microservices.ps1 -Task Build
./scripts/Invoke-Microservices.ps1 -Task Start
./scripts/Invoke-Microservices.ps1 -Task Status
./scripts/Invoke-Microservices.ps1 -Task Logs -Service marketplace
./scripts/Invoke-Microservices.ps1 -Task Stop
```

Build Maven trực tiếp: `./mvnw.cmd --batch-mode --no-transfer-progress verify`.
Chạy frontend tại `D:\CameraHub_Frontend` bằng `npm run dev`, truy cập
[CameraHub](http://127.0.0.1:5173). Health của Gateway:
[API health](http://127.0.0.1:8080/api/health).

Script tạo cặp khóa JWT RSA trong `.local/jwt/`, mật khẩu database và internal key trong
`.local/microservices.env` được Git bỏ qua. `GATEWAY_PORT` chọn 8080 khi sử dụng
frontend, hoặc 8086 để thử nghiệm song song. OpenAI/Google lấy từ file `.env`
hiện có. Không đưa khóa API hoặc mật khẩu lên Git.

## Tài liệu

- [Cấu trúc lớp, quyền sở hữu dữ liệu, Docker, import, audit và rollback](microservices/README.md).
- [ApiResponse, ErrorCode và exception theo nghiệp vụ](docs/API_RESPONSE_CONTRACT.md).
- [Thiết kế kiến trúc và các phương án mở rộng](docs/MICROSERVICE_ARCHITECTURE.md).
- [Kết quả kiểm thử bản chuyển đổi](docs/MICROSERVICES_VERIFICATION.md).
- [Luồng duyệt tin và chống trùng](docs/LISTING_MODERATION.md).
- [Trợ lý AI](docs/AI_ASSISTANT.md).

Sự kiện giữa service dùng HTTP outbox/inbox có lưu bền vững và thử lại. Identity
cấp JWT và refresh token, kiểm tra thu hồi phiên trong PostgreSQL. Trình duyệt dùng
cookie HttpOnly và CSRF; API client có thể dùng Bearer JWT. Thanh toán vẫn là **SIMULATION**.

Mã monolith và công cụ cũ đã được dọn khỏi cây mã nguồn chính. Bản sao lưu có kiểm tra
SHA-256 nằm tại `.local/backups/legacy-before-cleanup-20261026.zip` (không đưa lên Git).
Database gốc và repository `D:\DB_Of_CameraHub` được giữ nguyên. Các bản chạy mới
ghi vào database Docker độc lập; dữ liệu mới không tự đồng bộ ngược về database cũ.
# Deploy public

Cấu hình Render có tính phí nằm trong [render.yaml](render.yaml). Xem [hướng dẫn Render và dự toán](docs/DEPLOY_RENDER.md) trước khi tạo tài nguyên. Docker chạy local và Render là hai môi trường riêng.

