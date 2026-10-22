package vn.camerahub.identity;

import java.security.Principal;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class IdentityController {
    private final AccountService accounts;
    public IdentityController(AccountService accounts) { this.accounts = accounts; }

    @GetMapping("/csrf")
    public Csrf csrf(CsrfToken token) { return new Csrf(token.getHeaderName(), token.getToken()); }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountService.Account register(@Valid @RequestBody Registration registration) {
        return accounts.register(registration.email(), registration.password(), registration.fullName(), registration.phone());
    }

    @GetMapping("/me")
    public AccountService.Account me(Principal principal) { return accounts.get(UUID.fromString(principal.getName())); }

    public record Csrf(String headerName, String token) {}
    public record Registration(@NotBlank @Email @Size(max=254) String email,
            @NotBlank @Size(min=12,max=72) String password, @NotBlank @Size(max=160) String fullName,
            @NotBlank @Pattern(regexp="[+0-9 ().-]{1,20}") String phone) {}
}
