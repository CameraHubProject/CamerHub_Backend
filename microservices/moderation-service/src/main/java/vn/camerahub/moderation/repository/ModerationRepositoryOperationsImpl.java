package vn.camerahub.moderation.repository;

import java.util.*;

import vn.camerahub.moderation.entity.ReviewJob;
import vn.camerahub.micro.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ModerationRepositoryOperationsImpl implements ModerationRepositoryOperations {
    private final JpaQueries queries;

    public List<Map<String, Object>> lockDuplicateIndex(Object... args) {
        return queries.queryForList("SELECT pg_advisory_xact_lock(1128353106,2)", args);
    }

    public int acknowledgeDecision(Object... args) {
        return queries.update("UPDATE camerahub.review_decisions SET applied=? WHERE id=? AND listing_id=? AND revision=?", args);
    }

    public int upsertIndex(Object... args) {
        return queries.update("""
                INSERT INTO camerahub.listing_index(listing_id,revision,snapshot,listing_status,updated_at) VALUES (?,?,?::jsonb,?,?)
                ON CONFLICT(listing_id) DO UPDATE SET revision=EXCLUDED.revision,snapshot=EXCLUDED.snapshot,listing_status=EXCLUDED.listing_status,updated_at=EXCLUDED.updated_at
                WHERE camerahub.listing_index.revision<EXCLUDED.revision OR
                (camerahub.listing_index.revision=EXCLUDED.revision AND camerahub.listing_index.updated_at<EXCLUDED.updated_at
                  AND (EXCLUDED.listing_status<>'AI_REVIEW' OR camerahub.listing_index.listing_status='AI_REVIEW'))
                """, args);
    }

    public int supersedeOldRevisions(Object... args) {
        return queries.update("UPDATE camerahub.review_jobs SET state='SUPERSEDED',updated_at=now() WHERE listing_id=? AND revision<? AND state IN ('QUEUED','PROCESSING','PENDING_REVIEW')", args);
    }

    public int enqueueRevision(Object... args) {
        return queries.update("INSERT INTO camerahub.review_jobs(listing_id,revision,owner_id,snapshot) VALUES (?,?,?,?::jsonb) ON CONFLICT DO NOTHING", args);
    }

    public int restoreHumanQueue(Object... args) {
        return queries.update("UPDATE camerahub.review_jobs SET state='PENDING_REVIEW',note='Tin chờ Admin duyệt từ hệ thống cũ' WHERE listing_id=? AND revision=? AND state='QUEUED'", args);
    }

    public int markIndexReady(Object... args) {
        return queries.update("UPDATE camerahub.service_flags SET value=true WHERE key='index_ready'", args);
    }

    public <T> T isIndexReady(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT value FROM camerahub.service_flags WHERE key='index_ready'", type, args);
    }

    public List<Map<String, Object>> lockNextJob(Object... args) {
        return queries.queryForList("SELECT listing_id,revision FROM camerahub.review_jobs WHERE state='QUEUED' OR (state='PROCESSING' AND leased_until<now()) ORDER BY created_at LIMIT 1 FOR UPDATE SKIP LOCKED", args);
    }

    public ReviewJob claimJob(Object... args) {
        return queries.queryForObject("UPDATE camerahub.review_jobs SET state='PROCESSING',claim_token=?,leased_until=now()+interval '90 seconds',attempts=attempts+1 WHERE listing_id=? AND revision=? RETURNING *", (r, n) -> new ReviewJob(r.getObject("listing_id", UUID.class), r.getLong("revision"), r.getObject("owner_id", UUID.class), r.getString("snapshot")), args);
    }

    public List<Map<String, Object>> lockClaim(Object... args) {
        return queries.queryForList("SELECT state,claim_token FROM camerahub.review_jobs WHERE listing_id=? AND revision=? FOR UPDATE", args);
    }

    public <T> List<T> findOwnedClaimSnapshot(Class<T> type, Object... args) {
        return queries.queryForList("SELECT snapshot::text FROM camerahub.review_jobs WHERE listing_id=? AND revision=? AND claim_token=? AND state='PROCESSING' FOR UPDATE", type, args);
    }

    public int insertDecision(Object... args) {
        return queries.update("INSERT INTO camerahub.review_decisions(id,listing_id,revision,status,note,source,actor_id) VALUES (?,?,?,?,?,?,?)", args);
    }

    public int finishJob(Object... args) {
        return queries.update("UPDATE camerahub.review_jobs SET state=?,note=?,source=?,decision_id=?,leased_until=NULL,updated_at=now() WHERE listing_id=? AND revision=?", args);
    }

    public List<Map<String, Object>> findHumanQueue(Object... args) {
        return queries.queryForList("SELECT snapshot::text,note FROM camerahub.review_jobs WHERE state='PENDING_REVIEW' ORDER BY created_at LIMIT 100", args);
    }

    public List<Map<String, Object>> lockHumanReview(Object... args) {
        return queries.queryForList("SELECT snapshot::text,owner_id FROM camerahub.review_jobs WHERE listing_id=? AND revision=? AND state='PENDING_REVIEW' FOR UPDATE", args);
    }
}

