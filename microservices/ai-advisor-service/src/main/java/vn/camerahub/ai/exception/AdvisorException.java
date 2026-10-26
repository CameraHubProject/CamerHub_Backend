package vn.camerahub.ai.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class AdvisorException extends AppException {
    public AdvisorException(AdvisorErrorCode code) {
        super(code);
    }

    public AdvisorException(AdvisorErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
