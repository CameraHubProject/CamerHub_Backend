package vn.camerahub.micro.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;
import vn.camerahub.micro.*;
import vn.camerahub.micro.exception.ApiExceptionWriter;

/**
 * Verifies the RSA signature locally, then current account roles and token revocation at Identity.
 */
@lombok.RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final InternalClient client;
    private final TokenStatusVerifier local;
    private final ApiExceptionWriter errors;
    private final String key;
    private final boolean identity;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean internal = path.startsWith("/internal/");
        try {
            if (internal && !identity) {
                if (!InternalKeyFilter.matches(request.getHeader("X-CameraHub-Key"), key.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    errors.write(response, vn.camerahub.micro.exception.CommonErrorCode.INTERNAL_ACCESS_DENIED);
                    return;
                }
                authenticate("service", List.of("SERVICE"));
            } else if (path.startsWith("/api/") || path.equals("/internal/token")) {
                // Login and refresh must work even when the previous access token has expired.
                boolean authFlow = Set.of("/api/auth/login", "/api/auth/register", "/api/auth/csrf", "/api/auth/refresh", "/api/auth/logout", "/api/auth/google/status", "/api/auth/google/link", "/api/auth/google/callback").contains(path) || path.startsWith("/api/auth/oauth2/");
                if (!authFlow) {
                    String raw = JwtUtil.token(request);
                    if (raw != null) {
                        var token = jwt.decode(raw);
                        List<String> roles;
                        if (identity) roles = local.verify(token);
                        else {
                            var state = client.call("identity", "/internal/token", "GET", null, Map.of("Authorization", "Bearer " + raw));
                            roles = new ArrayList<>();
                            state.path("roles").forEach(value -> roles.add(value.asText()));
                        }
                        authenticate(token.getSubject(), roles);
                        request.setAttribute("camerahub.jwt", token);
                    } else if (identity) SecurityContextHolder.clearContext();
                }
                if (!identity && !Set.of("GET", "HEAD", "OPTIONS").contains(request.getMethod()) && !JwtUtil.bearer(request)) {
                    var headers = new HashMap<String, String>();
                    headers.put("Cookie", Objects.toString(request.getHeader("Cookie"), ""));
                    String csrf = request.getHeader("X-CSRF-TOKEN");
                    if (csrf != null) headers.put("X-CSRF-TOKEN", csrf);
                    client.call("identity", "/internal/session", "POST", null, headers);
                }
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            errors.write(response, vn.camerahub.micro.exception.CommonErrorCode.TOKEN_INVALID);
        } catch (ResponseStatusException e) {
            errors.write(response, e.getStatusCode().value(), Objects.toString(e.getReason(), "Dịch vụ xác thực chưa sẵn sàng."));
        } catch (vn.camerahub.micro.exception.AppException e) {
            errors.write(response, e);
        }
    }

    private void authenticate(String id, List<String> roles) {
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(id, null, roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()));
        SecurityContextHolder.setContext(context);
    }
}
