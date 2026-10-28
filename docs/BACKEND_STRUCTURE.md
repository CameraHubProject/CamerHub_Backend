# Cấu trúc backend hiện tại

Backend chạy một Spring Boot application: `vn.camerahub.CameraHubApplication`.

| Module | Trách nhiệm |
| --- | --- |
| identity | Tài khoản, hồ sơ, JWT, quyền, Google login, địa chỉ |
| marketplace | Danh mục, tin đăng, ảnh, giữ hàng |
| moderation | Duyệt AI, nhận diện tin/ảnh trùng, Admin duyệt trường hợp chưa kết luận |
| commerce | Checkout, đơn hàng, thanh toán mô phỏng, đánh giá |
| ai | Tư vấn, lịch sử hội thoại, hạn mức |
| shared | Cấu hình chung, response, exception, sự kiện nội bộ |

Mỗi module có controller nhận HTTP và DTO có `@Valid`; service xử lý nghiệp vụ, quyền và transaction; repository truy cập PostgreSQL; entity ánh xạ bảng; mapper chuyển entity sang DTO. Exception theo nghiệp vụ có ErrorCode riêng, GlobalExceptionHandler thống nhất HTTP status, thông báo và lỗi từng trường. Frontend hiển thị thông báo backend trả về.

`identity/config/SecurityConfig` áp dụng một SecurityFilterChain cho toàn ứng dụng. `shared/config` chứa JWT và các cấu hình dùng chung. Các URL `/internal/**` không được truy cập từ bên ngoài. Module trao đổi trực tiếp qua `LocalInternalClient`, không gọi HTTP tới service khác. Outbox/inbox hỗ trợ thử lại sự kiện duyệt bài.

Một datasource dùng Neon; Flyway ở `src/main/resources/db/unified`, JPA `ddl-auto=validate`. Ảnh lưu tại `product_media_data` để không phụ thuộc ổ tạm Render. Bản thiết kế microservice trước đây là lịch sử; quy trình triển khai hiện tại nằm trong [DEPLOY_RENDER.md](DEPLOY_RENDER.md).
