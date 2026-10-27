package vn.camerahub.identity;

import org.junit.jupiter.api.Test;
import vn.camerahub.identity.exception.*;
import vn.camerahub.identity.service.*;
import vn.camerahub.identity.dto.IdentityDtos.Registration;

import static org.assertj.core.api.Assertions.*;

class AccountErrorTest {
    @Test
    void passwordConfirmationIsEnforcedByServiceWithoutFrontendLogic() {
        var service = new AccountService(null, null, null, null, null, null, null);
        var error = catchThrowableOfType(() -> service.register(new Registration("a@b.test", "long-password", "Name", "0912345678", "different")), AccountException.class);
        assertThat(error.getErrorCode()).isEqualTo(IdentityErrorCode.PASSWORD_CONFIRM_MISMATCH);
        assertThat(error.getErrors()).containsKey("confirmPassword");
    }

    @Test
    void oauthStatusDoesNotReflectUntrustedReasonText() {
        assertThat(GoogleAccountService.loginMessage("<script>private-data</script>", false)).isEmpty();
        assertThat(GoogleAccountService.loginMessage("cancelled", false)).contains("hủy");
    }
}
