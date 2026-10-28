package vn.camerahub.identity;

import org.junit.jupiter.api.Test;
import jakarta.validation.Validation;
import vn.camerahub.identity.dto.IdentityDtos.Registration;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationValidationTest {
    @Test
    void invalidRegistrationHasVietnameseErrorsForEveryInvalidField() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var errors = factory.getValidator().validate(new Registration("invalid", "short", "", "abc"));
            assertThat(errors).extracting(error -> error.getPropertyPath().toString())
                    .contains("email", "password", "fullName", "phone");
            assertThat(errors).extracting(error -> error.getMessage()).contains("Địa chỉ email không hợp lệ.", "Vui lòng nhập thông tin này.");
        }
    }
}
