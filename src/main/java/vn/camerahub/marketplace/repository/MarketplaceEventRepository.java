package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketplaceEventRepository extends JpaRepository<Product, UUID>, MarketplaceEventRepositoryOperations {
}
