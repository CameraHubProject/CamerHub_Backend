# Triển khai CameraHub lên Render

## Trạng thái

`render.yaml` là cấu hình triển khai **có tính phí**. Tạo file hoặc đăng nhập Render chưa tạo website công khai. Chỉ có link sử dụng sau khi đồng bộ Blueprint, các service deploy thành công và kiểm tra HTTPS.

Mã nguồn backend và frontend phải có trên nhánh `codex/render-deploy` trong hai repository CameraHubProject trước khi Render đọc Blueprint. File môi trường, khóa JWT, ảnh và database trên máy không được đẩy lên Git.

## Cấu hình

```text
Người dùng → HTTPS camerahub-web.onrender.com (React + Nginx)
                  └─ /api/* → gateway (private)
                                ├─ identity → identity-db
                                ├─ marketplace → marketplace-db + persistent media disk
                                ├─ moderation → moderation-db
                                ├─ commerce → commerce-db
                                └─ advisor → advisor-db
```

Địa chỉ thực tế do Render cấp; không mặc định rằng hostname ví dụ trên còn trống. Cả 7 ứng dụng và 5 PostgreSQL nằm trong Singapore. Chỉ frontend có endpoint Internet. Nginx chuyển `/api` sang gateway qua mạng riêng; frontend tiếp tục dùng API tương đối, cookie và CSRF cùng origin. Đường dẫn React như `/tai-khoan` vẫn hoạt động khi tải lại trang.

Dockerfile build từ source bằng Java 21/Maven và Node 22, không phụ thuộc `target`/`dist` trên máy. Backend chạy bằng UID 10001. JWT private key chỉ được cấp cho Identity; public key được chia sẻ cho các service xác minh token. Cookie production bật `Secure` và `HttpOnly`.

## Dự toán trước khi tạo tài nguyên

| Tài nguyên | Số lượng | Giá dự kiến USD/tháng |
| --- | ---: | ---: |
| App 0.5 CPU / 512 MB | 7 | 49.00 |
| PostgreSQL 0.1 CPU / 256 MB | 5 | 30.00 |
| Storage PostgreSQL, mỗi DB 1 GB | 5 GB | 1.50 |
| Disk ảnh Marketplace | 1 GB | 0.25 |
| Tổng | | **80.75** |

Giá là ước tính, kiểm tra tổng Render hiển thị ngay trước khi tạo. Chưa gồm thuế, OpenAI, tên miền và mức sử dụng vượt hạn mức (băng thông/build). 512 MB/app và 256 MB/database là cấu hình ban đầu, cần theo dõi bộ nhớ và tải thực tế. Không bật tự tăng dung lượng. `autoDeployTrigger: off` yêu cầu deploy thủ công cho lần cập nhật tiếp theo.

