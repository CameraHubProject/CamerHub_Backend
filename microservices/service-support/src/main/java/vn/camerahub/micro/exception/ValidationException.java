package vn.camerahub.micro.exception;

import java.util.Map;

public final class ValidationException extends AppException {
    public ValidationException(Map<String, String> errors) {
        super(CommonErrorCode.VALIDATION_FAILED, errors);
    }
}
