package vn.camerahub.marketplace.exception;

import vn.camerahub.shared.exception.ErrorCode;
import org.springframework.http.HttpStatus;

@lombok.Getter
@lombok.RequiredArgsConstructor
public enum MarketplaceErrorCode implements ErrorCode {
    BRAND_ALREADY_EXISTS(HttpStatus.CONFLICT, "Hãng máy này đã tồn tại. Vui lòng chọn tên khác."),
    BRAND_IN_USE(HttpStatus.CONFLICT, "Hãng đã có dòng máy hoặc sản phẩm sử dụng nên không thể xóa. Bạn vẫn có thể sửa tên hãng."),
    BRAND_NOT_FOUND(HttpStatus.NOT_FOUND, "Hãng máy không còn tồn tại. Vui lòng tải lại danh sách."),
    ACCOUNT_INACTIVE(HttpStatus.CONFLICT, "Tài khoản không hoạt động."),
    DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bản nháp của bạn."),
    SUBMISSION_NOT_ALLOWED(HttpStatus.CONFLICT, "Không thể gửi duyệt tin ở trạng thái này."),
    PHOTO_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng thêm ít nhất một ảnh thực tế."),
    PRODUCT_NOT_AVAILABLE(HttpStatus.NOT_FOUND, "Thiết bị không còn được đăng bán."),
    EDITABLE_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bản nháp có thể chỉnh sửa."),
    LISTING_NOT_EDITABLE(HttpStatus.CONFLICT, "Tin không còn ở trạng thái cho phép chỉnh sửa."),
    INSPECTED_MODEL_LOCKED(HttpStatus.CONFLICT, "Thiết bị đã có hồ sơ kiểm định. Vui lòng liên hệ hỗ trợ để điều chỉnh hãng/model."),
    INVALID_BRAND_SELECTION(HttpStatus.BAD_REQUEST, "Vui lòng chọn một hãng máy hợp lệ."),
    MODEL_NOT_FOUND(HttpStatus.BAD_REQUEST, "Vui lòng chọn model có trong danh mục."),
    BRAND_NOT_AVAILABLE(HttpStatus.BAD_REQUEST, "Hãng máy không có trong danh mục."),
    LISTING_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy tin đăng của bạn."),
    LISTING_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng nhập lý do, tối đa 2000 ký tự."),
    LISTING_WITHDRAWAL_NOT_ALLOWED(HttpStatus.CONFLICT, "Tin đã được đặt mua hoặc đã bán, không thể ẩn hoặc xóa."),
    BRAND_EDIT_NOT_ALLOWED(HttpStatus.CONFLICT, "Không thể sửa hãng của tin đang duyệt, bị từ chối hoặc đã đặt mua/đã bán."),
    INSPECTED_BRAND_LOCKED(HttpStatus.CONFLICT, "Thiết bị đã có hồ sơ kiểm định. Vui lòng liên hệ hỗ trợ để điều chỉnh hãng máy."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "Giá phải là số nguyên dương, tối đa 13 chữ số."),
    PHOTO_UPLOAD_NOT_ALLOWED(HttpStatus.NOT_FOUND, "Không tìm thấy tin đăng được phép bổ sung ảnh."),
    PHOTO_TOO_LARGE(HttpStatus.BAD_REQUEST, "Ảnh tối đa 5 MB."),
    INVALID_PHOTO(HttpStatus.BAD_REQUEST, "Ảnh không hợp lệ."),
    UNSUPPORTED_PHOTO_FORMAT(HttpStatus.BAD_REQUEST, "Chỉ nhận ảnh JPG hoặc PNG."),
    PHOTO_DIMENSIONS_EXCEEDED(HttpStatus.BAD_REQUEST, "Ảnh tối đa 24 megapixel."),
    UNREADABLE_PHOTO(HttpStatus.BAD_REQUEST, "Không đọc được ảnh. Vui lòng chọn lại ảnh JPG hoặc PNG hợp lệ."),
    PHOTO_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "Mỗi tin đăng tối đa 8 ảnh."),
    MEDIA_DRAFT_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy bản nháp được phép chỉnh sửa."),
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy ảnh trong bản nháp này."),
    INVALID_FILTER(HttpStatus.BAD_REQUEST, "Bộ lọc không hợp lệ."),
    DUPLICATE_PRODUCT(HttpStatus.BAD_REQUEST, "Mỗi thiết bị chỉ có một chiếc."),
    RESERVATION_REQUEST_REUSED(HttpStatus.CONFLICT, "Mã giỏ hàng đã được dùng cho dữ liệu khác."),
    RESERVATION_CANCELLED(HttpStatus.CONFLICT, "Giỏ hàng này đã bị hủy."),
    OUT_OF_STOCK(HttpStatus.CONFLICT, "Một thiết bị vừa hết hàng. Vui lòng kiểm tra lại giỏ."),
    SELF_PURCHASE(HttpStatus.BAD_REQUEST, "Bạn không thể mua thiết bị của chính mình."),
    SELLER_INACTIVE(HttpStatus.CONFLICT, "Người bán không còn hoạt động."),
    PRODUCT_RESERVED(HttpStatus.CONFLICT, "Thiết bị đã được giữ chỗ."),
    RESERVATION_UNCONFIRMED(HttpStatus.CONFLICT, "Đơn hàng chưa hoàn tất giữ chỗ."),
    CHECKOUT_CANCELLATION_UNCONFIRMED(HttpStatus.CONFLICT, "Chưa có xác nhận hủy checkout."),
    ORDER_CANCELLATION_UNCONFIRMED(HttpStatus.CONFLICT, "Chưa có xác nhận hủy đơn."),
    BRAND_LIST_CHANGED(HttpStatus.BAD_REQUEST, "Danh sách hãng máy đã thay đổi. Vui lòng chọn lại hãng máy."),
    CUSTOM_BRAND_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Chỉ nhập tên hãng tùy chỉnh khi chọn Khác."),
    CUSTOM_BRAND_REQUIRED(HttpStatus.BAD_REQUEST, "Vui lòng nhập tên hãng máy cụ thể."),
    BRAND_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "Tên hãng máy không được vượt quá 100 ký tự."),
    INVALID_BRAND_NAME(HttpStatus.BAD_REQUEST, "Tên hãng máy chứa ký tự không hợp lệ.");
    private final HttpStatus status;
    private final String message;

    public String getCode() {
        return "MARKETPLACE_" + name();
    }
}
