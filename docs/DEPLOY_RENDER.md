# CameraHub: một backend Spring Boot trên Render

Frontend tiếp tục ở https://camerahubweb.vercel.app. Vercel chuyển tiếp `/api/*` đến https://camerhub-backend.onrender.com. Backend có đầy đủ đăng nhập, tài khoản, bài đăng, duyệt bài, giao dịch và AI trong cùng JVM. Dữ liệu và ảnh sản phẩm nằm trong một database Neon hiện có.

## Mã nguồn

`src/main/java/vn/camerahub` chứa các module `identity`, `marketplace`, `moderation`, `commerce`, `ai` và `shared`. Mỗi module chia `controller`, `dto`, `entity`, `mapper`, `repository`, `service`, `exception` theo chức năng thực tế. Đây là modular monolith: các module gọi trực tiếp trong ứng dụng, không cần chạy các microservice độc lập.

## Chạy tại máy

Yêu cầu Java 21 và `.env` có DB_URL, DB_USERNAME, DB_PASSWORD của Neon. Không bắt buộc Docker hay PowerShell 7.

```bat
backend.cmd Build
backend.cmd Start
backend.cmd Status
backend.cmd Logs
backend.cmd Stop
```

Build lại và Stop/Start sau khi sửa mã nguồn. API ở http://127.0.0.1:8080; Swagger ở `/swagger-ui.html` chỉ bật mặc định khi chạy tại máy.

## Cập nhật service Render đang có

1. Đưa phiên bản mã nguồn này lên nhánh được Render theo dõi (hiện là `main`).
2. Trong **CamerHub_Backend → Settings**, Runtime Docker, Root Directory để trống, Dockerfile Path `deploy/render/Dockerfile`, Docker Build Context Directory `.`. Health Check Path `/api/health`.
3. Chạy `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/Prepare-UnifiedRenderEnvironment.ps1` tại thư mục dự án.
4. Trong **Environment → Add from .env**, nhập nội dung `.local/render/unified-backend.env`. File chứa bí mật: không commit, không đưa vào frontend hay gửi vào chat. Giữ nguyên JWT và SERVICE_INTERNAL_KEY giữa các lần deploy.
5. Save và deploy lại commit mới. Không tạo thêm database Render hoặc private service. Biến `SERVICE=api-gateway` cũ không còn được dùng.
6. Kiểm tra `/api/health` trả `service: camerahub-backend`, `/api/public/brands` trả danh sách và `/api/auth/google/status` không còn 503. Mở Vercel kiểm tra danh mục và đăng nhập.

`render.yaml` mô tả duy nhất một backend Free ở Oregon, dùng Neon ngoài Render. Với service đã tạo thủ công, cập nhật tại chỗ, không tạo Blueprint thứ hai. Frontend không cần đổi rewrite hiện tại.

## Database và ảnh

Flyway dùng riêng `camerahub.unified_schema_history`, giữ lịch sử migration cũ. Migration thêm bảng/cột phục vụ backend gộp và giữ ID, giá, trạng thái các đơn cũ. JPA chỉ `validate`, không tự xóa/tạo lại schema. Trước khi áp dụng lần đầu cần sao lưu và chạy thử trên bản sao database.

Ảnh mới được chuẩn hóa PNG rồi lưu ở `product_media_data.content` trong Neon, kèm metadata và dấu vân tay chống trùng. `LegacyPhotoImporter` tự nhập ảnh cũ từ `storage/product-media` nếu có file phù hợp, không xóa bản gốc. Cần chạy một lần tại máy có ảnh cũ trước khi Render phục vụ ảnh đó. Render không cần ổ đĩa trả phí.

AI là bước duyệt mặc định, phát hiện trùng nội dung/ảnh; trường hợp không tự kết luận chuyển Admin. Khi không có khóa AI hoặc AI lỗi, hệ thống chuyển Admin. Backend quản lý validation, JWT, quyền truy cập và thông báo lỗi dạng `ApiResponse`.
