package vn.camerahub.micro.config;

import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.*;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.*;

@Component
public class JwtUtil {
    public static final String ACCESS_COOKIE = "CAMERAHUB_ACCESS";
    private final JwtDecoder decoder;

    public JwtUtil(@Value("${camerahub.jwt.public-key}") String path, @Value("${camerahub.jwt.issuer:camerahub-identity}") String issuer) throws Exception {
        var encoded = Files.readString(Path.of(path)).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        var key = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(encoded)));
        var jwt = NimbusJwtDecoder.withPublicKey(key).build();
        var audience = new JwtClaimValidator<List<String>>("aud", values -> values != null && values.contains("camerahub-api"));
        var type = new JwtClaimValidator<String>("token_use", "access"::equals);
        jwt.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer), audience, type));
        this.decoder = jwt;
    }

    public Jwt decode(String token) {
        var jwt = decoder.decode(token);
        UUID.fromString(jwt.getSubject());
        UUID.fromString(jwt.getId());
        if (jwt.getExpiresAt() == null || jwt.getIssuedAt() == null) throw new BadJwtException("Missing token dates");
        return jwt;
    }

    public static String token(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null) {
            if (!authorization.startsWith("Bearer ") || authorization.length() < 8)
                throw new BadJwtException("Invalid Authorization header");
            return authorization.substring(7);
        }
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies())
            if (ACCESS_COOKIE.equals(cookie.getName())) return cookie.getValue();
        return null;
    }

    public static boolean bearer(HttpServletRequest request) {
        return request.getHeader("Authorization") != null;
    }
}
