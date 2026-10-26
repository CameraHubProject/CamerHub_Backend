package vn.camerahub.micro.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AppException {
    public ResourceNotFoundException(String message) {
        super(new ResourceError(message));
    }

    public ResourceNotFoundException(ErrorCode code) {
        super(code);
    }

    private record ResourceError(String message) implements ErrorCode {
        public String getCode() {
            return CommonErrorCode.RESOURCE_NOT_FOUND.getCode();
        }

        public HttpStatus getStatus() {
            return HttpStatus.NOT_FOUND;
        }

        public String getMessage() {
            return message;
        }
    }
}
