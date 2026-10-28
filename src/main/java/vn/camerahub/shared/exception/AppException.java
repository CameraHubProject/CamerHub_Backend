package vn.camerahub.shared.exception;

import java.util.Map;

/**
 * All public business failures carry a stable machine code and a backend-owned message.
 */
@lombok.Getter
public class AppException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, String> errors;

    public AppException(ErrorCode code) {
        this(code, Map.of());
    }

    public AppException(ErrorCode code, Map<String, String> errors) {
        super(code.getMessage());
        this.errorCode = code;
        this.errors = Map.copyOf(errors);
    }

    public org.springframework.http.HttpStatus getStatusCode() {
        return errorCode.getStatus();
    }
}
