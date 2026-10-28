package vn.camerahub.marketplace.dto.response;

import java.util.*;
import java.time.*;

public record CameraModelResponse(UUID id, UUID brandId, String modelName, String deviceType, String specs) {
}
