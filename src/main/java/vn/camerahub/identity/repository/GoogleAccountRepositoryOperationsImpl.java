package vn.camerahub.identity.repository;

import vn.camerahub.identity.entity.*;

import java.util.*;

import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class GoogleAccountRepositoryOperationsImpl implements GoogleAccountRepositoryOperations {
    private final JpaQueries queries;

    public <T> List<T> findAuthIdentities(Class<T> type, Object... args) {
        return queries.queryForList("SELECT user_id FROM camerahub.auth_identities WHERE provider='google' AND subject=?", type, args);
    }

    public <T> List<T> findUsers(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.users WHERE lower(email)=?", type, args);
    }

    public int updateUsers(Object... args) {
        return queries.update("UPDATE camerahub.users SET email_verified_at=now() WHERE id=?", args);
    }

    public int insertAuthIdentities(Object... args) {
        return queries.update("INSERT INTO camerahub.auth_identities(provider,subject,user_id) VALUES ('google',?,?)", args);
    }

    public int updateUsers2(Object... args) {
        return queries.update("UPDATE camerahub.users SET email_verified_at=COALESCE(email_verified_at,now()) WHERE id=?", args);
    }

    public int insertAuthIdentities2(Object... args) {
        return queries.update("INSERT INTO camerahub.auth_identities(provider,subject,user_id) VALUES ('google',?,?)", args);
    }
}

