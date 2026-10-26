package vn.camerahub.identity.repository;

import java.util.*;

import vn.camerahub.identity.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleAccountRepository extends JpaRepository<AuthIdentity, AuthIdentityId>, GoogleAccountRepositoryOperations {
}
