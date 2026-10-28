package vn.camerahub.shared.dto;

import java.time.Instant;
import java.util.Map;

public record ApiResponse<T>(int code, String errorCode, String message, T data, Map<String, String> errors,
                             Instant timestamp) {
    public static <T> ApiResponse<T> success(int code, String message, T data) {
        return new ApiResponse<>(code, null, message, data, Map.of(), Instant.now());
    }

    public static ApiResponse<Void> error(int code, String message, Map<String, String> errors) {
        return new ApiResponse<>(code, vn.camerahub.shared.exception.CommonErrorCode.forStatus(code).getCode(), message, null, errors, Instant.now());
    }

    public static ApiResponse<Void> error(vn.camerahub.shared.exception.ErrorCode code, Map<String, String> errors) {
        return new ApiResponse<>(code.getStatus().value(), code.getCode(), code.getMessage(), null, errors, Instant.now());
    }
}
