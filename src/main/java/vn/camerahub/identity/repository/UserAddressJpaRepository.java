package vn.camerahub.identity.repository;

import vn.camerahub.identity.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressJpaRepository extends JpaRepository<UserAddress, UUID> {
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(UUID userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("update UserAddress a set a.isDefault=false where a.userId=:id")
    int clearDefaults(@org.springframework.data.repository.query.Param("id") UUID id);
}
