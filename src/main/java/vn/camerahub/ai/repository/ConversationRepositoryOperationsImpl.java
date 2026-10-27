package vn.camerahub.ai.repository;

import java.util.*;

import vn.camerahub.ai.entity.AdvisorConversation;
import vn.camerahub.shared.persistence.*;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
@lombok.RequiredArgsConstructor
public class ConversationRepositoryOperationsImpl implements ConversationRepositoryOperations {
    private final JpaQueries queries;

    public int createIfAbsent(Object... args) {
        return queries.update("INSERT INTO camerahub.advisor_conversations(id,owner_key) VALUES (?,?) ON CONFLICT DO NOTHING", args);
    }

    public int acquireLease(Object... args) {
        return queries.update("UPDATE camerahub.advisor_conversations SET lease_token=?,leased_until=now()+interval '3 minutes' WHERE id=? AND owner_key=? AND (leased_until IS NULL OR leased_until<now())", args);
    }

    public int saveMessages(Object... args) {
        return queries.update("UPDATE camerahub.advisor_conversations SET messages=?::jsonb,leased_until=NULL,updated_at=now() WHERE id=? AND lease_token=?", args);
    }

    public int releaseLease(Object... args) {
        return queries.update("UPDATE camerahub.advisor_conversations SET leased_until=NULL WHERE id=? AND lease_token=?", args);
    }

    public <T> T countActiveLease(Class<T> type, Object... args) {
        return queries.queryForObject("SELECT count(*) FROM camerahub.advisor_conversations WHERE id=? AND owner_key=? AND leased_until>now()", type, args);
    }

    public int resetWhenIdle(Object... args) {
        return queries.update("UPDATE camerahub.advisor_conversations SET messages='[]'::jsonb,updated_at=now() WHERE id=? AND owner_key=? AND (leased_until IS NULL OR leased_until<now())", args);
    }
}

