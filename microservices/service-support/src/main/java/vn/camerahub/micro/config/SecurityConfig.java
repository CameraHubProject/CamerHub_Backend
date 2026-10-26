package vn.camerahub.micro.config;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import vn.camerahub.micro.InternalClient;
import vn.camerahub.micro.exception.ApiExceptionWriter;

@Configuration
@ConditionalOnProperty(name = "camerahub.security.mode", havingValue = "downstream", matchIfMissing = true)
public class SecurityConfig {
    @Bean
    SecurityFilterChain downstreamSecurityChain(HttpSecurity http, JwtUtil jwt, InternalClient client, ApiExceptionWriter errors, @Value("${camerahub.internal.key}") String key) throws Exception {
        return http.csrf(csrf -> csrf.disable()).sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).requestCache(c -> c.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/health", "/actuator/health", "/api/docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().requestMatchers("/internal/**").hasRole("SERVICE")
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/api/ai/status", "/api/ai/conversation").permitAll().requestMatchers(HttpMethod.POST, "/api/ai/chat", "/api/ai/conversation/reset").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/marketplace/**").authenticated().anyRequest().denyAll())
                .exceptionHandling(e -> e.authenticationEntryPoint((q, r, x) -> errors.write(r, vn.camerahub.micro.exception.CommonErrorCode.AUTHENTICATION_REQUIRED)).accessDeniedHandler((q, r, x) -> errors.write(r, vn.camerahub.micro.exception.CommonErrorCode.ACCESS_DENIED)))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, client, null, errors, key, false), AnonymousAuthenticationFilter.class).build();
    }
}
