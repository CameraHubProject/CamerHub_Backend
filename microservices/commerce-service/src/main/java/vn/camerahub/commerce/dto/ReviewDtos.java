package vn.camerahub.commerce.dto;

import java.util.*;

import jakarta.validation.constraints.*;

public final class ReviewDtos {
    private ReviewDtos() {
    }

    public record Review(@NotNull @Min(1)
                         @Max(5) Integer rating,
                         @NotBlank @Size(max = 2000) String comment) {
    }
}

