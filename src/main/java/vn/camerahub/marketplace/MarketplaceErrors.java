package vn.camerahub.marketplace;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
    @ExceptionHandler(MaxUploadSizeExceededException.class) ResponseEntity<?> photoTooLarge() {
        return ResponseEntity.status(413).body(Map.of("message","Ảnh tối đa 5 MB. Vui lòng chọn ảnh có dung lượng nhỏ hơn."));
    }
}
