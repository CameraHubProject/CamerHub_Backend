package vn.camerahub.shared.exception;

import org.springframework.http.HttpStatus;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường được đánh dấu."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Dữ liệu gửi lên không đúng định dạng."),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "Vui lòng đăng nhập để tiếp tục."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Số điện thoại/email hoặc mật khẩu không đúng, hoặc tài khoản không hoạt động."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này."),
    CSRF_INVALID(HttpStatus.FORBIDDEN, "Phiên bảo mật không hợp lệ. Vui lòng tải lại trang rồi thử lại."),
    INTERNAL_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Không được truy cập API nội bộ."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên yêu cầu."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức HTTP không được hỗ trợ."),
    DATA_CONFLICT(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc vi phạm điều kiện."),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Dữ liệu vừa thay đổi hoặc đang được xử lý. Vui lòng tải lại."),
    UPLOAD_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh tối đa 5 MB."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Định dạng dữ liệu không được hỗ trợ."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Bạn đang gửi quá nhiều yêu cầu. Vui lòng thử lại sau."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Hệ thống chưa xử lý được yêu cầu. Vui lòng thử lại sau."),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "Dịch vụ chưa hoàn tất yêu cầu. Vui lòng thử lại."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Một dịch vụ đang chưa sẵn sàng. Vui lòng thử lại sau."),
    UPSTREAM_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "Dịch vụ phản hồi chậm. Vui lòng thử lại sau.");
    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return name();
    }

    public static CommonErrorCode forStatus(int status) {
        return switch (status) {
            case 400 -> INVALID_REQUEST;
            case 401 -> AUTHENTICATION_REQUIRED;
            case 403 -> ACCESS_DENIED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 405 -> METHOD_NOT_ALLOWED;
            case 409 -> DATA_CONFLICT;
            case 413 -> UPLOAD_TOO_LARGE;
            case 415 -> UNSUPPORTED_MEDIA_TYPE;
            case 429 -> RATE_LIMITED;
            case 502 -> UPSTREAM_ERROR;
            case 503 -> SERVICE_UNAVAILABLE;
            case 504 -> UPSTREAM_TIMEOUT;
            default -> INTERNAL_ERROR;
        };
    }
}
