package vn.camerahub.shared.config;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenStatusVerifier {
    List<String> verify(Jwt jwt);
}
