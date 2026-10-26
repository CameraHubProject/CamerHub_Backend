package vn.camerahub.identity.repository;

import vn.camerahub.identity.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleJpaRepository extends JpaRepository<Role, String> {
}
