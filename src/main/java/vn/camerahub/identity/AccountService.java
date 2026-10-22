package vn.camerahub.identity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;

    public AccountService(JdbcTemplate jdbc, PasswordEncoder passwords) {
        this.jdbc = jdbc;
        this.passwords = passwords;
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) {
        if (identifier == null || identifier.length() > 254) throw new UsernameNotFoundException("Invalid account");
        boolean byEmail = identifier.contains("@");
        String key = byEmail ? normalizeEmail(identifier) : PhoneNumber.normalize(identifier);
        if (key == null) throw new UsernameNotFoundException("Invalid account");
        var users = jdbc.query("SELECT id,password_hash,account_status FROM camerahub.users WHERE "
                + (byEmail ? "lower(email)=?" : "camerahub.normalized_login_phone(phone)=? AND camerahub.normalized_login_phone(phone) IS NOT NULL"),
                (row, index) -> new Login(row.getObject("id", UUID.class), row.getString("password_hash"), row.getString("account_status")),
                key);
        if (users.size() != 1) throw new UsernameNotFoundException("Invalid account");
        var user = users.getFirst();
        return User.withUsername(user.id().toString()).password(user.hash())
                .disabled(!"ACTIVE".equals(user.status())).roles(roles(user.id()).toArray(String[]::new)).build();
    }

    @Transactional
    public Account register(String email, String password, String fullName, String phone) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu tối đa 72 byte UTF-8.");
        }
        String canonicalPhone = PhoneNumber.normalize(phone);
        if (phone != null && !phone.isBlank() && canonicalPhone == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại di động Việt Nam chưa đúng. Ví dụ: 0912 345 678 hoặc +84 912 345 678.");
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO camerahub.users(id,email,password_hash,full_name,phone) VALUES (?,?,?,?,?)",
                id, normalizeEmail(email), passwords.encode(password), fullName.strip(), canonicalPhone);
        jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'BUYER')", id);
        audit(id, id, "ACCOUNT_REGISTERED");
        return get(id);
    }

    public Account get(UUID id) {
        var users = jdbc.query("SELECT id,email,full_name,phone,account_status FROM camerahub.users WHERE id=?",
                (row, index) -> new Account(row.getObject("id", UUID.class), row.getString("email"), row.getString("full_name"),
                        row.getString("phone"), row.getString("account_status"), List.of()), id);
        if (users.isEmpty()) throw new UsernameNotFoundException("Invalid account");
        Account user = users.getFirst();
        return new Account(user.id(), user.email(), user.fullName(), user.phone(), user.status(), roles(id));
    }

    private List<String> roles(UUID id) {
        // SQL keeps BUYER; API exposes USER. SELLER requires an approved verification.
        return jdbc.queryForList("""
                SELECT CASE WHEN role_code='BUYER' THEN 'USER' ELSE role_code END
                FROM camerahub.user_roles r WHERE user_id=? AND
                (role_code<>'SELLER' OR EXISTS (SELECT 1 FROM camerahub.seller_verifications v
                  WHERE v.seller_id=r.user_id AND v.status='VERIFIED')) ORDER BY role_code
                """, String.class, id);
    }

    void audit(UUID actor, UUID entity, String action) {
        jdbc.update("INSERT INTO camerahub.audit_logs(actor_id,entity_type,entity_id,action) VALUES (?,'IDENTITY',?,?)", actor, entity, action);
    }

    static String normalizeEmail(String email) { return email.strip().toLowerCase(Locale.ROOT); }
    private record Login(UUID id, String hash, String status) {}
    public record Account(UUID id, String email, String fullName, String phone, String status, List<String> roles) {}
}
