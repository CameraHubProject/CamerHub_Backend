package vn.camerahub.marketplace.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class BrandException extends AppException {
    public BrandException(MarketplaceErrorCode code) {
        super(code);
    }

    public BrandException(MarketplaceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
