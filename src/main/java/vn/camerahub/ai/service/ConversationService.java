package vn.camerahub.ai.service;

import vn.camerahub.ai.exception.*;

import vn.camerahub.ai.service.AiChatService;
import vn.camerahub.ai.repository.ConversationRepository;
import com.fasterxml.jackson.databind.*;

import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;


@Service
@lombok.RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository repository;
    private final ObjectMapper json;
    private final AiChatService chat;

    public List<AiChatService.Message> history(UUID id, String owner) {
        var rows = repository.findByIdAndOwnerKey(id, owner);
        if (rows.isEmpty()) return List.of();
        try {
            return json.readValue(rows.orElseThrow().messages(), new com.fasterxml.jackson.core.type.TypeReference<List<AiChatService.Message>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Object chat(UUID id, String owner, String address, String message, boolean web, UUID product) {
        repository.createIfAbsent(id, owner);
        UUID token = UUID.randomUUID();
        if (repository.acquireLease(token, id, owner) != 1)
            throw new AdvisorException(AdvisorErrorCode.PREVIOUS_CONVERSATION_BUSY);
        try {
            var conversation = new AiChatService.Conversation();
            conversation.restore(history(id, owner));
            var messages = chat.chat(conversation, owner + id, id.toString(), message, web, product);
            if (repository.saveMessages(json.writeValueAsString(messages), id, token) != 1)
                throw new AdvisorException(AdvisorErrorCode.CONVERSATION_CHANGED);
            return messages;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            repository.releaseLease(id, token);
        }
    }

    public void reset(UUID id, String owner) {
        if (repository.countActiveLease(Integer.class, id, owner) > 0)
            throw new AdvisorException(AdvisorErrorCode.RESPONSE_IN_PROGRESS);
        repository.resetWhenIdle(id, owner);
    }
}

