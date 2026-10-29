package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ProductMediaRepositoryOperationsImpl implements ProductMediaRepositoryOperations {
    private final JpaQueries queries;

    public List<Map<String, Object>> findProducts(Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.products WHERE id=? AND seller_id=? AND deleted_at IS NULL AND listing_status NOT IN ('RESERVED','SOLD') FOR UPDATE", args);
    }

    public <T> List<T> findProductMedia(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.product_media WHERE product_id=? AND image_sha256=?", type, args);
    }

    public <T> T findProductMedia2(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.product_media WHERE product_id=?", type, args);
    }

    public int insertProductMedia(Object... args) {
        return queries.update("INSERT INTO camerahub.product_media(id,product_id,storage_key,media_type,sort_order,image_sha256,image_dhash,image_signature) VALUES (?,?,?,'IMAGE',(SELECT count(*) FROM camerahub.product_media WHERE product_id=?),?,?,?)", args);
    }

    public List<Map<String, Object>> findProductMedia3(Object... args) {
        return queries.queryForList("""
                SELECT pm.storage_key FROM camerahub.product_media pm JOIN camerahub.products p ON p.id=pm.product_id
                WHERE pm.id=? AND p.deleted_at IS NULL AND ((p.listing_status IN ('PUBLISHED','SOLD') AND EXISTS
                  (SELECT 1 FROM camerahub.public_accounts u WHERE u.id=p.seller_id AND u.account_status='ACTIVE')) OR p.seller_id::text=? OR ?)
                """, args);
    }

    public <T> List<T> findProductMedia4(Class<T> type, Object... args) {
        return queries.queryForList("SELECT storage_key FROM camerahub.product_media WHERE id=?", type, args);
    }

    public List<Map<String, Object>> findProducts2(Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.products WHERE id=? AND seller_id=? AND deleted_at IS NULL AND listing_status NOT IN ('RESERVED','SOLD') FOR UPDATE", args);
    }

    public int markMediaEdited(Object... args) {
        return queries.update("""
                UPDATE camerahub.products SET listing_status=CASE WHEN listing_status IN ('DRAFT','HIDDEN') THEN listing_status ELSE 'NEEDS_REVISION' END,
                updated_at=now(),version=version+1,submitted_revision=submitted_revision+1
                WHERE id=? AND seller_id=? AND deleted_at IS NULL AND listing_status NOT IN ('RESERVED','SOLD')
                """, args);
    }

    public int deleteProductMedia(Object... args) {
        return queries.update("DELETE FROM camerahub.product_media WHERE id=? AND product_id=?", args);
    }
}

