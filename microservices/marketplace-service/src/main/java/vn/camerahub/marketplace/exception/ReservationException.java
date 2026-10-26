package vn.camerahub.marketplace.exception;

import vn.camerahub.micro.exception.AppException;

import java.util.Map;

public final class ReservationException extends AppException {
    public ReservationException(MarketplaceErrorCode code) {
        super(code);
    }

    public ReservationException(MarketplaceErrorCode code, Map<String, String> fields) {
        super(code, fields);
    }
}
