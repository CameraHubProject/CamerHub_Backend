package vn.camerahub.micro.exception;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Preserves the public error contract when a request crosses service boundaries.
 */
public final class RemoteServiceException extends AppException {
    public RemoteServiceException(HttpStatus status, String code, String message, Map<String, String> errors) {
        super(new RemoteErrorCode(status, code, message), errors);
    }

    private record RemoteErrorCode(HttpStatus status, String code, String message) implements ErrorCode {
        public HttpStatus getStatus() {
            return status;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}
