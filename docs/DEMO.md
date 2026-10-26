# Dữ liệu demo CameraHub

> Tài liệu tham khảo bản monolith trước khi tách. Mã nguồn và công cụ cũ đã được nén vào `.local/backups/legacy-before-cleanup-20261026.zip`. Lệnh chạy và kết quả kiểm thử hiện tại: [microservices](../microservices/README.md). Xác minh người bán đã được bỏ khỏi ứng dụng hiện tại.

Bộ dữ liệu dành riêng cho database `camerahub_mvp_dev`. Tất cả tài khoản, hồ sơ, giá, số lần chụp và tình trạng thiết bị bên dưới đều giả lập. Không dùng tài liệu xác minh danh tính thật.

## Tài khoản

Mật khẩu ban đầu dùng chung: `CameraHubDemo!2026`.

| Email | Vai trò và tình huống |
| --- | --- |
| admin@demo.camerahub.test | Admin xét duyệt hồ sơ, đồng thời có quyền người mua |
| buyer@demo.camerahub.test | Người mua |
| seller@demo.camerahub.test | Người bán 01, đã xác minh mô phỏng, đồng thời có quyền người mua |
| seller2@demo.camerahub.test | Người bán 02, đã xác minh mô phỏng, đồng thời có quyền người mua |
| pending@demo.camerahub.test | Hồ sơ đang chờ Admin duyệt |
| revision@demo.camerahub.test | Hồ sơ cần bổ sung, có thể gửi lại file |
| inspector@demo.camerahub.test | Kiểm định viên, đồng thời có quyền người mua; chưa có giao diện nghiệp vụ kiểm định |

Đăng nhập tại `http://127.0.0.1:5173/dang-nhap`. Xem tài khoản tại `/tai-khoan`; Admin xét duyệt tại `/quan-tri/xac-minh`.

## Tin đăng

8 tin đăng có tiền tố `[DEMO]`, trạng thái `PUBLISHED`, thuộc hai Seller trên. Tất cả để `NOT_INSPECTED`; không tạo chứng nhận kiểm định giả. Giao diện hiện dùng khung giữ chỗ ảnh thiết bị.

| Sản phẩm | Giá giả lập |
| --- | ---: |
| Canon EOS R6 body | 28.500.000 ₫ |
| Sony Alpha a6400 body | 14.900.000 ₫ |
| Fujifilm X-T30 II | 19.500.000 ₫ |
| Nikon Z6 II body | 24.800.000 ₫ |
| Canon RF 50mm F1.8 STM | 3.900.000 ₫ |
| Sony FE 24–70mm F2.8 GM | 22.500.000 ₫ |
| Sigma 18–50mm F2.8 DC DN | 9.200.000 ₫ |
| Godox V1-C | 3.200.000 ₫ |

Demo tại trang chủ: mở danh sách thiết bị, lọc hãng/loại/ngân sách, xem thông tin sản phẩm. Đăng nhập Admin để duyệt hồ sơ pending; đăng nhập revision để gửi lại hồ sơ. Các ảnh hồ sơ riêng tư được tạo bằng dữ liệu giả và ghi rõ DEMO ONLY.

## Chạy lại

```powershell
Set-Location D:\CamerHub_Backend
.\scripts\Seed-Demo.ps1
```

Script chỉ chạy thủ công, không gắn vào Flyway hay khởi động ứng dụng. Nó dùng cấu hình `.env` có sẵn, chặn database khác `camerahub_mvp_dev`, tạo dữ liệu trong một transaction và khóa để tránh chạy đồng thời. UUID cùng audit marker nhận diện dữ liệu demo. Chạy lại không nhân bản, không đặt lại mật khẩu, không cấp lại quyền đã bị thu hồi và không đổi trạng thái hồ sơ hoặc tin đăng đã chỉnh sửa. Khi trùng với một tài khoản có sẵn không do script tạo, toàn bộ transaction bị hủy. Dữ liệu ứng dụng hiện có được giữ nguyên; không có thao tác xóa/reset schema.

Script dùng Java 21 và đúng ba thư viện JDBC/BCrypt/logging từ bản backend đã build trong `target`. Các file hồ sơ mẫu được lưu trong `storage/private`, phù hợp adapter dev mặc định; nếu đổi thư mục private của backend, cần điều chỉnh thư mục tương ứng trước khi seed. Nếu transaction thất bại, các file vừa tạo được dọn lại.

F01 đang hoạt động; demo này chỉ thêm dữ liệu mẫu để xem tin đăng và thử tài khoản. Chức năng tạo tin qua giao diện, chat, đặt cọc và các luồng mua bán tiếp theo vẫn chờ triển khai.
