package vn.camerahub.identity.controller;

import vn.camerahub.identity.exception.*;

import vn.camerahub.identity.service.AccountService;
import vn.camerahub.identity.entity.*;

import java.security.Principal;
import java.util.*;

import org.springframework.web.bind.annotation.*;

@RestController
@lombok.RequiredArgsConstructor
public class InternalIdentityController {
    private final AccountService accounts;
    @org.springframework.beans.factory.annotation.Autowired
    private vn.camerahub.identity.service.AuthTokenService tokens;

    @RequestMapping(value = "/internal/session", method = {RequestMethod.GET, RequestMethod.POST})
    public Object session(Principal principal) {
        if (principal == null) return Map.of("authenticated", false);
        var account = accounts.get(UUID.fromString(principal.getName()));
        return Map.of("authenticated", true, "id", account.getId(), "roles", account.getRoles(), "status", account.getAccountStatus());
    }

    @GetMapping("/internal/token")
    public Object token(jakarta.servlet.http.HttpServletRequest request) {
        var jwt = (org.springframework.security.oauth2.jwt.Jwt) request.getAttribute("camerahub.jwt");
        if (jwt == null)
            throw new AccountException(IdentityErrorCode.JWT_INVALID);
        return Map.of("id", jwt.getSubject(), "roles", tokens.verify(jwt));
    }

    @GetMapping("/internal/accounts/{id}")
    public Object account(@PathVariable UUID id) {
        return accounts.publicData(id);
    }
}

