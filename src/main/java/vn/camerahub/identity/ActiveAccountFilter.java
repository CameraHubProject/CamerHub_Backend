package vn.camerahub.identity;

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

/** Recheck account status and roles on every request, including already logged-in sessions. */
public final class ActiveAccountFilter extends OncePerRequestFilter {
    private final AccountService accounts;
    public ActiveAccountFilter(AccountService accounts) { this.accounts = accounts; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/api/auth/") || request.getRequestURI().startsWith("/api/seller-verification")
                || request.getRequestURI().startsWith("/api/admin/")) response.setHeader("Cache-Control", "no-store");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            try {
                var account = accounts.get(UUID.fromString(authentication.getName()));
                if (!"ACTIVE".equals(account.status())) throw new UsernameNotFoundException("Inactive account");
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(new UsernamePasswordAuthenticationToken(account.id().toString(), null,
                        account.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList()));
                SecurityContextHolder.setContext(context);
            } catch (UsernameNotFoundException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                var session = request.getSession(false);
                if (session != null) session.invalidate();
                response.setStatus(401);
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
