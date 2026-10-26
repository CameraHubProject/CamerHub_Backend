package vn.camerahub.micro.config;

import java.util.List;

import org.springframework.security.oauth2.jwt.Jwt;

public interface TokenStatusVerifier {
    List<String> verify(Jwt jwt);
}
