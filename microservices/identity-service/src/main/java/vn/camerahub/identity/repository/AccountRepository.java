package vn.camerahub.identity.repository;

import java.util.*;

import vn.camerahub.identity.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByEmailIgnoreCase(String email);

    @org.springframework.data.jpa.repository.Query(value = "SELECT * FROM camerahub.users WHERE camerahub.normalized_login_phone(phone)=:phone", nativeQuery = true)
    Optional<Account> findByLoginPhone(@org.springframework.data.repository.query.Param("phone") String phone);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.id=:id")
    Optional<Account> lockById(@org.springframework.data.repository.query.Param("id") UUID id);
}
