package vn.camerahub.commerce.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class ReviewException extends AppException {
    public ReviewException(CommerceErrorCode code) {
        super(code);
    }

    public ReviewException(CommerceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
