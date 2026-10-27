package vn.camerahub.ai.dto.response;

import java.util.*;
import java.time.*;

public record AdvisorRateWindowResponse(String clientKey, Long minute, Integer requests) {
}
