package vn.camerahub.commerce.dto.response;

import java.util.*;
import java.time.*;

public record ShowroomResponse(UUID id, String name, String address, Boolean active) {
}
