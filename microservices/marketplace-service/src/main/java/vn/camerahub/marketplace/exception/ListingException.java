package vn.camerahub.marketplace.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class ListingException extends AppException {
    public ListingException(MarketplaceErrorCode code) {
        super(code);
    }

    public ListingException(MarketplaceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
