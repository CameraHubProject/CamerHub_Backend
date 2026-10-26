package vn.camerahub.moderation.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class ModerationException extends AppException {
    public ModerationException(ModerationErrorCode code) {
        super(code);
    }

    public ModerationException(ModerationErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
