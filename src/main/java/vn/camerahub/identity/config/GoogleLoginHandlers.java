package vn.camerahub.identity.config;

import vn.camerahub.identity.service.GoogleAccountService;
import vn.camerahub.identity.service.GoogleOidcUserService;
import vn.camerahub.identity.entity.*;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import vn.camerahub.identity.config.GoogleLoginConfiguration.FrontendLocation;

@Component
@lombok.RequiredArgsConstructor
public class GoogleLoginHandlers implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
    public static final String PENDING = GoogleLoginHandlers.class.getName() + ".PENDING";
    private final FrontendLocation frontend;
    private final vn.camerahub.identity.service.AuthTokenService tokens;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        response.setHeader("Cache-Control", "no-store");
        var user = (GoogleOidcUserService.LocalUser) authentication.getPrincipal();
        var session = request.getSession();
        session.removeAttribute(PENDING);
        if (user.account() == null) {
            var context = SecurityContextHolder.createEmptyContext();
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            session.setAttribute(PENDING, new GoogleAccountService.PendingLink(user.identity(), Instant.now().plusSeconds(300), 0));
            response.sendRedirect(frontend.origin() + "/dang-nhap?google=link_required");
        } else {
            // Store the same minimal local principal as password login, without Google tokens/claims.
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                    user.account().getId().toString(), null, user.getAuthorities()));
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            tokens.issue(user.account().getId(), response);
            response.sendRedirect(frontend.origin() + "/tai-khoan");
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        var session = request.getSession(false);
        if (session != null) session.removeAttribute(PENDING);
        response.setHeader("Cache-Control", "no-store");
        String code = exception instanceof OAuth2AuthenticationException oauth ? oauth.getError().getErrorCode() : "failed";
        String reason = switch (code) {
            case "access_denied" -> "cancelled";
            case "account_inactive" -> "inactive";
            case "unverified_email" -> "unverified";
            default -> "failed";
        };
        response.sendRedirect(frontend.origin() + "/dang-nhap?google=" + reason);
    }
}

