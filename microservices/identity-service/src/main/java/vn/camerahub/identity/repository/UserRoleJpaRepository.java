package vn.camerahub.identity.repository;

import vn.camerahub.identity.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleJpaRepository extends JpaRepository<UserRole, UserRoleId> {
    @org.springframework.data.jpa.repository.Query("select case when r.roleCode='BUYER' then 'USER' else r.roleCode end from UserRole r where r.userId=:id and r.roleCode<>'SELLER' order by r.roleCode")
    List<String> findActiveRoles(@org.springframework.data.repository.query.Param("id") UUID id);
}
