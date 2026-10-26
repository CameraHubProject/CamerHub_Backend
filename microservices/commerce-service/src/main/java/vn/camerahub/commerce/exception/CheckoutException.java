package vn.camerahub.commerce.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class CheckoutException extends AppException {
    public CheckoutException(CommerceErrorCode code) {
        super(code);
    }

    public CheckoutException(CommerceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
