# Báo cáo 4 tính năng CameraHub

## Kết quả triển khai

1. **Sửa tin đã đăng:** dùng lại route `/api/marketplace/listings/{id}/draft` và form sửa tin. Backend kiểm tra tác giả (403 nếu khác tác giả, 404 nếu tin không tồn tại), dùng lại DTO/validation lúc tạo tin, lưu `updated_at`. Tin PUBLISHED chuyển về NEEDS_REVISION khi sửa nội dung hoặc ảnh; nút lưu dùng lại quy trình gửi AI duyệt. Giữ nguyên khóa sửa đối với tin đã đặt mua/đã bán/đang duyệt và các ràng buộc kiểm định hiện có.
2. **Sửa hồ sơ:** dùng lại `/api/auth/me` và ProfileEditor. Chỉ nhận tên và số điện thoại đã tồn tại; người dùng được xác định từ principal. Từ chối các trường ngoài danh sách, gồm id, email, username, role. Validate tên, số điện thoại và kiểm tra trùng số điện thoại theo cơ chế hiện có.
3. **Avatar:** POST multipart và GET ảnh tại `/api/auth/me/avatar`, đều yêu cầu đăng nhập. Client/server giới hạn 2 × 1024 × 1024 byte, chấp nhận JPG/JPEG, PNG, WebP; server giải mã nội dung, đối chiếu MIME/đuôi file, kiểm tra kích thước ảnh. Chỉ lưu UUID sinh ở server; tên gốc không dùng làm đường dẫn. Ảnh cũ được thay thế trong cùng giao dịch DB. Response trả URL UUID mới; React Query cập nhật session ngay, không cần reload.
4. **Đổi mật khẩu:** `/api/auth/me/password` nhận đủ mật khẩu hiện tại/mới/xác nhận. Kiểm tra mật khẩu hiện tại, xác nhận, khác mật khẩu cũ, tối thiểu 12 ký tự và tối đa 72 byte UTF-8 theo quy tắc hiện có. Dùng PasswordEncoder BCrypt hiện có (cost 12), không trả mật khẩu/hash; DTO che mật khẩu trong `toString()`.

## Schema và dependency

Migration [V6__account_avatar.sql](D:/CamerHub_Backend/src/main/resources/db/unified/V6__account_avatar.sql) chỉ thêm `avatar_key`, `avatar_content`, `avatar_type` nullable và CHECK dung lượng/loại ảnh vào `users`. Hệ thống chưa có nơi lưu avatar; ba trường này cần để lưu ảnh bền vững và thay ảnh cũ bằng giao dịch DB. Không sửa dữ liệu/cột cũ. Migration đã được Flyway áp dụng thành công trên database thử, Hibernate validate thành công.

