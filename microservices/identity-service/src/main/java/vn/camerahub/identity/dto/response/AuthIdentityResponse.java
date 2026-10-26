package vn.camerahub.identity.dto.response;

import java.util.*;
import java.time.*;

public record AuthIdentityResponse(String provider, String subject, UUID userId, Instant createdAt) {
}
