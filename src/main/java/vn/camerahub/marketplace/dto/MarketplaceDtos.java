package vn.camerahub.marketplace.dto;

import java.util.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public final class MarketplaceDtos {
    private MarketplaceDtos() {
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

    public record Listing(UUID modelId, @NotBlank @Size(max = 240) String title, @NotBlank String priceVnd,
                          @Min(0) Integer shutterCount, @NotBlank @Size(max = 10000) String description,
                          @NotBlank @Size(max = 4000) String conditionNotes,
                          @Size(max = 2000) String accessories, @Size(max = 200) String serialNumber,
                          @NotNull @Size(max = 4) List<@Pattern(regexp = "TRAVEL|VLOG|PRO|BEGINNER") String> purposes,
                          UUID brandId, @Pattern(regexp = "BODY|BODY_LENS|LENS|OTHER") String deviceType,
                          @Size(max = 100, message = "Tên hãng máy không được vượt quá 100 ký tự.") String brandName,
                          @Size(max = 100, message = "Tên hãng máy không được vượt quá 100 ký tự.") String customBrandName) {
        @AssertTrue(message = "Vui lòng chọn một hãng máy hoặc model; chỉ nhập tên tùy chỉnh khi chọn Khác.")
        public boolean isSelectionValid() {
            return (modelId == null ? 0 : 1) + (brandId == null ? 0 : 1) + (brandName == null ? 0 : 1) == 1
                    && (customBrandName == null || "Khác".equals(brandName));
        }

        public Listing(UUID modelId, String title, String priceVnd, Integer shutterCount, String description,
                       String conditionNotes, String accessories, String serialNumber, List<String> purposes, UUID brandId, String deviceType) {
            this(modelId, title, priceVnd, shutterCount, description, conditionNotes, accessories, serialNumber, purposes, brandId, deviceType, null, null);
        }

        // Preserve the existing Java/API model-based contract for older clients.
        public Listing(UUID modelId, String title, String priceVnd, Integer shutterCount, String description,
                       String conditionNotes, String accessories, String serialNumber, List<String> purposes) {
            this(modelId, title, priceVnd, shutterCount, description, conditionNotes, accessories, serialNumber, purposes, null, null);
        }
    }

    public record BrandSelection(
            @NotBlank(message = "Vui lòng chọn hãng máy.") @Size(max = 100, message = "Tên hãng máy không được vượt quá 100 ký tự.") String brandName,
            @Size(max = 100, message = "Tên hãng máy không được vượt quá 100 ký tự.") String customBrandName) {
    }

    public record ListingReason(
            @NotBlank(message = "Vui lòng nhập lý do.")
            @Size(max = 2000, message = "Lý do tối đa 2000 ký tự.") String reason) {
    }

    public record Review(boolean approve, @NotBlank @Size(max = 2000) String note) {
    }

    public record Buyback(@NotBlank @Size(max = 160) String model, @NotBlank @Size(max = 4000) String conditionNotes,
                          String expectedPriceVnd) {
    }
}

