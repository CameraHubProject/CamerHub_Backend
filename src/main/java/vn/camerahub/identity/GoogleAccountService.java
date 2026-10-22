package vn.camerahub.identity;

import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Only called with claims already verified by Spring's OIDC authentication provider. */
@Service
public class GoogleAccountService {
    private final JdbcTemplate jdbc;
    private final AccountService accounts;

    public GoogleAccountService(JdbcTemplate jdbc, AccountService accounts) { this.jdbc = jdbc; this.accounts = accounts; }

    @Transactional
    public AccountService.Account resolve(Identity identity) {
        var linked = jdbc.queryForList("SELECT user_id FROM camerahub.auth_identities WHERE provider='google' AND subject=?", UUID.class, identity.subject());
        if (!linked.isEmpty()) return active(linked.getFirst());
        // Matching an email alone never grants access to an existing local account.
        var existing = jdbc.queryForList("SELECT id FROM camerahub.users WHERE lower(email)=?", UUID.class, identity.email());
        if (!existing.isEmpty()) { active(existing.getFirst()); return null; }
        var account = accounts.register(identity.email(), UUID.randomUUID().toString() + UUID.randomUUID(), identity.name(), null);
        jdbc.update("UPDATE camerahub.users SET email_verified_at=now() WHERE id=?", account.id());
        link(identity, account.id());
        return account;
    }

    @Transactional
    public AccountService.Account confirmLink(Identity identity, UUID userId) {
        var account = active(userId);
        if (!account.email().equalsIgnoreCase(identity.email())) throw failure("link_failed");
        jdbc.update("INSERT INTO camerahub.auth_identities(provider,subject,user_id) VALUES ('google',?,?)", identity.subject(), userId);
        jdbc.update("UPDATE camerahub.users SET email_verified_at=COALESCE(email_verified_at,now()) WHERE id=?", userId);
        accounts.audit(userId, userId, "GOOGLE_LINKED");
        return account;
    }

    private void link(Identity identity, UUID id) {
        jdbc.update("INSERT INTO camerahub.auth_identities(provider,subject,user_id) VALUES ('google',?,?)", identity.subject(), id);
        accounts.audit(id, id, "GOOGLE_REGISTERED");
    }

    private AccountService.Account active(UUID id) {
        var account = accounts.get(id);
        if (!"ACTIVE".equals(account.status())) throw failure("account_inactive");
        return account;
    }

    public static Identity verified(String subject, String email, Boolean emailVerified, String name) {
        if (!Boolean.TRUE.equals(emailVerified) || subject == null || subject.isBlank() || subject.length() > 255
                || email == null || email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw failure("unverified_email");
        var normalized = AccountService.normalizeEmail(email);
        var display = name == null || name.isBlank() ? normalized.substring(0, normalized.indexOf('@')) : name.strip();
        if (display.length() > 160) display = display.substring(0, 160);
        return new Identity(subject, normalized, display);
    }

    static OAuth2AuthenticationException failure(String code) { return new OAuth2AuthenticationException(new OAuth2Error(code)); }
    public record Identity(String subject, String email, String name) implements java.io.Serializable {}
    public record PendingLink(Identity identity, Instant expiresAt, int attempts) implements java.io.Serializable {}
}
