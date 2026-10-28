package vn.camerahub.moderation.dto;

import java.util.*;

import jakarta.validation.constraints.*;

public final class ModerationDtos {
    private ModerationDtos() {
    }

    public record Review(boolean approve, @NotBlank @Size(max = 2000) String note, @NotNull @Min(0) Long revision) {
    }
}

