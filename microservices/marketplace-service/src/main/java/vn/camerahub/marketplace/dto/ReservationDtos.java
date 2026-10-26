package vn.camerahub.marketplace.dto;

import java.util.*;

import jakarta.validation.constraints.*;

public final class ReservationDtos {
    private ReservationDtos() {
    }

    public record Reserve(@NotNull UUID requestId, @NotNull UUID buyerId,
                          @NotEmpty @Size(max = 20) List<@NotNull UUID> productIds) {
    }
}

