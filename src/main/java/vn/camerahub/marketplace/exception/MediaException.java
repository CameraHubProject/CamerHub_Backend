package vn.camerahub.marketplace.exception;

import vn.camerahub.shared.exception.AppException;

import java.util.Map;

public final class MediaException extends AppException {
    public MediaException(MarketplaceErrorCode code) {
        super(code);
    }

    public MediaException(MarketplaceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
