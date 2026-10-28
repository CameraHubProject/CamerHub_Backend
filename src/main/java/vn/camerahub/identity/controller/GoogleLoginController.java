package vn.camerahub.identity.controller;

import vn.camerahub.identity.exception.*;

import vn.camerahub.identity.service.AccountService;
import vn.camerahub.identity.service.GoogleAccountService;
import vn.camerahub.identity.config.GoogleLoginHandlers;
import vn.camerahub.identity.entity.*;

import java.time.Instant;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/auth/google")
public class GoogleLoginController {
    private final AccountService accounts;
    private final GoogleAccountService google;
    private final PasswordEncoder passwords;
    private final boolean enabled;
    private final vn.camerahub.identity.service.AuthTokenService tokens;

    public GoogleLoginController(AccountService accounts, GoogleAccountService google, PasswordEncoder passwords, vn.camerahub.identity.service.AuthTokenService tokens,
                                 @Value("${camerahub.google.enabled}") boolean enabled) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.google = google;
        this.passwords = passwords;
        this.enabled = enabled;
    }

    @GetMapping("/status")
    public Status status(HttpServletRequest request, @RequestParam(required = false) String reason) {
        var pending = pending(request);
        return new Status(enabled, pending == null ? null : pending.identity().email(), GoogleAccountService.loginMessage(reason, pending != null));
    }

    @PostMapping("/link")
    public vn.camerahub.shared.dto.ApiResponse<Void> link(@Valid @RequestBody Confirmation confirmation, HttpServletRequest request, HttpServletResponse response) {
        var pending = pending(request);
        if (!enabled || pending == null)
            throw new AccountException(IdentityErrorCode.GOOGLE_LINK_EXPIRED);
        var session = request.getSession();
        // Attempts belong to this short-lived, server-verified Google identity, never to client input.
        session.setAttribute(GoogleLoginHandlers.PENDING, new GoogleAccountService.PendingLink(pending.identity(), pending.expiresAt(), pending.attempts() + 1));
        var user = accounts.loadUserByUsername(pending.identity().email());
        if (!user.isEnabled() || !passwords.matches(confirmation.password(), user.getPassword())) {
            if (pending.attempts() >= 4) session.removeAttribute(GoogleLoginHandlers.PENDING);
            throw new AccountException(IdentityErrorCode.INVALID_LINK_PASSWORD);
        }
        var account = google.confirmLink(pending.identity(), UUID.fromString(user.getUsername()));
        var authentication = UsernamePasswordAuthenticationToken.authenticated(account.getId().toString(), null,
                account.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList());
        new ChangeSessionIdAuthenticationStrategy().onAuthentication(authentication, request, response);
        new CsrfAuthenticationStrategy(new HttpSessionCsrfTokenRepository()).onAuthentication(authentication, request, response);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        session.removeAttribute(GoogleLoginHandlers.PENDING);
        tokens.issue(account.getId(), response);

        return vn.camerahub.shared.dto.ApiResponse.success(200, "Đã liên kết tài khoản Google.", null);
    }

    private GoogleAccountService.PendingLink pending(HttpServletRequest request) {
        var session = request.getSession(false);
        if (session == null) return null;
        var value = session.getAttribute(GoogleLoginHandlers.PENDING);
        if (!(value instanceof GoogleAccountService.PendingLink pending)) return null;
        if (!pending.expiresAt().isAfter(Instant.now()) || pending.attempts() >= 5) {
            session.removeAttribute(GoogleLoginHandlers.PENDING);
            return null;
        }
        return pending;
    }

    public record Status(boolean enabled, String pendingEmail, String message) {
    }

    public record Confirmation(@NotBlank @Size(max = 72) String password) {
    }
}