Nguồn: [Render pricing](https://render.com/pricing), [chi phí app 512 MB](https://render.com/articles/render-vs-netlify), [giá storage](https://render.com/articles/how-much-does-cloud-application-hosting-cost-for-small-businesses). Nếu chỉ demo ngắn hạn, nên thống nhất một cấu hình tiết kiệm riêng trước; file này giữ các microservice chạy độc lập.

## Chuẩn bị khóa và tài khoản

Tại thư mục backend, dùng Windows PowerShell (không cần cài lệnh `pwsh`):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Prepare-RenderSecrets.ps1
```

Java 21 cần có trong PATH hoặc `JAVA_HOME`. Script tạo cặp RSA mới riêng cho Render, giữ lại khi chạy lần sau, và ghi vào `.local/render/keys/` đã được Git bỏ qua:

- `JWT_PUBLIC_KEY_B64.txt`: điền `JWT_PUBLIC_KEY_B64` của Identity.
- `JWT_PRIVATE_KEY_B64.txt`: điền `JWT_PRIVATE_KEY_B64` của Identity.
- `identity-jwt.env`: hai biến trên, thuận tiện nhập vào Render nếu cần.

Không đưa các giá trị này vào chat, repository hoặc frontend. Cất giữ private key an toàn; thay khóa sẽ làm token cũ mất hiệu lực.

## Tạo Blueprint

1. Đăng nhập [Render Dashboard](https://dashboard.render.com/).
2. Kết nối GitHub và chỉ cấp quyền cho hai repository backend/frontend nếu repository là private. Người sở hữu tự chấp thuận quyền truy cập và thanh toán.
3. Chọn **New → Blueprint**, repository `CameraHubProject/CamerHub_Backend`, branch `codex/render-deploy`, path `render.yaml`.
4. Điền hai biến JWT vào Identity. Điền `OPENAI_API_KEY` vào Moderation; Advisor tham chiếu cùng key. Chỉ đưa key dành cho triển khai này vào Render; không đưa key vào Docker build args hoặc Git. Nếu để trống, AI không gọi được mô hình và bài cần kiểm tra sẽ chuyển Admin theo cơ chế hiện có.
5. Kiểm tra 7 app, 5 database, region, dung lượng và tổng phí trước khi chọn tạo/deploy.
6. Chờ cả hai Docker build và Flyway trên 5 database hoàn tất. Các service không phụ thuộc thứ tự khởi động; outbox tự retry. Moderation tự xây chỉ mục chống trùng sau khi Marketplace sẵn sàng.
7. Mở URL của `camerahub-web`, kiểm tra `/api/health`, đăng ký/đăng nhập, đổi hồ sơ, tạo nháp và tải ảnh. Kiểm tra cookie Secure/HttpOnly trên HTTPS và tải lại một đường dẫn con.

Database mới khởi tạo rỗng, có danh mục 34 hãng cùng lựa chọn “Khác”. Không tự đưa hồ sơ, tài khoản, giao dịch hoặc ảnh cũ từ máy lên Internet. Khi cần chuyển dữ liệu cũ, lên kế hoạch import và đối soát riêng; tắt `CAMERAHUB_MODERATION_BOOTSTRAP_ON_START` cho tới khi import hoàn tất rồi bootstrap chỉ mục. Không chạy script import local với database Render nếu chưa kiểm tra đích.

## Bật Google và cấp Admin

Google mặc định tắt để đăng ký mật khẩu hoạt động mà không cần cấu hình OAuth. Khi bật:

- Thiết lập Google OAuth web client với redirect `https://<frontend-thực-tế>/api/auth/google/callback`.
- Thêm `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_LOGIN_ENABLED=true` vào Identity rồi redeploy. `FRONTEND_URL` tự tham chiếu public URL của frontend.
- Nếu gắn tên miền riêng, thay `FRONTEND_URL` bằng origin HTTPS đó và cập nhật Google redirect tương ứng.

Không tạo sẵn Admin hoặc mật khẩu chung. Người vận hành đăng ký tài khoản của mình, xác minh đúng người sở hữu rồi cấp vai trò qua database Identity bằng công cụ quản trị có quyền. Không mở database ra toàn Internet: `ipAllowList: []` chặn kết nối public.

## Vận hành

- Xem log riêng cho từng service; health app ở `/actuator/health` trên mạng private. Frontend `/healthz` kiểm tra Nginx, `/api/health` kiểm tra gateway; các đường này không thay thế kiểm thử một luồng nghiệp vụ qua các service.
- Ảnh nằm trong disk Marketplace `/var/data/media`, không nằm trong image. Không xóa/recreate disk khi cập nhật code. Disk là một bản lưu trữ; cần backup ảnh và database cho vận hành thực tế. Có disk nên Marketplace chỉ chạy một instance với cấu hình này.
- Không sao chép `.env` trên máy vào cloud. Chỉ nhập các secret cần thiết vào Render environment. Lỗi startup vì thiếu biến/khóa phải sửa cấu hình, không tắt xác thực.
- Khi cập nhật: kiểm tra test, push source, deploy service thay đổi; giữ nguyên JWT và disk. Không xóa database để sửa lỗi Flyway.
- Muốn chuyển VPS sau: giữ nguyên module Spring Boot và API; đổi cấu hình host/database/media rồi dùng Docker Compose. Cấu hình chạy local hiện tại không đổi.

Tài liệu Render: [Blueprint specification](https://render.com/docs/blueprint-spec), [Docker](https://render.com/docs/docker), [private network](https://render.com/docs/private-network).

## Kiểm chứng trước khi tạo tài nguyên

Đã kiểm tra Blueprint với JSON schema chính thức của Render, build đủ 6 image Java từ source bằng Maven và build frontend bằng Node 22 trong Docker. Bộ kiểm thử Java: 26 test đạt. Bộ Docker thử nghiệm riêng với 5 PostgreSQL rỗng: 16 kiểm tra HTTP đạt, gồm migration, danh mục, JWT/cookie Secure và HttpOnly, lỗi nghiệp vụ, đổi hồ sơ, xác thực qua Commerce, tạo tin, ghi ảnh bằng user không phải root, tự bootstrap chống trùng và chuyển Admin khi AI tắt. Kiểm thử HTTP cục bộ mô phỏng trình duyệt bằng cách gửi cookie thủ công; cần kiểm tra lại trên HTTPS thực của Render sau khi deploy. Các kết quả trên chưa xác nhận website đã online.
