# Cấu trúc Spring Boot backend

Mỗi service là ứng dụng Spring Boot độc lập, có database và migration riêng.
Các package nghiệp vụ không import entity/repository từ service khác.

```text
identity-service/src/main/java/vn/camerahub/identity/
  controller/IdentityController.java
  service/AccountService.java
  repository/AccountRepository.java
  entity/Account.java
  mapper/ProfileMapper.java
  exception/IdentityErrorCode.java
  exception/AccountException.java
  dto/IdentityDtos.java
  dto/response/ProfileResponse.java
  config/SecurityConfig.java
  config/JwtIssuerConfig.java

service-support/src/main/java/vn/camerahub/micro/
  config/AppConfig.java
  config/SecurityConfig.java
  config/JwtAuthenticationFilter.java
  config/JwtUtil.java
  config/OpenApiConfig.java
  dto/ApiResponse.java
  dto/ApiResponseAdvice.java
  exception/ErrorCode.java
  exception/CommonErrorCode.java
  exception/AppException.java
  exception/ValidationException.java
  exception/RemoteServiceException.java
  exception/ResourceNotFoundException.java
  exception/GlobalExceptionHandler.java
  exception/ApiExceptionWriter.java
  persistence/JpaQueries.java
```

Marketplace, Moderation, Commerce và Advisor cũng có controller, service, repository,
entity, mapper và DTO riêng. Đã ánh xạ 29 entity thuộc các bảng nghiệp vụ đang triển khai;
những nghiệp vụ cũ chưa triển khai vẫn nằm trong database gốc.

## Dependency và code gọn

`microservices/pom.xml` khai báo Lombok, MapStruct và annotation processor dùng chung.
`service-support/pom.xml` khai báo Web, JPA, Security, Validation, Actuator, Flyway,
PostgreSQL, OAuth2 Resource Server (Nimbus JWT) và Springdoc OpenAPI. Service nghiệp vụ
phụ thuộc module này, chỉ khai báo thêm thư viện đặc thù như Google OAuth2 Client.
Gateway chỉ phụ thuộc Web/Actuator, không kéo theo JPA hoặc database.

Entity dùng `@Getter`, `@Setter`, `@NoArgsConstructor`; không dùng `@Data` trên entity
có quan hệ lazy để tránh equals/toString duyệt quan hệ. Một số accessor nghiệp vụ
được giữ để phục vụ kiểm tra trạng thái và tương thích truy vấn hiện có.
Controller/service dùng `@RequiredArgsConstructor` với dependency `private final`.
DTO dạng record đã có constructor và accessor nên không cần thêm Lombok.

```java
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface ProfileMapper {
    @Mapping(target = "status", source = "accountStatus")
    ProfileResponse toResponse(Account user);
}
```

Maven sinh implementation trong `target/generated-sources/annotations`; không sửa
hoặc đưa file sinh tự động lên Git. Bật Annotation Processing trong IntelliJ nếu IDE
chưa nhận getter/setter hay mapper. Maven CLI vẫn là cách kiểm tra biên dịch chuẩn.

Không thêm MySQL/H2 vì database hiện tại là PostgreSQL với JSONB, khóa và upsert đặc thù.
JWT dùng Nimbus đã tích hợp Spring Security; không cần thêm JJWT song song.

## Validation và lỗi từ backend

Controller nhận `@Valid @RequestBody`. DTO định nghĩa `@NotBlank`, `@Email`, `@Size`,
`@Pattern`, `@Min`...; DTO lồng nhau có `@Valid`. Thông báo mặc định tiếng Việt nằm ở
`service-support/src/main/resources/ValidationMessages.properties`. Service kiểm tra
điều kiện nghiệp vụ như quyền sở hữu, trạng thái, số điện thoại và chống trùng.

`GlobalExceptionHandler` trả mã HTTP đúng cùng một cấu trúc JSON; filter xác thực
dùng `ApiExceptionWriter` để trả cùng định dạng. Lỗi ngoài dự kiến được ẩn chi tiết
nội bộ. Frontend đã đọc `data` khi thành công và giữ `message`/`errors` khi thất bại.

```json
{
  "code": 400,
  "errorCode": "VALIDATION_FAILED",
  "message": "Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường được đánh dấu.",
  "data": null,
  "errors": { "email": "Địa chỉ email không hợp lệ." },
  "timestamp": "2026-10-26T00:00:00Z"
}
```

API JSON thành công được bọc trong `ApiResponse<T>` với `errorCode: null`. Thông báo
thao tác nằm ở controller (`ApiResponse.success` hoặc `@SuccessMessage`) và được frontend
hiển thị trực tiếp. Các thao tác xóa/hủy cũng trả JSON thành công với `data: null`.
Health, ảnh nhị phân và API nội bộ giữ định dạng dữ liệu tương ứng. Swagger của Identity mở tại
http://127.0.0.1:8081/swagger-ui.html; các service nghiệp vụ dùng cổng 8082–8085.

Chi tiết mã lỗi, exception theo nghiệp vụ và trách nhiệm FE/BE:
[Hợp đồng phản hồi API](API_RESPONSE_CONTRACT.md).

## Xác thực

`POST /api/auth/login` nhận form email (hoặc số điện thoại) và password, trả JWT cùng
cookie HttpOnly. Access token hết hạn sau 15 phút. `POST /api/auth/refresh` đổi refresh
token để cấp JWT mới; `POST /api/auth/logout` thu hồi phiên. Client trình duyệt phải
lấy CSRF từ `GET /api/auth/csrf` trước các thao tác thay đổi dữ liệu. API client có thể
gửi `Authorization: Bearer ...`. Frontend hiện yêu cầu đăng nhập lại khi access token
hết hạn; endpoint refresh sẵn sàng cho client cần tự động gia hạn.

Khóa RSA nằm trong `.local/jwt/`, không đưa lên Git. Mỗi service kiểm tra chữ ký JWT
và tình trạng thu hồi/quyền tài khoản hiện tại. Thay đổi schema qua Flyway; Hibernate
chỉ kiểm tra schema, không tự sửa hoặc xóa dữ liệu.

Chạy: `backend.cmd Build`, rồi `backend.cmd Start`. Kiểm tra: `backend.cmd Status`.
