package vn.camerahub.identity.service;

import vn.camerahub.identity.exception.*;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.camerahub.identity.entity.*;
import vn.camerahub.identity.repository.*;
import vn.camerahub.identity.mapper.ProfileMapper;
import vn.camerahub.identity.dto.response.PublicAccountResponse;
import vn.camerahub.identity.util.PhoneNumber;
import vn.camerahub.shared.IntegrationEvents;
import vn.camerahub.shared.exception.*;

@Service
@Transactional(readOnly = true)
@lombok.RequiredArgsConstructor
public class AccountService implements UserDetailsService {
    private final AccountRepository repository;
    private final UserRoleJpaRepository roles;
    private final AuditLogJpaRepository audit;
    private final PasswordEncoder passwords;
    private final IntegrationEvents events;
    private final ProfileMapper mapper;
    private final Clock clock;

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        if (identifier == null || identifier.length() > 254) throw new UsernameNotFoundException("Invalid account");
        boolean email = identifier.contains("@");
        String key = email ? normalizeEmail(identifier) : PhoneNumber.normalize(identifier);
        if (key == null) throw new UsernameNotFoundException("Invalid account");
        var account = (email ? repository.findByEmailIgnoreCase(key) : repository.findByLoginPhone(key)).orElseThrow(() -> new UsernameNotFoundException("Invalid account"));
        return User.withUsername(account.getId().toString()).password(account.getPasswordHash()).disabled(!"ACTIVE".equals(account.getAccountStatus())).roles(roles.findActiveRoles(account.getId()).toArray(String[]::new)).build();
    }

    @Transactional
    public Account register(vn.camerahub.identity.dto.IdentityDtos.Registration request) {
        if (request.confirmPassword() != null && !request.confirmPassword().equals(request.password()))
            throw new AccountException(IdentityErrorCode.PASSWORD_CONFIRM_MISMATCH);
        return register(request.email(), request.password(), request.fullName(), request.phone());
    }

    @Transactional
    public Account register(String email, String password, String fullName, String phone) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new AccountException(IdentityErrorCode.PASSWORD_TOO_LONG);
        String canonical = PhoneNumber.normalize(phone);
        if (phone != null && !phone.isBlank() && canonical == null)
            throw new AccountException(IdentityErrorCode.INVALID_PHONE);
        if (repository.findByEmailIgnoreCase(normalizeEmail(email)).isPresent())
            throw new AccountException(IdentityErrorCode.EMAIL_ALREADY_USED);
        if (canonical != null && repository.findByLoginPhone(canonical).isPresent())
            throw new AccountException(IdentityErrorCode.PHONE_ALREADY_USED);
        var account = new Account();
        account.setId(UUID.randomUUID());
        account.setEmail(normalizeEmail(email));
        account.setPasswordHash(passwords.encode(password));
        account.setFullName(fullName.strip());
        account.setPhone(canonical);
        account.setAccountStatus("ACTIVE");
        account.setCreatedAt(clock.instant());
        account.setUpdatedAt(clock.instant());
        account.setProfileVersion(0L);
        saveAccount(account);
        var role = new UserRole();
        role.setUserId(account.getId());
        role.setRoleCode("BUYER");
        roles.saveAndFlush(role);
        audit(account.getId(), account.getId(), "ACCOUNT_REGISTERED");
        changed(account);
        account.setRoles(List.of("USER"));
        return account;
    }

    public Account get(UUID id) {
        var account = repository.findById(id).orElseThrow(() -> new UsernameNotFoundException("Invalid account"));
        account.setRoles(roles.findActiveRoles(id));
        return account;
    }

    @Transactional
    public Account update(UUID id, String fullName, String phone) {
        String canonical = PhoneNumber.normalize(phone);
        if (canonical == null)
            throw new AccountException(IdentityErrorCode.INVALID_PHONE);
        var account = repository.lockById(id).orElseThrow(() -> new AccountException(IdentityErrorCode.USER_NOT_FOUND));
        if (!"ACTIVE".equals(account.getAccountStatus()))
            throw new AccountException(IdentityErrorCode.ACCOUNT_INACTIVE);
        if (repository.findByLoginPhone(canonical).filter(other -> !other.getId().equals(id)).isPresent())
            throw new AccountException(IdentityErrorCode.PHONE_ALREADY_USED);
        account.setFullName(fullName.strip());
        account.setPhone(canonical);
        account.setUpdatedAt(clock.instant());
        account.setProfileVersion(account.getProfileVersion() + 1);
        saveAccount(account);
        audit(id, id, "PROFILE_UPDATED");
        changed(account);
        account.setRoles(roles.findActiveRoles(id));
        return account;
    }

    public PublicAccountResponse publicData(UUID id) {
        return mapper.toPublicResponse(repository.findById(id).orElseThrow(() -> new AccountException(IdentityErrorCode.MEMBER_NOT_FOUND)));
    }

    private void changed(Account user) {
        events.publish("identity.profile.v1", user.getId(), user.getProfileVersion(), mapper.toPublicResponse(user));
    }

    @Transactional
    public void audit(UUID actor, UUID entity, String action) {
        var item = new AuditLog();
        item.setId(UUID.randomUUID());
        item.setActorId(actor);
        item.setEntityType("IDENTITY");
        item.setEntityId(entity);
        item.setAction(action);
        item.setOccurredAt(clock.instant());
        audit.saveAndFlush(item);
    }

    public static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    private void saveAccount(Account account) {
        try {
            repository.saveAndFlush(account);
        } catch (org.springframework.dao.DataIntegrityViolationException error) {
            String detail = Objects.toString(error.getMostSpecificCause().getMessage(), "");
            if (detail.contains("uq_users_email_lower"))
                throw new AccountException(IdentityErrorCode.EMAIL_ALREADY_USED);
            if (detail.contains("uq_users_login_phone"))
                throw new AccountException(IdentityErrorCode.PHONE_ALREADY_USED);
            throw error;
        }
    }
}
