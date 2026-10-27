package vn.camerahub.ai.dto;

import java.util.*;

import jakarta.validation.constraints.*;

public final class AiChatDtos {
    private AiChatDtos() {
    }

    public record Chat(@NotBlank @Size(max = 2000) String message, boolean webSearch, UUID productId) {
    }
}

