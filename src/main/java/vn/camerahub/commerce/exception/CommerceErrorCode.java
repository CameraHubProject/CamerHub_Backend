package vn.camerahub.commerce.exception;

import vn.camerahub.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum CommerceErrorCode implements ErrorCode {
    DUPLICATE_PRODUCT(HttpStatus.BAD_REQUEST, "Mỗi thiết bị chỉ có một chiếc."),
    CHECKOUT_REQUEST_REUSED(HttpStatus.CONFLICT, "Mã yêu cầu đã dùng cho giỏ hàng khác."),
    CHECKOUT_CANCELLED(HttpStatus.CONFLICT, "Checkout này đã hủy. Vui lòng tạo yêu cầu mới."),
    CHECKOUT_PROCESSING(HttpStatus.CONFLICT, "Giỏ hàng đang được xử lý. Vui lòng thử lại với cùng yêu cầu."),
    CHECKOUT_RECONCILING(HttpStatus.SERVICE_UNAVAILABLE, "Checkout đang chờ đối soát. Hãy thử lại cùng yêu cầu."),
    SHOWROOM_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng chọn showroom."),
    SHOWROOM_UNAVAILABLE(HttpStatus.BAD_REQUEST, "Showroom chưa khả dụng."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy đơn hàng."),
    ORDER_CANNOT_CANCEL(HttpStatus.CONFLICT, "Chỉ tự hủy được đơn chưa thanh toán."),
    INVALID_PAYMENT_STAGE(HttpStatus.BAD_REQUEST, "Giai đoạn thanh toán không hợp lệ."),
    PAYMENT_STAGE_EXPIRED(HttpStatus.CONFLICT, "Đơn hàng không còn ở bước thanh toán này hoặc đã hết hạn."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "Giá phải là số nguyên dương, tối đa 13 chữ số."),
    REVIEW_NOT_ALLOWED(HttpStatus.CONFLICT, "Chỉ đánh giá sau khi đơn hàng hoàn tất."),
    SELLER_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy người bán."),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "Trang dữ liệu không hợp lệ."),
    CHECKOUT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy yêu cầu đặt hàng.");
    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return "COMMERCE_" + name();
    }
}