Thêm duy nhất codec `com.twelvemonkeys.imageio:imageio-webp:3.12.0` để Java giải mã WebP thật; không nâng phiên bản dependency hiện có. Tham khảo [TwelveMonkeys](https://github.com/haraldk/TwelveMonkeys).

## Danh sách file

### Backend — D:\CamerHub_Backend

| File | Thay đổi và lý do |
|---|---|
| [pom.xml](D:/CamerHub_Backend/pom.xml) | Thêm codec giải mã WebP cho kiểm tra nội dung avatar. |
| [SecurityConfig.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/config/SecurityConfig.java) | Yêu cầu đăng nhập cho các route `/api/auth/me/**` mới. |
| [IdentityController.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/controller/IdentityController.java) | Endpoint đổi mật khẩu, upload và đọc avatar của chính mình. |
| [IdentityDtos.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/dto/IdentityDtos.java) | DTO đổi mật khẩu, validation và từ chối trường hồ sơ không được phép. |
| [ProfileResponse.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/dto/response/ProfileResponse.java) | Trả URL avatar để UI cập nhật ngay. |
| [Account.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/entity/Account.java) | Ánh xạ ba cột avatar và URL UUID; ẩn nội dung ảnh khỏi JSON. |
| [AccountService.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/service/AccountService.java) | Validate tên hồ sơ; kiểm tra và hash mật khẩu mới bằng encoder hiện có. |
| [AvatarService.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/identity/service/AvatarService.java) — mới | Validate và thay avatar bằng giao dịch, UUID an toàn. |
| [V6__account_avatar.sql](D:/CamerHub_Backend/src/main/resources/db/unified/V6__account_avatar.sql) — mới | Migration tối thiểu để lưu avatar. |
| [ProductRepositoryOperationsImpl.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/marketplace/repository/ProductRepositoryOperationsImpl.java) | Cho đọc/khóa sửa tin PUBLISHED bằng luồng sửa sẵn có; dùng lại SQL lưu updated_at. |
| [MarketplaceService.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/marketplace/service/MarketplaceService.java) | Kiểm tra tác giả, trả 403/404 rõ ràng, chuyển tin đã sửa về chờ gửi duyệt. |
| [ProductMediaRepositoryOperations.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/marketplace/repository/ProductMediaRepositoryOperations.java) | Khai báo thao tác chuyển trạng thái khi sửa ảnh tin đã đăng. |
| [ProductMediaRepositoryOperationsImpl.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/marketplace/repository/ProductMediaRepositoryOperationsImpl.java) | Cho tác giả sửa ảnh PUBLISHED; cập nhật trạng thái, updated_at và version. |
| [ProductMediaService.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/marketplace/service/ProductMediaService.java) | Chuyển tin về NEEDS_REVISION trong giao dịch upload/xóa ảnh thành công. |
| [GlobalExceptionHandler.java](D:/CamerHub_Backend/src/main/java/vn/camerahub/shared/exception/GlobalExceptionHandler.java) | Trả 400 khi avatar vượt cả giới hạn multipart hoặc thiếu file; giữ giới hạn ảnh sản phẩm hiện có. |
| [AccountEditingTest.java](D:/CamerHub_Backend/src/test/java/vn/camerahub/identity/AccountEditingTest.java) — mới | Test service hồ sơ và mật khẩu, dữ liệu sai/khác mật khẩu cũ/hash/updated_at. |
| [AccountFeaturesHttpTest.java](D:/CamerHub_Backend/src/test/java/vn/camerahub/identity/AccountFeaturesHttpTest.java) — mới | MockMvc dùng SecurityConfig thật, controller/service thật và repository mock: thành công, 401/403/400/404, JPG/PNG/WebP, file giả/quá lớn, trường hồ sơ cấm. |
| [ListingEditingTest.java](D:/CamerHub_Backend/src/test/java/vn/camerahub/marketplace/ListingEditingTest.java) — mới | Test sửa nội dung tin PUBLISHED, tác giả khác, tin mất, giá sai. |
| [ListingMediaEditingTest.java](D:/CamerHub_Backend/src/test/java/vn/camerahub/marketplace/ListingMediaEditingTest.java) — mới | Test sửa ảnh và chuyển trạng thái, chặn người khác sửa ảnh. |
| [Test-UnifiedBackend.ps1](D:/CamerHub_Backend/scripts/Test-UnifiedBackend.ps1) | Chấp nhận thêm 403 cho ca đọc tin của người khác, phù hợp quyền tác giả mới. |
| [FOUR_FEATURES_REPORT.md](D:/CamerHub_Backend/docs/FOUR_FEATURES_REPORT.md) — mới | Báo cáo thay đổi, kiểm tra và giả định. |

### Frontend — D:\CameraHub_Frontend

| File | Thay đổi và lý do |
|---|---|
| [MyListings.tsx](D:/CameraHub_Frontend/src/components/MyListings.tsx) | Hiện nút sửa bằng form hiện có cho tin PUBLISHED. |
| [ProfileContent.tsx](D:/CameraHub_Frontend/src/components/ProfileContent.tsx) | Nối form avatar/mật khẩu vào cài đặt, hiển thị avatar session mới. |
| [AccountSecurityEditor.tsx](D:/CameraHub_Frontend/src/components/AccountSecurityEditor.tsx) — mới | Form avatar và mật khẩu dùng component/class UI sẵn có, thông báo lỗi và cập nhật cache. |
| [identity.ts](D:/CameraHub_Frontend/src/lib/identity.ts) | API upload avatar/đổi mật khẩu và validation client, dùng CSRF hiện có. |
| [accountEditing.test.ts](D:/CameraHub_Frontend/src/lib/accountEditing.test.ts) — mới | Test định dạng/dung lượng file, multipart/CSRF, phản hồi quyền và validation mật khẩu/hồ sơ. |

Không sửa CSS, không đổi tên file/hàm/biến hiện có, không refactor ngoài bốn tính năng.

## Lệnh và output kiểm tra

Frontend, thư mục `D:\CameraHub_Frontend`:

```text
npm test
Test Files  14 passed (14)
     Tests  67 passed (67)

npm run build
> tsc -b && vite build
✓ 127 modules transformed.
✓ built in 2.79s
```

Backend, thư mục `D:\CamerHub_Backend`:

```text
.\mvnw.cmd -f feature-verification.pom.xml --batch-mode --no-transfer-progress verify
Tests run: 42, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

`feature-verification.pom.xml` là bản sao tạm của pom.xml, chỉ thêm finalName=camerahub-feature-verification. Lần chạy `mvnw.cmd --batch-mode --no-transfer-progress verify` đầu tiên đã qua toàn bộ test nhưng bước repackage bị khóa file JAR bởi backend đang chạy; dùng tên riêng giúp kiểm tra đóng gói mà không dừng backend đó. Bản POM tạm được xóa sau khi kiểm tra. [Log Maven đầy đủ](D:/CamerHub_Backend/.local/all-feature-tests.log).

Lần chạy frontend trong sandbox gặp EPERM khi Vite đọc realpath; các lệnh test/build phía trên đã chạy thành công ngoài sandbox. Test Java lần đầu trong sandbox dừng ở Mockito agent; đã dừng lần chạy đó và chạy lại ngoài sandbox. Cấu hình MockMvc ban đầu tự nạp JPA ngoài ý muốn đã được tách cấu hình test và chạy lại thành công.

Smoke test DB/API trên server tạm `127.0.0.1:18080`, dùng database `camerahub_unified_verify_*` đã có trong cấu hình thử:

```text
.\scripts\Test-Authentication.ps1 -BaseUrl http://127.0.0.1:18080
14 authentication checks passed

.\scripts\Test-UnifiedBackend.ps1 -BaseUrl http://127.0.0.1:18080
17 checks passed

Flyway:
Migrating schema "camerahub" to version "6 - account avatar"
Successfully applied 1 migration to schema "camerahub", now at version v6
```

Hai script được gọi qua runner tạm `.local/Run-Feature-Smoke.ps1`, kiểm tra tên database thử trước khi mở server, dùng JAR tên riêng và dừng server trong `finally`. Script `Test-Microservices.ps1` chỉ là alias gọi `Test-UnifiedBackend.ps1`, nên không chạy lại cùng bộ kiểm tra. [Log smoke test đầy đủ](D:/CamerHub_Backend/.local/feature-smoke.log), [log startup/migration](D:/CamerHub_Backend/.local/feature-server.log). Server thử đã dừng sau khi hoàn tất; không dừng backend đang chạy của người dùng.

`git diff --check` thành công ở cả hai repository. Tổng cộng: **42 test Java + 67 test frontend + 31 kiểm tra smoke API đều qua**; production build frontend và đóng gói backend bằng tên JAR riêng thành công.

## Giả định và giới hạn

- Hồ sơ hiện tại có tên và số điện thoại; không có bio, vì vậy không thêm bio hoặc trường hồ sơ mới.
- Quy tắc mật khẩu hiện có là độ dài/UTF-8 nêu trên; không tự thêm yêu cầu ký tự hoa, số hoặc ký tự đặc biệt.
- Sửa tin đang bán cần duyệt lại theo quy trình hiện có. Tin đang giao dịch/đang duyệt vẫn không được sửa.
- Avatar phục vụ cho chính người đang đăng nhập qua API riêng tư, không mở endpoint ảnh công khai mới.
- Repository được mock trong test HTTP/service; smoke test riêng kiểm tra startup, migration và các luồng API với PostgreSQL thật trên database thử.
- Chưa triển khai production, chưa thay migration trên database chính, chưa restart backend đang chạy của người dùng.
