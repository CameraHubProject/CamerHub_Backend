package vn.camerahub.identity;

import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import vn.camerahub.identity.service.AccountService;
import vn.camerahub.identity.dto.IdentityDtos.*;
import vn.camerahub.identity.entity.Account;
import vn.camerahub.identity.repository.*;
import vn.camerahub.identity.mapper.ProfileMapper;
import vn.camerahub.shared.IntegrationEvents;
import vn.camerahub.shared.exception.ValidationException;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AccountEditingTest {
    AccountRepository repository = mock(AccountRepository.class);
    UserRoleJpaRepository roles = mock(UserRoleJpaRepository.class);
    IntegrationEvents events = mock(IntegrationEvents.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    Clock clock = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);
    AccountService service = new AccountService(repository, roles, mock(AuditLogJpaRepository.class), encoder, events, mock(ProfileMapper.class), clock);
    Account account;
    String oldHash;
    @BeforeEach void setup() {
        account = new Account(); account.setId(UUID.randomUUID()); account.setEmail("owner@example.test");
        account.setFullName("Old Name"); account.setAccountStatus("ACTIVE"); account.setProfileVersion(1L);
        oldHash = encoder.encode("old-password-123"); account.setPasswordHash(oldHash);
        when(repository.lockById(account.getId())).thenReturn(Optional.of(account));
        when(roles.findActiveRoles(account.getId())).thenReturn(List.of("USER"));
    }
    @Test void profileUpdatesExistingFieldsOnly() {
        service.update(account.getId(), " New Name ", "0912345678");
        assertThat(account.getFullName()).isEqualTo("New Name");
        assertThat(account.getEmail()).isEqualTo("owner@example.test");
        assertThat(account.getPasswordHash()).isEqualTo(oldHash);
        assertThat(account.getUpdatedAt()).isEqualTo(clock.instant());
        verify(repository).saveAndFlush(account); verify(events).publish(eq("identity.profile.v1"), eq(account.getId()), eq(2L), any());
    }
    @Test void invalidProfileIsNotSaved() {
        assertThatThrownBy(() -> service.update(account.getId(), " ", "0912345678")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> service.update(account.getId(), "Name", "abc")).isInstanceOf(RuntimeException.class);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void passwordUsesExistingEncoderAndReturnsNoSecret() {
        service.changePassword(account.getId(), new PasswordChange("old-password-123", "new-password-456", "new-password-456"));
        assertThat(encoder.matches("new-password-456", account.getPasswordHash())).isTrue();
        assertThat(account.getPasswordHash()).isNotEqualTo(oldHash);
        assertThat(account.getUpdatedAt()).isEqualTo(clock.instant());
        assertThat(new PasswordChange("secret", "secret", "secret").toString()).doesNotContain("secret");
    }
    @Test void wrongOldSameWeakMismatchedAndMultibytePasswordsAreRejected() {
        for (var input : List.of(new PasswordChange("wrong", "new-password-456", "new-password-456"),
            new PasswordChange("old-password-123", "old-password-123", "old-password-123"),
            new PasswordChange("old-password-123", "short", "short"),
            new PasswordChange("old-password-123", "new-password-456", "mismatch"),
            new PasswordChange("old-password-123", "ệ".repeat(25), "ệ".repeat(25)))) {
            assertThatThrownBy(() -> service.changePassword(account.getId(), input)).isInstanceOf(ValidationException.class);
        }
        assertThat(account.getPasswordHash()).isEqualTo(oldHash);
        verify(repository, never()).saveAndFlush(any());
    }
}
