package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.commerce.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID>, OrderRepositoryOperations {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.id=:id and o.buyerId=:buyer")
    List<Order> lockBuyerOrder(@org.springframework.data.repository.query.Param("id") UUID id, @org.springframework.data.repository.query.Param("buyer") UUID buyer);
}
