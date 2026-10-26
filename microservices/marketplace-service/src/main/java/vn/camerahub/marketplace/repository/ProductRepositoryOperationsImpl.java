package vn.camerahub.marketplace.repository;

import java.util.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import vn.camerahub.micro.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ProductRepositoryOperationsImpl implements ProductRepositoryOperations {
    private final JpaQueries queries;
    private final ObjectMapper json;
    private final vn.camerahub.marketplace.mapper.CatalogMapper mapper;

    private static final String PRODUCT_SELECT = """
            SELECT p.id,p.title,b.name AS brand,m.model_name,m.device_type,p.price_vnd,p.shutter_count,
            p.condition_notes,p.inspection_badge,p.description,p.accessories,p.repair_history,p.listing_status,
            p.presentation::text,m.specs::text,p.seller_id,seller.full_name AS seller_name,p.moderation_source,p.moderation_note,p.inspection_checks::text
            FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id
            JOIN camerahub.brands b ON b.id=m.brand_id
            JOIN camerahub.public_accounts seller ON seller.id=p.seller_id
            """;

    public List<Map<String, Object>> findPublished(Object... args) {
        return queries.query(PRODUCT_SELECT + " WHERE p.listing_status='PUBLISHED' AND seller.account_status='ACTIVE' ORDER BY p.created_at DESC,p.id", rowMapper(false), args);
    }

    public List<Map<String, Object>> findPublicDetail(Object... args) {
        return queries.query(PRODUCT_SELECT + " WHERE p.id=? AND p.listing_status IN ('PUBLISHED','SOLD') AND seller.account_status='ACTIVE'", rowMapper(false), args);
    }

    public <T> List<T> findImageUrls(Class<T> type, Object... args) {
        return queries.queryForList("SELECT '/api/public/product-media/'||id::text FROM camerahub.product_media WHERE product_id=? AND storage_key LIKE 'product-public/%' ORDER BY sort_order,created_at", type, args);
    }

    public List<Map<String, Object>> findCameraModels(Object... args) {
        return queries.queryForList("SELECT m.id,b.name AS brand,m.model_name AS \"modelName\",m.device_type AS \"deviceType\" FROM camerahub.camera_models m JOIN camerahub.brands b ON b.id=m.brand_id ORDER BY b.name,m.model_name", args);
    }

    public List<Map<String, Object>> findBrands(Object... args) {
        return queries.queryForList("SELECT id,name FROM camerahub.brands WHERE lower(name)<>lower('Khác')", args);
    }

    public List<Map<String, Object>> findSellerListings(Object... args) {
        return queries.query(PRODUCT_SELECT + " WHERE p.seller_id=? ORDER BY p.created_at DESC", rowMapper(true), args);
    }

    public List<Map<String, Object>> findEditableDraft(Object... args) {
        return queries.query(PRODUCT_SELECT + " WHERE p.id=? AND p.seller_id=? AND p.listing_status IN ('DRAFT','NEEDS_REVISION')", rowMapper(true), args);
    }

    public <T> T findModelId(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT model_id FROM camerahub.products WHERE id=?", type, args);
    }

    public <T> T findPrivateSerial(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT serial_number_private FROM camerahub.products WHERE id=?", type, args);
    }

    public List<Map<String, Object>> findDraftMedia(Object... args) {
        return queries.queryForList("SELECT id,'/api/public/product-media/'||id::text AS url FROM camerahub.product_media WHERE product_id=? ORDER BY sort_order,created_at", args);
    }

    public List<Map<String, Object>> lockEditableDraft(Object... args) {
        return queries.queryForList("SELECT listing_status,model_id,inspection_badge FROM camerahub.products WHERE id=? AND seller_id=? AND listing_status IN ('DRAFT','NEEDS_REVISION') FOR UPDATE", args);
    }

    public <T> T countInspectionChecks(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT CASE WHEN inspection_checks<>'[]'::jsonb THEN 1 ELSE 0 END FROM camerahub.products WHERE id=?", type, args);
    }

    public int saveDraft(Object... args) {
        return queries.update("""
                UPDATE camerahub.products SET model_id=?,title=?,price_vnd=?,shutter_count=?,description=?,condition_notes=?,
                accessories=?,serial_number_private=?,presentation=?::jsonb,updated_at=now(),version=version+1 WHERE id=?
                """, args);
    }

    public List<Map<String, Object>> findPublicSellerListings(Object... args) {
        return queries.query(PRODUCT_SELECT + " WHERE p.seller_id=? AND p.listing_status=? AND seller.account_status='ACTIVE' ORDER BY p.updated_at DESC,p.id LIMIT ? OFFSET ?", rowMapper(false), args);
    }

    public int insertDraft(Object... args) {
        return queries.update("""
                INSERT INTO camerahub.products(id,seller_id,model_id,title,price_vnd,shutter_count,description,
                  condition_notes,accessories,serial_number_private,listing_status,presentation)
                VALUES (?,?,?,?,?,?,?,?,?,?,'DRAFT',?::jsonb)
                """, args);
    }

    public int insertProductStatusHistory(Object... args) {
        return queries.update("INSERT INTO camerahub.product_status_history(product_id,new_status,actor_id,reason) VALUES (?,'DRAFT',?,'Người bán lưu nháp')", args);
    }

    public <T> T countModel(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.camera_models WHERE id=?", type, args);
    }

    public <T> T countBrand(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.brands WHERE id=?", type, args);
    }

    public void lockBrandCatalog(Object... args) {
        queries.execute("SELECT pg_advisory_xact_lock(1128353106,1)");
    }

    public <T> List<T> findBrandNames(Class<T> type, Object... args) {
        return queries.queryForList("SELECT name FROM camerahub.brands", type, args);
    }

    public <T> T upsertBrand(Class<T> type, Object... args) {
        return queries.queryForObject("""
                INSERT INTO camerahub.brands(name) VALUES (?)
                ON CONFLICT(name) DO UPDATE SET name=EXCLUDED.name RETURNING id
                """, type, args);
    }

    public <T> T upsertUnspecifiedModel(Class<T> type, Object... args) {
        return queries.queryForObject("""
                INSERT INTO camerahub.camera_models(brand_id,model_name,device_type)
                VALUES (?,'Chưa khai báo model',?)
                ON CONFLICT(brand_id,model_name,device_type) DO UPDATE SET model_name=EXCLUDED.model_name
                RETURNING id
                """, type, args);
    }

    public List<Map<String, Object>> lockBrandSelection(Object... args) {
        return queries.queryForList("""
                SELECT p.listing_status,p.inspection_badge,m.brand_id,m.device_type
                FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id
                WHERE p.id=? AND p.seller_id=? FOR UPDATE OF p
                """, args);
    }

    public <T> T countExistingInspectionChecks(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT CASE WHEN inspection_checks<>'[]'::jsonb THEN 1 ELSE 0 END FROM camerahub.products WHERE id=?", type, args);
    }

    public int changeBrandAndRequeue(Object... args) {
        return queries.update("""
                UPDATE camerahub.products SET model_id=?,listing_status='AI_REVIEW',moderation_note='Đang chờ AI kiểm tra',reviewed_by=NULL,
                reviewed_at=NULL,rejection_reason=NULL,updated_at=now(),version=version+1 WHERE id=? AND seller_id=?
                """, args);
    }

    public int restoreDraftStatus(Object... args) {
        return queries.update("UPDATE camerahub.products SET listing_status=? WHERE id=?", args);
    }

    private JpaRowMapper<Map<String, Object>> rowMapper(boolean moderated) {
        return (row, index) -> {
            var item = moderated ? mapper.moderatedProduct(row, index) : mapper.product(row, index);
            item.put("images", findImageUrls(String.class, row.getObject("id", UUID.class)));
            return item;
        };
    }
}

