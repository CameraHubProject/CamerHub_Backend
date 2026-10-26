package vn.camerahub.commerce.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class OrderException extends AppException {
    public OrderException(CommerceErrorCode code) {
        super(code);
    }

    public OrderException(CommerceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
