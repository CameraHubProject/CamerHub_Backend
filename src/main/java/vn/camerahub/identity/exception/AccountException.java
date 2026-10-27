package vn.camerahub.identity.exception;

import vn.camerahub.shared.exception.AppException;

import java.util.Map;

public final class AccountException extends AppException {
    public AccountException(IdentityErrorCode code) {
        super(code, fields(code));
    }

    public AccountException(IdentityErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }

    private static Map<String, String> fields(IdentityErrorCode code) {
        String field = switch (code) {
            case INVALID_PHONE, PHONE_ALREADY_USED -> "phone";
            case EMAIL_ALREADY_USED -> "email";
            case PASSWORD_TOO_LONG -> "password";
            case PASSWORD_CONFIRM_MISMATCH -> "confirmPassword";
            default -> null;
        };
        return field == null ? Map.of() : Map.of(field, code.getMessage());
    }
}
