package vn.camerahub.identity.config;

import java.nio.file.*;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.*;
import java.util.*;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

@Configuration
public class JwtIssuerConfig {
    @Bean
    public JwtEncoder jwtEncoder(@Value("${camerahub.jwt.private-key}") String privatePath, @Value("${camerahub.jwt.public-key}") String publicPath) throws Exception {
        var factory = KeyFactory.getInstance("RSA");
        var privateKey = (RSAPrivateKey) factory.generatePrivate(new PKCS8EncodedKeySpec(pem(privatePath)));
        var publicKey = (RSAPublicKey) factory.generatePublic(new X509EncodedKeySpec(pem(publicPath)));
        var key = new com.nimbusds.jose.jwk.RSAKey.Builder(publicKey).privateKey(privateKey).keyID("camerahub-local-v1").build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }

    private byte[] pem(String path) throws Exception {
        return Base64.getDecoder().decode(Files.readString(Path.of(path)).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", ""));
    }
}

