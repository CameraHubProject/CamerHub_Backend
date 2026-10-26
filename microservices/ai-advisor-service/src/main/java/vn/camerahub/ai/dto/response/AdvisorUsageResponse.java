package vn.camerahub.ai.dto.response;

import java.util.*;
import java.time.*;

public record AdvisorUsageResponse(LocalDate day, String clientKey, Integer requests) {
}
