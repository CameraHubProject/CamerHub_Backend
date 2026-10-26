package vn.camerahub.micro.config;

import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import org.springframework.security.oauth2.jwt.JwtException;

import static org.assertj.core.api.Assertions.*;

class JwtUtilTest {
    @TempDir
    Path directory;
    private KeyPair pair;
    private JwtUtil util;

    @BeforeEach
    void setup() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        pair = generator.generateKeyPair();
        Path key = directory.resolve("public.pem");
        Files.writeString(key, "-----BEGIN PUBLIC KEY-----\n" + Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----");
        util = new JwtUtil(key.toString(), "issuer");
    }

    private String token(String issuer, String audience, Instant expiry, String use) throws Exception {
        var claims = new JWTClaimsSet.Builder().issuer(issuer).audience(audience).subject(UUID.randomUUID().toString()).jwtID(UUID.randomUUID().toString()).issueTime(Date.from(Instant.now().minusSeconds(60))).expirationTime(Date.from(expiry)).claim("token_use", use).build();
        var signed = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        signed.sign(new RSASSASigner(pair.getPrivate()));
        return signed.serialize();
    }

    @Test
    void validatesIssuerAudienceExpiryAndTokenUse() throws Exception {
        assertThat(util.decode(token("issuer", "camerahub-api", Instant.now().plusSeconds(300), "access")).getSubject()).isNotEmpty();
        for (var invalid : List.of(token("wrong", "camerahub-api", Instant.now().plusSeconds(300), "access"), token("issuer", "other", Instant.now().plusSeconds(300), "access"), token("issuer", "camerahub-api", Instant.now().minusSeconds(300), "access"), token("issuer", "camerahub-api", Instant.now().plusSeconds(300), "refresh")))
            assertThatThrownBy(() -> util.decode(invalid)).isInstanceOf(JwtException.class);
    }

    @Test
    void invalidSignatureCannotAuthenticate() throws Exception {
        String valid = token("issuer", "camerahub-api", Instant.now().plusSeconds(300), "access");
        String[] parts = valid.split("\\.");
        parts[2] = (parts[2].charAt(0) == 'a' ? "b" : "a") + parts[2].substring(1);
        assertThatThrownBy(() -> util.decode(String.join(".", parts))).isInstanceOf(JwtException.class);
    }
}
