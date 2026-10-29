package vn.camerahub.identity.dto;

import java.util.*;

import jakarta.validation.constraints.*;

public final class IdentityDtos {
    private IdentityDtos() {
    }

    public record Profile(@NotBlank @Size(max = 160) String fullName,
                          @NotBlank @Pattern(regexp = "[+0-9 ().-]{1,20}") String phone) {
        @com.fasterxml.jackson.annotation.JsonAnySetter
        public void rejectField(String name, Object value) {
            throw new vn.camerahub.shared.exception.ValidationException(Map.of(name, "Không được sửa trường này."));
        }
    }

    public record PasswordChange(@NotBlank @Size(max = 72) String currentPassword,
                                 @NotBlank @Size(min = 12, max = 72) String newPassword,
                                 @NotBlank @Size(max = 72) String confirmPassword) {
        @Override public String toString() { return "PasswordChange[REDACTED]"; }
    }

    public record Csrf(String headerName, String token) {
    }

    public record Registration(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(min = 12, max = 72) String password,
                               @NotBlank @Size(max = 160) String fullName,
                               @NotBlank @Pattern(regexp = "[+0-9 ().-]{1,20}") String phone, String confirmPassword) {
        public Registration(String email, String password, String fullName, String phone) {
            this(email, password, fullName, phone, null);
        }
    }
}

