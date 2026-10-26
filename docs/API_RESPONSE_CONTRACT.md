# Phản hồi và xử lý lỗi CameraHub

Backend quyết định dữ liệu nghiệp vụ, quyền thao tác, trạng thái, số tiền cuối cùng,
mã lỗi và thông báo. Frontend hiển thị dữ liệu, message và lỗi từng trường; quản lý
trạng thái tải, form, cache, ảnh xem trước và lỗi kết nối khi không có phản hồi backend.
Kiểm tra nhanh tệp ảnh ở trình duyệt giúp tránh tải tệp sai; backend vẫn kiểm tra lại
toàn bộ ảnh và giới hạn. Các nhãn giao diện, định dạng tiền/ngày và giỏ hàng local
vẫn thuộc phần trình bày. Kiểm tra hàng tồn và giá khi checkout thuộc backend.

## Định dạng

```json
{
  "code": 409,
  "errorCode": "IDENTITY_PHONE_ALREADY_USED",
  "message": "Số điện thoại đã được sử dụng.",
  "data": null,
  "errors": { "phone": "Số điện thoại đã được sử dụng." },
  "timestamp": "2026-10-26T00:00:00Z"
}
```

`code` tiếp tục bằng HTTP status để giữ tương thích. `errorCode` là mã nghiệp vụ ổn định,
khác nhau dù cùng HTTP 409. Thành công có `errorCode: null`, `data` chứa DTO hoặc null.
Frontend dùng message backend; không suy thông báo nghiệp vụ từ HTTP status hay tự
dịch mã lỗi. API ảnh nhị phân và health không bị bọc trong JSON thành công.

## Sở hữu lỗi

| Phạm vi | Bộ mã | Exception |
| --- | --- | --- |
| Lỗi chung, validation, xác thực, hạ tầng | `CommonErrorCode` | `AppException`, `ValidationException`, `ResourceNotFoundException`, `RemoteServiceException` |
| Tài khoản và đăng nhập | `IdentityErrorCode` | `AccountException` |
| Tin đăng, ảnh, hãng máy, giữ chỗ | `MarketplaceErrorCode` | `ListingException`, `MediaException`, `BrandException`, `ReservationException` |
| Đơn hàng, checkout, đánh giá | `CommerceErrorCode` | `OrderException`, `CheckoutException`, `ReviewException` |
| Duyệt tin và trùng lặp | `ModerationErrorCode` | `ModerationException` |
| Tư vấn và hạn mức AI | `AdvisorErrorCode` | `AdvisorException` |

Mỗi bộ mã và exception nghiệp vụ nằm trong package `exception/` của service sở hữu.
`service-support` chỉ chứa interface `ErrorCode`, exception nền và cách serialize chung.
Không import enum nghiệp vụ từ service khác. `InternalClient` nhận lỗi HTTP và tạo
`RemoteServiceException`, giữ nguyên mã, message và lỗi từng trường khi đi qua service.
Gateway chuyển tiếp nguyên body; lỗi do Gateway tạo có cùng định dạng và mã lỗi chung.

```java
if (repository.findByLoginPhone(canonical).isPresent()) {
    throw new AccountException(IdentityErrorCode.PHONE_ALREADY_USED);
}
```

`IdentityErrorCode` định nghĩa HTTP status và message ở một chỗ. `AccountException`
gắn `errors.phone` cho lỗi điện thoại. `GlobalExceptionHandler` xử lý exception và DTO
validation; `ApiExceptionWriter` dùng cùng hợp đồng trong security filter.
Lỗi lập trình, SQL hoặc nhà cung cấp ngoài không được trả chi tiết nhạy cảm cho client.

Đăng ký nhận thêm `confirmPassword`; service so khớp khi client gửi trường này.
Client cũ và luồng Google vẫn dùng hợp đồng trước đây. Giới hạn mật khẩu 72 byte UTF-8
được kiểm tra trong service. Email/số điện thoại trùng có mã lỗi riêng, kể cả trường
hợp hai request đồng thời vi phạm unique index trong database.

## Thông báo thành công

Controller có thể trả `ApiResponse.success(...)` trực tiếp hoặc khai báo:

```java
@PostMapping("/{id}/submit")
@SuccessMessage("Đã gửi tin. AI đang kiểm tra nội dung và ảnh.")
public Object submit(...) {
    return service.submit(...);
}
```

`ApiResponseAdvice` gắn message và data một lần. Các thao tác xóa/hủy trả
`ApiResponse<Void>` với HTTP 200 và `data: null` để client đọc được thông báo.
Chưa thay toàn bộ public projection Map thành response DTO trong lần sửa này.

Frontend dùng `checkedResponse` để tạo `ApiError(status, errorCode, message, errors)`.
`request<T>` trả data cho phần hiển thị; `requestResult<T>` giữ cả message cho thông báo
sau thao tác. Trang đăng ký và hồ sơ gắn lỗi backend vào input; nơi khác hiển thị summary
và nội dung lỗi từng trường qua `errorMessage`. Thông báo Google cũng lấy từ endpoint
status; backend chỉ chấp nhận reason đã biết, không phản chiếu chuỗi tùy ý từ URL.

## Nguyên tắc tách

Microservice là ranh giới nghiệp vụ có ứng dụng và dữ liệu riêng. Controller, service,
repository, entity, mapper và exception là các lớp bên trong ranh giới đó. Tách theo
trách nhiệm giúp sửa độc lập và tìm lỗi nhanh. Chỉ tạo thêm service khi nghiệp vụ,
quyền sở hữu dữ liệu hoặc nhu cầu vận hành cần một ranh giới độc lập.
