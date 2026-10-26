package vn.camerahub.identity.dto;

import java.util.*;

import jakarta.validation.constraints.*;

public final class AddressDtos {
    private AddressDtos() {
    }

    public record Address(@NotBlank @Size(max = 160) String recipientName,
                          @NotBlank @Pattern(regexp = "[+0-9 ()-]{8,20}") String phone,
                          @NotBlank @Size(max = 160) String province, @NotBlank @Size(max = 500) String streetAddress,
                          boolean isDefault) {
    }
}

