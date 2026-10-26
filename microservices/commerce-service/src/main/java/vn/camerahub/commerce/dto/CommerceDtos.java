package vn.camerahub.commerce.dto;

import java.util.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class CommerceDtos {
    private CommerceDtos() {
    }

    public record Address(@NotBlank @Size(max = 160) String recipientName,
                          @NotBlank @Pattern(regexp = "[+0-9 ()-]{8,20}") String phone,
                          @NotBlank @Size(max = 160) String province, @NotBlank @Size(max = 500) String streetAddress,
                          boolean isDefault) {
    }

    public record Checkout(@NotNull UUID requestId, @NotEmpty @Size(max = 20) List<@NotNull UUID> productIds,
                           @NotBlank @Pattern(regexp = "DELIVERY|SHOWROOM") String delivery,
                           @NotNull @Valid Address address) {
    }

    public record Payment(@NotBlank @Pattern(regexp = "DEPOSIT|BALANCE_AND_SHIPPING") String stage) {
    }

    public record Review(boolean approve, @NotBlank @Size(max = 2000) String note) {
    }

    public record Buyback(@NotBlank @Size(max = 160) String model, @NotBlank @Size(max = 4000) String conditionNotes,
                          String expectedPriceVnd) {
    }
}

