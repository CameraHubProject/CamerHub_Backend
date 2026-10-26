package vn.camerahub.micro.exception;

import java.util.*;

import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.dao.*;
import org.springframework.web.bind.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import vn.camerahub.micro.dto.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<?> business(AppException e) {
        return failure(e.getErrorCode(), e.getErrors());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException e) {
        return failure(e.getStatusCode().value(), e.getReason() == null ? "Yêu cầu không hợp lệ." : e.getReason(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var fields = new LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), Objects.toString(error.getDefaultMessage(), "Không hợp lệ")));
        return business(new ValidationException(fields));
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, jakarta.validation.ConstraintViolationException.class})
    public ResponseEntity<?> invalid(Exception e) {
        return failure(CommonErrorCode.INVALID_REQUEST, Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> conflict() {
        return failure(CommonErrorCode.DATA_CONFLICT, Map.of());
    }

    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class, org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<?> changed() {
        return failure(CommonErrorCode.CONCURRENT_MODIFICATION, Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> size() {
        return failure(CommonErrorCode.UPLOAD_TOO_LARGE, Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> denied() {
        return failure(CommonErrorCode.ACCESS_DENIED, Map.of());
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<?> missingAccount() {
        return failure(CommonErrorCode.AUTHENTICATION_REQUIRED, Map.of());
    }

    @ExceptionHandler({org.springframework.web.servlet.resource.NoResourceFoundException.class, org.springframework.web.servlet.NoHandlerFoundException.class, org.springframework.web.HttpRequestMethodNotSupportedException.class, org.springframework.web.HttpMediaTypeNotSupportedException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<?> requestError(Exception e) {
        int code = ((org.springframework.web.ErrorResponse) e).getStatusCode().value();
        String message = switch (code) {
            case 404 -> "Không tìm thấy tài nguyên yêu cầu.";
            case 405 -> "Phương thức HTTP không được hỗ trợ.";
            case 415 -> "Định dạng dữ liệu không được hỗ trợ.";
            default -> "Yêu cầu thiếu thông tin bắt buộc.";
        };
        return failure(code, message, Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> unexpected(Exception e) {
        log.error("Unhandled backend failure: {} at {}", e.getClass().getName(), Arrays.stream(e.getStackTrace()).limit(6).toList());
        return failure(CommonErrorCode.INTERNAL_ERROR, Map.of());
    }

    private ResponseEntity<?> failure(int code, String message, Map<String, String> fields) {
        return ResponseEntity.status(code).cacheControl(CacheControl.noStore()).body(ApiResponse.error(code, message, fields));
    }

    private ResponseEntity<?> failure(ErrorCode code, Map<String, String> fields) {
        return ResponseEntity.status(code.getStatus()).cacheControl(CacheControl.noStore()).body(ApiResponse.error(code, fields));
    }
}
