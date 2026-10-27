package vn.camerahub.shared.config;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.camerahub.shared.exception.*;

@lombok.RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final TokenStatusVerifier tokens;
    private final ApiExceptionWriter errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean authFlow = Set.of("/api/auth/login", "/api/auth/register", "/api/auth/csrf", "/api/auth/refresh", "/api/auth/logout", "/api/auth/google/status", "/api/auth/google/link", "/api/auth/google/callback").contains(path) || path.startsWith("/api/auth/oauth2/");
        try {
            if (path.startsWith("/api/") && !authFlow) {
                String raw = JwtUtil.token(request);
                if (raw == null) SecurityContextHolder.clearContext();
                else {
                    var token = jwt.decode(raw);
                    var authorities = tokens.verify(token).stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(new UsernamePasswordAuthenticationToken(token.getSubject(), null, authorities));
                    SecurityContextHolder.setContext(context);
                    request.setAttribute("camerahub.jwt", token);
                }
            }
            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException error) {
            errors.write(response, CommonErrorCode.TOKEN_INVALID);
        } catch (AppException error) {
            errors.write(response, error);
        }
    }
}
