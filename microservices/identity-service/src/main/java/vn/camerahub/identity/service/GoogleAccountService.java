package vn.camerahub.identity.service;

import vn.camerahub.identity.service.AccountService;
import vn.camerahub.identity.repository.GoogleAccountRepository;
import vn.camerahub.identity.entity.*;

import java.time.Instant;
import java.util.UUID;

import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Only called with claims already verified by Spring's OIDC authentication provider.
 */
@Service
@lombok.RequiredArgsConstructor
public class GoogleAccountService {
    private final GoogleAccountRepository repository;
    private final AccountService accounts;

    @Transactional
    public Account resolve(Identity identity) {
        var linked = repository.findAuthIdentities(UUID.class, identity.subject());
        if (!linked.isEmpty()) return active(linked.getFirst());
        // Matching an email alone never grants access to an existing local account.
        var existing = repository.findUsers(UUID.class, identity.email());
        if (!existing.isEmpty()) {
            active(existing.getFirst());
            return null;
        }
        var account = accounts.register(identity.email(), UUID.randomUUID().toString() + UUID.randomUUID(), identity.name(), null);
        repository.updateUsers(account.getId());
        link(identity, account.getId());
        return account;
    }

    @Transactional
    public Account confirmLink(Identity identity, UUID userId) {
        var account = active(userId);
        if (!account.getEmail().equalsIgnoreCase(identity.email())) throw failure("link_failed");
        repository.insertAuthIdentities(identity.subject(), userId);
        repository.updateUsers2(userId);
        accounts.audit(userId, userId, "GOOGLE_LINKED");
        return account;
    }

    private void link(Identity identity, UUID id) {
        repository.insertAuthIdentities2(identity.subject(), id);
        accounts.audit(id, id, "GOOGLE_REGISTERED");
    }

    private Account active(UUID id) {
        var account = accounts.get(id);
        if (!"ACTIVE".equals(account.getAccountStatus())) throw failure("account_inactive");
        return account;
    }

    public static Identity verified(String subject, String email, Boolean emailVerified, String name) {
        if (!Boolean.TRUE.equals(emailVerified) || subject == null || subject.isBlank() || subject.length() > 255
                || email == null || email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))
            throw failure("unverified_email");
        var normalized = AccountService.normalizeEmail(email);
        var display = name == null || name.isBlank() ? normalized.substring(0, normalized.indexOf('@')) : name.strip();
        if (display.length() > 160) display = display.substring(0, 160);
        return new Identity(subject, normalized, display);
    }

    static OAuth2AuthenticationException failure(String code) {
        return new OAuth2AuthenticationException(new OAuth2Error(code));
    }

    public static String loginMessage(String reason, boolean pending) {
        if (reason == null) return "";
        return switch (reason) {
            case "cancelled" -> "Bạn đã hủy đăng nhập Google. Có thể thử lại bất cứ lúc nào.";
            case "inactive" -> "Tài khoản CameraHub hiện không hoạt động. Vui lòng liên hệ hỗ trợ.";
            case "unverified" -> "Google chưa xác minh email này. Hãy xác minh email rồi thử lại.";
            case "failed" -> "Chưa đăng nhập được bằng Google. Vui lòng thử lại từ nút bên dưới.";
            case "link_required" -> pending ? "" : "Phiên liên kết đã hết hạn. Hãy bắt đầu lại từ nút Google bên dưới.";
            default -> "";
        };
    }

    public record Identity(String subject, String email, String name) implements java.io.Serializable {
    }

    public record PendingLink(Identity identity, Instant expiresAt, int attempts) implements java.io.Serializable {
    }
}

