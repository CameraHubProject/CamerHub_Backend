# Kiểm thử backend gộp — 27/10/2026

## Kết quả đã chạy

- Maven Java 21 `clean verify`: 21 unit test đạt, đóng gói thành công `target/camerahub-backend-0.1.0-SNAPSHOT.jar`.
- Bản sao Neon riêng: đối chiếu SHA-256 dữ liệu gốc trong 48 bảng trước/sau migration; giữ nguyên 8 tài khoản, 9 sản phẩm và 2 đơn hàng.
- `Test-Authentication.ps1`: 14 kiểm tra đạt (validation tiếng Việt, email/điện thoại trùng, JWT, cookie HttpOnly, quyền Admin, refresh/logout thu hồi token).
- `Test-UnifiedBackend.ps1`: 17 kiểm tra đạt trên database thử (sửa hồ sơ, CSRF, chặn API nội bộ, AI không kết luận chuyển Admin, Admin duyệt, chặn ảnh trùng giữa hai người bán, checkout thử lại không nhân đôi đơn, không bán trùng hàng, hủy đơn trả hàng, địa chỉ, hội thoại).
- Khởi động lại backend thử: ảnh mới vẫn tải được HTTP 200 từ Neon.
- Khởi động với Neon chính: migration tới V5 thành công, JPA validate thành công, 32 repository được scan; đối chiếu lại 48 bảng gốc không thay đổi dữ liệu cũ. Giữ 8 tài khoản, 9 sản phẩm, 2 đơn, 3 ảnh đã được lưu bền; không còn phân quyền INSPECTOR.

## Giới hạn

Docker Desktop chưa chạy nên chưa build image tại máy. Java/JAR, Flyway, JPA và các luồng API đã được kiểm tra thực tế với Neon. Render hiện còn bản Gateway cũ cho tới khi xuất bản commit mới và nhập các biến môi trường backend gộp. Chưa xác nhận website production hoạt động với bản gộp.

AI được tắt khi chạy kiểm thử tích hợp để kiểm tra chính xác luồng chuyển Admin và tránh gọi API trả phí. Bộ unit test kiểm tra phân loại ảnh và cấu trúc phản hồi AI; chưa thực hiện một lượt duyệt trực tiếp với nhà cung cấp AI trong lần chuyển đổi này.

## Khôi phục và bí mật

Bản sao hàng cũ, metadata schema và kết quả kiểm tra được lưu riêng trong `.local/unified`, không đưa lên Git. Mã microservice trước khi gộp đã nén và kiểm tra CRC trong `.local/backups/microservices-before-unification-*.zip`; có thể xem lại từ lịch sử Git. Không xóa database/volume Docker hay ảnh gốc tại máy.
