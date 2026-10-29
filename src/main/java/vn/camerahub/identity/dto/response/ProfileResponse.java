package vn.camerahub.identity.dto.response;

import java.util.*;

public record ProfileResponse(UUID id, String email, String fullName, String phone, String status, List<String> roles, String avatarUrl) {
}
