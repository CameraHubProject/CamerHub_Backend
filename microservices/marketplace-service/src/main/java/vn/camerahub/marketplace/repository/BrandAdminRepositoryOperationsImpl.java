package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.micro.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class BrandAdminRepositoryOperationsImpl implements BrandAdminRepositoryOperations {
    private final JpaQueries queries;

    public List<Map<String, Object>> findCameraModels(Object... args) {
        return queries.queryForList("""
                SELECT b.id,b.name,
                  (SELECT count(*) FROM camerahub.camera_models m WHERE m.brand_id=b.id) AS "modelCount",
                  (SELECT count(*) FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id WHERE m.brand_id=b.id) AS "productCount"
                FROM camerahub.brands b WHERE lower(b.name)<>lower('Khác')
                """, args);
    }

    public <T> List<T> findBrands(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.brands WHERE lower(name)=lower(?)", type, args);
    }

    public <T> T findBrands2(Class<T> type, Object... args) {
        return queries.queryForObject("INSERT INTO camerahub.brands(name) VALUES (?) RETURNING id", type, args);
    }

    public int updateBrands(Object... args) {
        return queries.update("UPDATE camerahub.brands SET name=? WHERE id=?", args);
    }

    public int deleteBrands(Object... args) {
        return queries.update("DELETE FROM camerahub.brands b WHERE b.id=? AND NOT EXISTS (SELECT 1 FROM camerahub.camera_models m WHERE m.brand_id=b.id)", args);
    }

    public <T> List<T> findBrands3(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.brands WHERE id=? FOR UPDATE", type, args);
    }

    public void lockDatabase(Object... args) {
        queries.execute("SELECT pg_advisory_xact_lock(1128353106,1)");
    }
}

