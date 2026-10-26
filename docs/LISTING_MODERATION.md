# Hồ sơ cá nhân và duyệt tin

Người dùng sửa tên và số điện thoại ở `/tai-khoan?tab=settings`. API `POST /api/auth/me`
chỉ cập nhật tài khoản của phiên đăng nhập; chuẩn hóa số di động Việt Nam, giữ ràng buộc
số điện thoại duy nhất và ghi audit. Số mới dùng được để đăng nhập. Email và quyền không đổi.

Xác minh người bán đã ngừng sử dụng: không có menu, biểu mẫu hay huy hiệu xác minh;
các API xác minh cũ bị chặn. Dữ liệu lịch sử được giữ nguyên.

## Quy trình

1. `POST /api/marketplace/listings` lưu `DRAFT`, chưa công khai.
2. Tải ít nhất một ảnh thực tế. Có thể mở lại bản nháp để chỉnh nội dung, thay ảnh.
3. `POST /api/marketplace/listings/{id}/submit` đối chiếu trùng và đưa vào `AI_REVIEW`.
4. Hai worker xử lý nền; kết quả đạt, độ tin cậy từ 0,95 trở lên → `PUBLISHED`;
   vi phạm rõ ràng → `REJECTED`; chưa rõ, thiếu ảnh, lỗi AI/đối chiếu hoặc thiếu API key → `PENDING_REVIEW`.
5. Admin chỉ thấy `PENDING_REVIEW` cùng lý do chuyển. Yêu cầu bổ sung → `NEEDS_REVISION`,
   người bán mở lại bản nháp và gửi AI kiểm tra lần nữa.

Hàng chờ lưu trong PostgreSQL, tự tiếp tục sau khởi động lại. Lease hết hạn sau 2 phút;
version ngăn kết quả cũ ghi đè. Không giữ transaction khi gọi AI. Khóa giao dịch chung
và đối chiếu lại trước xuất bản ngăn hai tin trùng cùng xuất hiện. Admin cũng không được bỏ qua chặn trùng.
Nội dung và ảnh của tin đang duyệt không được sửa cho tới khi có kết quả.

## Nhận diện trùng

- Nội dung: chuẩn hóa dấu, chữ hoa, dấu câu; so nội dung trùng hoàn toàn, số sê-ri trùng
  và nội dung gần trùng của cùng người bán. Cùng model nhưng mô tả thiết bị khác vẫn được phép.
- Hình ảnh: SHA-256 trên pixel chuẩn hóa; dHash kết hợp sai khác màu và độ biến thiên
  để nhận diện ảnh giống nhau sau thay đổi kích thước/nén nhẹ. Ảnh tải lại do lỗi mạng có tính idempotent.
- AI xem toàn bộ tối đa 8 ảnh tin mới, đối chiếu nội dung và tối đa 2 ảnh/tin của 3 ứng viên
  có tiêu đề gần nhất cùng hãng/loại. Có thể nhận ra nội dung viết lại và ảnh cắt/đổi sáng.
  Kết quả trùng phải trỏ tới một ID ứng viên có thật. Chỉ nghi ngờ thì chuyển Admin.
- Đối chiếu các tin đã gửi, đang bán, đã đặt, đã bán hoặc đã ẩn; không dùng bản nháp/bị từ chối
  làm bằng chứng chặn người khác. Ảnh cũ được lập dấu vân tay khi lần đầu đối chiếu.

Không cam kết nhận diện mọi kiểu chỉnh ảnh hoặc sao chép: ảnh biến đổi mạnh, thay tiêu đề/hãng
để tránh tập ứng viên có thể không được nhận ra. Cần tập ảnh thực tế có nhãn để hiệu chỉnh ngưỡng
và đánh giá tỷ lệ nhận nhầm trước khi mở rộng. Phát hiện ảnh giống không xác nhận quyền sở hữu thiết bị.

## Cấu hình và kiểm tra

Marketplace và Moderation có migration V1 riêng trong từng service. Marketplace sở hữu tin/ảnh, Moderation sở hữu hàng chờ, chỉ mục chống trùng và quyết định duyệt. Sự kiện dùng HTTP outbox/inbox bền vững.

AI dùng OPENAI_API_KEY, OPENAI_MODEL và AI_ENABLED. Không có key hoặc AI lỗi thì chuyển Admin. Worker dùng lease để phục hồi sau restart; mỗi revision tạo một công việc. Mỗi lần gọi AI tối đa 25 giây; timeout có thể phát sinh lần gọi lại.

API sử dụng ảnh dạng data URL và đầu ra JSON Schema theo
[OpenAI Images and vision](https://developers.openai.com/api/docs/guides/images-vision) và
[Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs).
Chỉ gửi nội dung tin và ảnh, không gửi số điện thoại/email, số sê-ri hay hồ sơ xác minh.

Chạy `./mvnw.cmd --batch-mode --no-transfer-progress verify`; bộ kiểm tra API và tình huống dừng/khởi động Moderation nằm trong `scripts/Test-Microservices.ps1`. Cách cấu hình môi trường thử và khôi phục AI: [hướng dẫn microservices](../microservices/README.md). Frontend: `npm run build`, `npm test`.
