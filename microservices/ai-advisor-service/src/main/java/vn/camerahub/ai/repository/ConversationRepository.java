package vn.camerahub.ai.repository;

import java.util.*;

import vn.camerahub.ai.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<AdvisorConversation, UUID>, ConversationRepositoryOperations {
    Optional<AdvisorConversation> findByIdAndOwnerKey(UUID id, String ownerKey);
}
