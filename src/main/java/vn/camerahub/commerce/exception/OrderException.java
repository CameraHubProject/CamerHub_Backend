package vn.camerahub.commerce.exception;

import vn.camerahub.shared.exception.AppException;

import java.util.Map;

public final class OrderException extends AppException {
    public OrderException(CommerceErrorCode code) {
        super(code);
    }

    public OrderException(CommerceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
