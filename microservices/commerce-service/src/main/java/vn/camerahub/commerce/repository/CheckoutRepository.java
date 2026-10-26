package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.commerce.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckoutRepository extends JpaRepository<CheckoutBatch, UUID>, CheckoutRepositoryOperations {
}
