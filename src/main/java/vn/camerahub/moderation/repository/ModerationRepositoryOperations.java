package vn.camerahub.moderation.repository;

import java.util.*;

import vn.camerahub.moderation.entity.*;
import vn.camerahub.shared.persistence.*;

public interface ModerationRepositoryOperations {
    List<Map<String, Object>> lockDuplicateIndex(Object... args);

    int acknowledgeDecision(Object... args);

    int upsertIndex(Object... args);

    int supersedeOldRevisions(Object... args);

    int enqueueRevision(Object... args);

    int restoreHumanQueue(Object... args);

    int markIndexReady(Object... args);

    <T> T isIndexReady(Class<T> type, Object... args);

    List<Map<String, Object>> lockNextJob(Object... args);

    ReviewJob claimJob(Object... args);

    List<Map<String, Object>> lockClaim(Object... args);

    <T> List<T> findOwnedClaimSnapshot(Class<T> type, Object... args);

    int insertDecision(Object... args);

    int finishJob(Object... args);

    List<Map<String, Object>> findHumanQueue(Object... args);

    List<Map<String, Object>> lockHumanReview(Object... args);
}
