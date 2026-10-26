package vn.camerahub.marketplace.util;

import vn.camerahub.marketplace.dto.MarketplaceDtos;
import vn.camerahub.marketplace.util.CameraBrands;

import jakarta.validation.Validation;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import vn.camerahub.marketplace.exception.BrandException;

import static org.assertj.core.api.Assertions.*;

class CameraBrandsTest {
    @Test
    void acceptsEveryStandardBrandIncludingAccentsAndKeepsOtherLast() {
        assertThat(CameraBrands.NAMES).hasSize(35).doesNotHaveDuplicates();
        assertThat(CameraBrands.NAMES.getLast()).isEqualTo("Khác");
        for (String name : CameraBrands.NAMES.subList(0, 34))
            assertThat(CameraBrands.resolve(name, null)).isEqualTo(name);
        assertThat(CameraBrands.resolve("Voigtländer", null)).isEqualTo("Voigtländer");
    }

    @Test
    void normalizesCustomNamesWithoutOverwritingCatalogNames() {
        assertThat(CameraBrands.resolve("Khác", "  Máy   Ảnh Việt  ")).isEqualTo("Máy Ảnh Việt");
        assertThat(CameraBrands.resolve("Khác", "canon")).isEqualTo("Canon");
        assertThat(CameraBrands.resolve("Khác", "OM System")).isEqualTo("OM System");
    }

    @Test
    void rejectsMissingUnknownAndInvalidCustomNames() {
        for (String name : List.of("", "Unknown"))
            assertThatThrownBy(() -> CameraBrands.resolve(name, null)).isInstanceOf(BrandException.class).hasMessageContaining("Vui lòng chọn lại hãng");
        assertThatThrownBy(() -> CameraBrands.resolve(null, null)).isInstanceOf(BrandException.class);
        for (String name : List.of("", "   ", "Khác", "a".repeat(101), "Invalid\u0000name"))
            assertThatThrownBy(() -> CameraBrands.resolve("Khác", name)).isInstanceOf(BrandException.class);
        assertThatThrownBy(() -> CameraBrands.resolve("Canon", "Other name")).isInstanceOf(BrandException.class);
    }

    @Test
    void retainsLegacyDtoContractsAndRejectsAmbiguousSelection() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var legacy = new vn.camerahub.marketplace.dto.MarketplaceDtos.Listing(UUID.randomUUID(), "Camera", "1000000", null, "Description", "Condition", null, null, List.of());
            assertThat(validator.validate(legacy)).isEmpty();
            var brand = new vn.camerahub.marketplace.dto.MarketplaceDtos.Listing(null, "Camera", "1000000", null, "Description", "Condition", null, null, List.of(), null, "BODY", "Khác", "Custom brand");
            assertThat(validator.validate(brand)).isEmpty();
            var ambiguous = new vn.camerahub.marketplace.dto.MarketplaceDtos.Listing(UUID.randomUUID(), "Camera", "1000000", null, "Description", "Condition", null, null, List.of(), null, "BODY", "Canon", null);
            assertThat(validator.validate(ambiguous)).isNotEmpty();
        }
    }
}

