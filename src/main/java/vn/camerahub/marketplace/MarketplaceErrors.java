package vn.camerahub.marketplace;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(basePackageClasses=MarketplaceController.class)
public class MarketplaceErrors {
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("message",e.getReason()==null?"Yêu cầu không hợp lệ.":e.getReason()));
    }
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> validation() {
        return ResponseEntity.badRequest().body(Map.of("message","Vui lòng kiểm tra các trường bắt buộc, số điện thoại và giá thiết bị."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class) ResponseEntity<?> conflict() {
        return ResponseEntity.status(409).body(Map.of("message","Dữ liệu vừa thay đổi. Vui lòng tải lại và thử lại."));
    }
}
