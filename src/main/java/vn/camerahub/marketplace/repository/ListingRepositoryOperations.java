package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import vn.camerahub.shared.persistence.*;

public interface ListingRepositoryOperations {
    int upsertPublicAccount(Object... args);

    List<Map<String, Object>> lockSubmission(Object... args);

    <T> T countImages(Class<T> type, Object... args);

    int markAiReview(Object... args);

    int insertProductStatusHistory(Object... args);

    int advanceSubmittedRevision(Object... args);

    Map<String, Object> findReviewSnapshot(Object... args);

    List<Map<String, Object>> findUnfingerprintedImages(Object... args);

    int saveFingerprint(Object... args);

    List<Map<String, Object>> findFingerprints(Object... args);
}
