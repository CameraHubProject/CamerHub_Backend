package vn.camerahub.identity.repository;

import java.util.*;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import vn.camerahub.identity.entity.JwtSession;

public interface JwtSessionRepository extends JpaRepository<JwtSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from JwtSession s where s.refreshHash=:hash")
    Optional<JwtSession> lockByRefreshHash(@Param("hash") String hash);
}
