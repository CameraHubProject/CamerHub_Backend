package vn.camerahub.identity.config;

import java.util.UUID;

import org.springframework.context.annotation.*;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import vn.camerahub.identity.service.*;
import vn.camerahub.shared.*;
import vn.camerahub.shared.config.*;
import vn.camerahub.shared.dto.ApiResponse;
import vn.camerahub.shared.exception.ApiExceptionWriter;

@Configuration("identitySecurityConfig")
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    SecurityFilterChain identitySecurityChain(HttpSecurity http, AccountService accounts, ObjectProvider<ClientRegistrationRepository> clients, GoogleOidcUserService googleUsers, GoogleLoginHandlers googleHandlers, AuthTokenService tokens, JwtUtil jwt, ApiExceptionWriter errors, ObjectMapper json) throws Exception {
        if (clients.getIfAvailable() != null)
            http.oauth2Login(google -> google.authorizationEndpoint(e -> e.baseUri("/api/auth/oauth2")).redirectionEndpoint(e -> e.baseUri("/api/auth/google/callback")).userInfoEndpoint(e -> e.oidcUserService(googleUsers)).authorizedClientRepository(new DiscardingGoogleClientRepository()).successHandler(googleHandlers).failureHandler(googleHandlers));
        return http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)).requestCache(c -> c.disable())
                .csrf(csrf -> csrf.ignoringRequestMatchers(request -> JwtUtil.bearer(request)))
                .authorizeHttpRequests(auth -> auth.requestMatchers(HttpMethod.GET, "/api/auth/csrf", "/api/auth/google/status", "/api/auth/oauth2/google", "/api/auth/google/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/google/link").permitAll()
                        .requestMatchers("/api/health", "/actuator/health", "/api/docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().requestMatchers("/internal/**").denyAll().requestMatchers(HttpMethod.GET, "/api/public/**", "/api/ai/status", "/api/ai/conversation").permitAll().requestMatchers(HttpMethod.POST, "/api/ai/chat", "/api/ai/conversation/reset").permitAll().requestMatchers("/api/admin/**").hasRole("ADMIN").requestMatchers("/api/auth/me", "/api/auth/me/**", "/api/marketplace/**").authenticated().anyRequest().denyAll())
                .formLogin(form -> form.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                        .successHandler((request, response, authentication) -> {
                            var token = tokens.issue(UUID.fromString(authentication.getName()), response);
                            response.setStatus(200);
                            response.setContentType("application/json;charset=UTF-8");
                            json.writeValue(response.getOutputStream(), ApiResponse.success(200, "Đăng nhập thành công.", token));
                        })
                        .failureHandler((request, response, exception) -> errors.write(response, vn.camerahub.shared.exception.CommonErrorCode.INVALID_CREDENTIALS)))
                .logout(logout -> logout.logoutUrl("/api/auth/logout").addLogoutHandler((request, response, auth) -> tokens.revoke(request, response)).deleteCookies("CAMERAHUB_SESSION").logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json;charset=UTF-8");
                    json.writeValue(response.getOutputStream(), ApiResponse.success(200, "Đăng xuất thành công.", null));
                }))
                .exceptionHandling(e -> e.authenticationEntryPoint((q, r, x) -> errors.write(r, vn.camerahub.shared.exception.CommonErrorCode.AUTHENTICATION_REQUIRED)).accessDeniedHandler((q, r, x) -> errors.write(r, vn.camerahub.shared.exception.CommonErrorCode.CSRF_INVALID)))
                .addFilterBefore(new JwtAuthenticationFilter(jwt, tokens, errors), AnonymousAuthenticationFilter.class)
                .addFilterBefore(new ActiveAccountFilter(accounts, errors), AuthorizationFilter.class).build();
    }
}
