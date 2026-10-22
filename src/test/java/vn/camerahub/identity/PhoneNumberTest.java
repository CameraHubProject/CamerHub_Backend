package vn.camerahub.identity;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberTest {
    @Test void normalizesLocalCountryCodeAndCommonFormatting() {
        for(String value : new String[]{"0912345678", "0912 345 678", "+84 912 345 678", "84912345678", "(0912) 345-678", "0912.345.678"})
            assertThat(PhoneNumber.normalize(value)).isEqualTo("+84912345678");
    }
    @Test void refusesIncompleteForeignAndInjectedIdentifiers() {
        for(String value : new String[]{"", "0912", "0012345678", "+849123456789", "+12025550123", "0912abc678", "0912345678@example.invalid", "' OR 1=1 --"})
            assertThat(PhoneNumber.normalize(value)).isNull();
        assertThat(PhoneNumber.normalize(null)).isNull();
    }
}
