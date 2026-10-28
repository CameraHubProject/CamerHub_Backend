package vn.camerahub.identity.service;

import vn.camerahub.identity.exception.*;

import java.security.*;
import java.time.*;
import java.util.*;

import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.camerahub.identity.entity.JwtSession;
import vn.camerahub.identity.repository.JwtSessionRepository;
import vn.camerahub.shared.config.*;

@Service
public class AuthTokenService implements TokenStatusVerifier {
    private static final String REFRESH = "CAMERAHUB_REFRESH";
    private final JwtSessionRepository sessions;
    private final AccountService accounts;
    private final JwtEncoder encoder;
    private final JwtUtil jwt;
    private final Clock clock;
    private final boolean secure;
    private final String issuer;

    public AuthTokenService(JwtSessionRepository sessions, AccountService accounts, JwtEncoder encoder, JwtUtil jwt, Clock clock, @Value("${server.servlet.session.cookie.secure:false}") boolean secure, @Value("${camerahub.jwt.issuer:camerahub-identity}") String issuer) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
        this.secure = secure;
        this.issuer = issuer;
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
    }

    @Transactional
    public TokenResponse issue(UUID user, HttpServletResponse response) {
        var account = accounts.get(user);
        if (!"ACTIVE".equals(account.getAccountStatus())) throw invalid();
        byte[] random = new byte[48];
        new SecureRandom().nextBytes(random);
        String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        UUID id = UUID.randomUUID();
        Instant now = clock.instant();
        sessions.save(new JwtSession(id, user, hash(refresh), now.plus(Duration.ofDays(7)), now));
        var claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of("camerahub-api")).subject(user.toString()).id(id.toString()).issuedAt(now).expiresAt(now.plusSeconds(900)).claim("roles", account.getRoles()).claim("token_use", "access").build();
        String access = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).keyId("camerahub-local-v1").build(), claims)).getTokenValue();
        cookie(response, JwtUtil.ACCESS_COOKIE, access, "/", 900);
        cookie(response, REFRESH, refresh, "/api/auth", 604800);
        return new TokenResponse(access, "Bearer", 900);
    }

    @Transactional
    public TokenResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String raw = cookie(request, REFRESH);
        if (raw == null) throw invalid();
        var old = sessions.lockByRefreshHash(hash(raw)).orElseThrow(AuthTokenService::invalid);
        if (old.isRevoked() || !old.getExpiresAt().isAfter(clock.instant())) throw invalid();
        old.setRevoked(true);
        return issue(old.getUserId(), response);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> verify(Jwt token) {
        var session = sessions.findById(UUID.fromString(token.getId())).orElseThrow(AuthTokenService::invalid);
        if (session.isRevoked() || !session.getExpiresAt().isAfter(clock.instant()) || !session.getUserId().toString().equals(token.getSubject()))
            throw invalid();
        var account = accounts.get(session.getUserId());
        if (!"ACTIVE".equals(account.getAccountStatus())) throw invalid();
        return account.getRoles();
    }

    @Transactional
    public void revoke(HttpServletRequest request, HttpServletResponse response) {
        String refresh = cookie(request, REFRESH);
        if (refresh != null) sessions.lockByRefreshHash(hash(refresh)).ifPresent(session -> session.setRevoked(true));
        String access = JwtUtil.token(request);
        if (access != null) try {
            sessions.findById(UUID.fromString(jwt.decode(access).getId())).ifPresent(session -> session.setRevoked(true));
        } catch (JwtException | IllegalArgumentException ignored) {
        }
        cookie(response, JwtUtil.ACCESS_COOKIE, "", "/", 0);
        cookie(response, REFRESH, "", "/api/auth", 0);
    }

    private void cookie(HttpServletResponse response, String name, String value, String path, long maxAge) {
        response.addHeader("Set-Cookie", ResponseCookie.from(name, value).path(path).httpOnly(true).secure(secure).sameSite("Lax").maxAge(maxAge).build().toString());
    }

    private String cookie(HttpServletRequest request, String name) {
        if (request.getCookies() != null)
            for (var cookie : request.getCookies()) if (name.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static AccountException invalid() {
        return new AccountException(IdentityErrorCode.SESSION_REVOKED);
    }
}
