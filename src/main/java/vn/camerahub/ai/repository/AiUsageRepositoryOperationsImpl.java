package vn.camerahub.ai.repository;

import java.time.*;

import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class AiUsageRepositoryOperationsImpl implements AiUsageRepositoryOperations {
    private final JpaQueries queries;

    public void lockBudget() {
        queries.queryForList("SELECT pg_advisory_xact_lock(1128353106,3)");
    }

    public int daily(LocalDate day) {
        return queries.queryForObject("SELECT COALESCE(sum(requests),0) FROM camerahub.advisor_usage WHERE day=?", Integer.class, day);
    }

    public int minute(String key, long minute) {
        return queries.queryForObject("SELECT COALESCE(sum(requests),0) FROM camerahub.advisor_rate_windows WHERE client_key=? AND minute=?", Integer.class, key, minute);
    }

    public void increment(LocalDate day, String key, long minute) {
        queries.update("INSERT INTO camerahub.advisor_usage(day,client_key,requests) VALUES (?,?,1) ON CONFLICT(day,client_key) DO UPDATE SET requests=camerahub.advisor_usage.requests+1", day, key);
        queries.update("INSERT INTO camerahub.advisor_rate_windows(client_key,minute,requests) VALUES (?,?,1) ON CONFLICT(client_key,minute) DO UPDATE SET requests=camerahub.advisor_rate_windows.requests+1", key, minute);
        queries.update("DELETE FROM camerahub.advisor_rate_windows WHERE minute<?", minute - 2);
    }
}
