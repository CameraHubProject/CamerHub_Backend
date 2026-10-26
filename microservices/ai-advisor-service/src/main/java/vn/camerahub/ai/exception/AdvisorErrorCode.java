package vn.camerahub.ai.exception;

import vn.camerahub.micro.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum AdvisorErrorCode implements ErrorCode {
    PROVIDER_QUOTA_EXHAUSTED(HttpStatus.SERVICE_UNAVAILABLE, "AI đang tạm hết lượt phục vụ. Bạn có thể thử lại sau hoặc xem kho máy."),
    PROVIDER_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Chưa kết nối được trợ lý AI. Vui lòng thử lại sau."),
    INVALID_AI_RESPONSE(HttpStatus.BAD_GATEWAY, "AI chưa trả về câu trả lời phù hợp. Vui lòng thử lại."),
    REQUEST_INTERRUPTED(HttpStatus.SERVICE_UNAVAILABLE, "Yêu cầu AI đã bị gián đoạn. Vui lòng thử lại."),
    PROVIDER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI đang phản hồi chậm. Vui lòng thử lại với câu hỏi ngắn hơn."),
    RESET_WHILE_BUSY(HttpStatus.CONFLICT, "Vui lòng đợi AI trả lời xong trước khi bắt đầu cuộc trò chuyện mới."),
    NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "Trợ lý AI chưa sẵn sàng. Bạn có thể tìm thiết bị trong kho máy."),
    CONVERSATION_BUSY(HttpStatus.CONFLICT, "AI đang trả lời câu hỏi trước. Vui lòng đợi một chút."),
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI đang bận tư vấn. Bạn vui lòng thử lại sau ít giây."),
    ANSWER_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI đang phản hồi chậm. Vui lòng hỏi ngắn gọn hơn rồi thử lại."),
    CONSULTATION_INCOMPLETE(HttpStatus.BAD_GATEWAY, "AI chưa hoàn tất tư vấn. Vui lòng chia nhỏ câu hỏi và thử lại."),
    ANSWER_INCOMPLETE(HttpStatus.BAD_GATEWAY, "AI chưa hoàn tất câu trả lời. Vui lòng thử lại với câu hỏi ngắn hơn."),
    DAILY_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "AI đã đạt giới hạn lượt tư vấn. Bạn vui lòng thử lại sau."),
    PREVIOUS_CONVERSATION_BUSY(HttpStatus.CONFLICT, "Vui lòng đợi cuộc trò chuyện trước hoàn tất."),
    CONVERSATION_CHANGED(HttpStatus.CONFLICT, "Cuộc trò chuyện đã thay đổi."),
    RESPONSE_IN_PROGRESS(HttpStatus.CONFLICT, "Vui lòng đợi AI trả lời xong.");
    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return "AI_" + name();
    }
}
