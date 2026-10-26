package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.micro.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ListingRepositoryOperationsImpl implements ListingRepositoryOperations {
    private final JpaQueries queries;

    public int upsertPublicAccount(Object... args) {
        return queries.update("""
                INSERT INTO camerahub.public_accounts(id,full_name,account_status,created_at,profile_version) VALUES (?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET full_name=EXCLUDED.full_name,account_status=EXCLUDED.account_status,profile_version=EXCLUDED.profile_version
                WHERE camerahub.public_accounts.profile_version<=EXCLUDED.profile_version
                """, args);
    }

    public List<Map<String, Object>> lockSubmission(Object... args) {
        return queries.queryForList("SELECT listing_status FROM camerahub.products WHERE id=? AND seller_id=? FOR UPDATE", args);
    }

    public <T> T countImages(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.product_media WHERE product_id=?", type, args);
    }

    public int markAiReview(Object... args) {
        return queries.update("UPDATE camerahub.products SET listing_status='AI_REVIEW',updated_at=now(),version=version+1 WHERE id=?", args);
    }

    public int insertProductStatusHistory(Object... args) {
        return queries.update("INSERT INTO camerahub.product_status_history(product_id,old_status,new_status,actor_id,reason) VALUES (?,?,'AI_REVIEW',?,'Gửi sang dịch vụ AI duyệt tin')", args);
    }

    public int advanceSubmittedRevision(Object... args) {
        return queries.update("UPDATE camerahub.products SET submitted_revision=submitted_revision+1,moderation_note='AI đang kiểm tra',moderation_source=NULL,reviewed_by=NULL,reviewed_at=NULL WHERE id=?", args);
    }

    public Map<String, Object> findReviewSnapshot(Object... args) {
        var item = queries.queryForMap("""
                SELECT p.id,p.seller_id AS "sellerId",p.title,p.description,p.condition_notes AS "conditionNotes",p.accessories,
                p.price_vnd::text AS "priceVnd",p.created_at AS "createdAt",p.submitted_revision AS revision,p.listing_status AS "listingStatus",
                p.serial_number_private,b.name AS brand,m.device_type AS "deviceType"
                FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id JOIN camerahub.brands b ON b.id=m.brand_id WHERE p.id=?
                """, args);
        item.put("createdAt", ((java.sql.Timestamp) item.get("createdAt")).toInstant().toString());
        return item;
    }

    public List<Map<String, Object>> findUnfingerprintedImages(Object... args) {
        return queries.queryForList("SELECT id,storage_key FROM camerahub.product_media WHERE product_id=? AND image_sha256 IS NULL", args);
    }

    public int saveFingerprint(Object... args) {
        return queries.update("UPDATE camerahub.product_media SET image_sha256=?,image_dhash=?,image_signature=? WHERE id=?", args);
    }

    public List<Map<String, Object>> findFingerprints(Object... args) {
        return queries.queryForList("SELECT id,image_sha256 AS sha256,image_dhash AS dhash,image_signature AS signature FROM camerahub.product_media WHERE product_id=? ORDER BY sort_order,created_at", args);
    }
}

