package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.micro.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class MarketplaceEventRepositoryOperationsImpl implements MarketplaceEventRepositoryOperations {
    private final JpaQueries queries;

    public int applyPublicAccountProjection(Object... args) {
        return queries.update("""
                INSERT INTO camerahub.public_accounts(id,full_name,account_status,created_at,profile_version) VALUES (?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET full_name=EXCLUDED.full_name,account_status=EXCLUDED.account_status,profile_version=EXCLUDED.profile_version
                WHERE camerahub.public_accounts.profile_version<EXCLUDED.profile_version
                """, args);
    }

    public int applyModerationDecision(Object... args) {
        return queries.update("""
                UPDATE camerahub.products SET listing_status=?,moderation_source=?,moderation_note=?,reviewed_at=now(),
                rejection_reason=CASE WHEN ? IN ('REJECTED','NEEDS_REVISION') THEN ? ELSE NULL END,updated_at=now(),version=version+1
                WHERE id=? AND submitted_revision=? AND listing_status IN ('AI_REVIEW','PENDING_REVIEW')
                """, args);
    }

    public int insertProductStatusHistory(Object... args) {
        return queries.update("INSERT INTO camerahub.product_status_history(product_id,new_status,reason) VALUES (?,?,?)", args);
    }

    public <T> List<T> findIndexPage(Class<T> type, Object... args) {
        return queries.queryForList("SELECT id FROM camerahub.products WHERE listing_status<>'DRAFT' ORDER BY id LIMIT 100 OFFSET ?", type, args);
    }
}

