package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<ProductReservation, UUID>, ReservationRepositoryOperations {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id=:id")
    List<Product> lockProduct(@org.springframework.data.repository.query.Param("id") UUID id);
}
