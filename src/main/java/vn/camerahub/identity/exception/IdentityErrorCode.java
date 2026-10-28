package vn.camerahub.identity.exception;

import vn.camerahub.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum IdentityErrorCode implements ErrorCode {
    GOOGLE_LINK_EXPIRED(HttpStatus.BAD_REQUEST, "Phiên liên kết Google đã hết hạn. Vui lòng bắt đầu lại."),
    INVALID_LINK_PASSWORD(HttpStatus.UNAUTHORIZED, "Mật khẩu không đúng hoặc tài khoản không hoạt động."),
    JWT_INVALID(HttpStatus.UNAUTHORIZED, "JWT không hợp lệ."),
    PASSWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8."),
    INVALID_PHONE(HttpStatus.BAD_REQUEST, "Số điện thoại di động Việt Nam chưa đúng."),
    ACCOUNT_INACTIVE(HttpStatus.CONFLICT, "Tài khoản không còn hoạt động."),
    SESSION_REVOKED(HttpStatus.UNAUTHORIZED, "Phiên đăng nhập đã hết hạn hoặc bị thu hồi. Vui lòng đăng nhập lại."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tài khoản."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy thành viên."),
    EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "Email đã được sử dụng."),
    PHONE_ALREADY_USED(HttpStatus.CONFLICT, "Số điện thoại đã được sử dụng."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "Mật khẩu xác nhận chưa khớp.");
    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return "IDENTITY_" + name();
    }
}
