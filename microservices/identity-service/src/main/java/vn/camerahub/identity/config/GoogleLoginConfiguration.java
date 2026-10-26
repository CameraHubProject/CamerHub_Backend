package vn.camerahub.identity.config;

import vn.camerahub.identity.entity.*;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
public class GoogleLoginConfiguration {
    @Bean
    @ConditionalOnProperty(name = "camerahub.google.enabled", havingValue = "true")
    ClientRegistrationRepository googleClients(@Value("${camerahub.google.client-id}") String id,
                                               @Value("${camerahub.google.client-secret}") String secret,
                                               @Value("${camerahub.google.redirect-uri}") String redirect) {
        if (id.isBlank() || secret.isBlank())
            throw new IllegalStateException("Configure Google client ID and secret in backend environment.");
        return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(id).clientSecret(secret).scope("openid", "email", "profile").redirectUri(redirect).build());
    }

    @Bean
    public FrontendLocation frontendLocation(@Value("${camerahub.frontend-url}") String origin) {
        URI uri = URI.create(origin);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null || uri.getFragment() != null || !(uri.getPath().isEmpty() || uri.getPath().equals("/")))
            throw new IllegalStateException("FRONTEND_URL must be an HTTP(S) origin without path, query or credentials.");
        return new FrontendLocation(origin.replaceAll("/+$", ""));
    }

    public record FrontendLocation(String origin) {
    }
}

