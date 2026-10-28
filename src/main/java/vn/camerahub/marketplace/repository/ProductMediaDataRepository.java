package vn.camerahub.marketplace.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.camerahub.marketplace.entity.ProductMediaData;

public interface ProductMediaDataRepository extends JpaRepository<ProductMediaData, UUID> {
}
