package vn.camerahub.moderation.exception;

import vn.camerahub.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum ModerationErrorCode implements ErrorCode {
    PROVIDER_QUOTA_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "AI đang tạm hết lượt phục vụ. Bạn có thể thử lại sau hoặc xem kho máy."),
    PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Chưa kết nối được trợ lý AI. Vui lòng thử lại sau."),
    INVALID_AI_RESPONSE(HttpStatus.BAD_GATEWAY, "AI chưa trả về câu trả lời phù hợp. Vui lòng thử lại."),
    REQUEST_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "Yêu cầu AI đã bị gián đoạn. Vui lòng thử lại."),
    PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI đang phản hồi chậm. Vui lòng thử lại với câu hỏi ngắn hơn."),
    REVIEW_REVISION_CHANGED(HttpStatus.CONFLICT, "Tin đã thay đổi hoặc không còn chờ Admin duyệt."),
    SELLER_INACTIVE(HttpStatus.CONFLICT, "Tài khoản người bán không hoạt động."),
    DUPLICATE_LISTING(HttpStatus.CONFLICT, "Tin có nội dung hoặc ảnh trùng với tin đăng khác. Không thể duyệt tin trùng.");
    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return "MODERATION_" + name();
    }
}
