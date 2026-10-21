package vn.camerahub.identity;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackageClasses = IdentityController.class)
public class IdentityErrors {
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message",
                exception.getReason() == null ? "Yêu cầu không hợp lệ." : exception.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation() {
        return ResponseEntity.badRequest().body(Map.of("message", "Kiểm tra email, họ tên, số điện thoại và mật khẩu (12–72 ký tự)."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("message", "Email hoặc hồ sơ đã tồn tại. Hãy tải lại và kiểm tra."));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<?> size() {
        return ResponseEntity.status(413).body(Map.of("message", "Hồ sơ tối đa 5 MB."));
    }
}
