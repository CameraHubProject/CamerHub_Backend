package vn.camerahub.identity.config;

import vn.camerahub.identity.service.AccountService;
import vn.camerahub.identity.entity.*;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Recheck account status and roles on every request, including already logged-in sessions.
 */
@lombok.RequiredArgsConstructor
public final class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountService accounts;
    private final vn.camerahub.shared.exception.ApiExceptionWriter errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/auth/") || request.getRequestURI().startsWith("/api/seller-verification")
                || request.getRequestURI().startsWith("/api/admin/")) response.setHeader("Cache-Control", "no-store");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                var account = accounts.get(UUID.fromString(authentication.getName()));
                if (!"ACTIVE".equals(account.getAccountStatus()))
                    throw new UsernameNotFoundException("Inactive account");
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(account.getId().toString(), null,
                        account.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()));
                SecurityContextHolder.setContext(context);
            } catch (UsernameNotFoundException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                errors.write(response, vn.camerahub.shared.exception.CommonErrorCode.AUTHENTICATION_REQUIRED);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}

