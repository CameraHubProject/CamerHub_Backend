package vn.camerahub.moderation.repository;

import java.util.*;

import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class DuplicateIndexRepositoryOperationsImpl implements DuplicateIndexRepositoryOperations {
    private final JpaQueries queries;

    public <T> List<T> findListingIndex(Class<T> type, Object... args) {
        return queries.queryForList("SELECT snapshot::text FROM camerahub.listing_index WHERE listing_id<>? AND listing_status NOT IN ('DRAFT','REJECTED','NEEDS_REVISION','HIDDEN')", type, args);
    }
}

