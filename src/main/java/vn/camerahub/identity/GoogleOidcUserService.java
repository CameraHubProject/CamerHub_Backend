package vn.camerahub.identity;

import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class GoogleOidcUserService extends OidcUserService {
    private final GoogleAccountService accounts;
    public GoogleOidcUserService(GoogleAccountService accounts) { this.accounts = accounts; }

    @Override
    public OidcUser loadUser(OidcUserRequest request) {
        var google = super.loadUser(request);
        var token = google.getIdToken();
        var identity = GoogleAccountService.verified(token.getSubject(), token.getEmail(), token.getEmailVerified(), token.getFullName());
        AccountService.Account account;
        try { account = accounts.resolve(identity); }
        catch (org.springframework.dao.DataAccessException exception) { throw GoogleAccountService.failure("account_unavailable"); }
        return new LocalUser(google, account, identity);
    }

    public static class LocalUser extends DefaultOidcUser {
        private final AccountService.Account account;
        private final GoogleAccountService.Identity identity;
        public LocalUser(OidcUser google, AccountService.Account account, GoogleAccountService.Identity identity) {
            super(authorities(account), google.getIdToken(), google.getUserInfo());
            this.account = account; this.identity = identity;
        }
        private static Collection<? extends GrantedAuthority> authorities(AccountService.Account account) {
            return account == null ? List.of(new SimpleGrantedAuthority("GOOGLE_LINK_PENDING"))
                    : account.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
        }
        @Override public String getName() { return account == null ? "google-link-pending" : account.id().toString(); }
        public AccountService.Account account() { return account; }
        public GoogleAccountService.Identity identity() { return identity; }
    }
}
