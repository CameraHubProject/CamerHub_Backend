package vn.camerahub.identity.dto.response;

import java.util.*;
import java.time.*;

public record UserAddressResponse(UUID id, UUID userId, String recipientName, String phone, String province,
                                  String district, String ward, String streetAddress, Boolean isDefault,
                                  Instant createdAt) {
}
