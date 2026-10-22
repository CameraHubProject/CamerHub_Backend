package vn.camerahub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import vn.camerahub.identity.AccountService;
import vn.camerahub.identity.ActiveAccountFilter;
import vn.camerahub.identity.GoogleOidcUserService;
import vn.camerahub.identity.GoogleLoginHandlers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, AccountService accounts,
            ObjectProvider<ClientRegistrationRepository> googleClients, GoogleOidcUserService googleUsers, GoogleLoginHandlers googleHandlers) throws Exception {
        if (googleClients.getIfAvailable() != null) {
            http.oauth2Login(google -> google
                    .authorizationEndpoint(endpoint -> endpoint.baseUri("/api/auth/oauth2"))
                    .redirectionEndpoint(endpoint -> endpoint.baseUri("/api/auth/google/callback"))
                    .userInfoEndpoint(info -> info.oidcUserService(googleUsers))
                    .authorizedClientRepository(new vn.camerahub.identity.DiscardingGoogleClientRepository())
                    .successHandler(googleHandlers).failureHandler(googleHandlers));
        }
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/api/public/**", "/api/auth/csrf", "/api/auth/google/status", "/api/auth/oauth2/google", "/api/auth/google/callback").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/google/link").permitAll()
                        .requestMatchers("/api/health", "/actuator/health", "/actuator/health/**",
                                "/api/docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/me", "/api/seller-verification", "/api/seller-verification/**", "/api/marketplace/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(form -> form.loginProcessingUrl("/api/auth/login").usernameParameter("email")
                        .successHandler((request, response, authentication) -> response.setStatus(204))
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write("{\"message\":\"Số điện thoại/email hoặc mật khẩu không đúng, hoặc tài khoản không hoạt động.\"}");
                        }))
                .logout(logout -> logout.logoutUrl("/api/auth/logout").deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) -> response.setStatus(204)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> response.setStatus(401))
                        .accessDeniedHandler((request, response, exception) -> response.setStatus(403)))
                .addFilterBefore(new ActiveAccountFilter(accounts), AuthorizationFilter.class)
                .build();
    }
}
