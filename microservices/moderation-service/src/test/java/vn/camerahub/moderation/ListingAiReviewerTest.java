package vn.camerahub.moderation.service;

import vn.camerahub.moderation.client.OpenAiGateway;
import vn.camerahub.moderation.service.AiSettings;
import vn.camerahub.moderation.service.ListingAiReviewer;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import vn.camerahub.micro.InternalClient;

class ListingAiReviewerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final ListingAiReviewer reviewer = new ListingAiReviewer(new AiSettings("", "test", true, false, 200), mock(OpenAiGateway.class), json, mock(InternalClient.class));

    private com.fasterxml.jackson.databind.JsonNode response(String decision, double confidence, String matched) {
        return json.valueToTree(Map.of("status", "completed", "output", List.of(Map.of("type", "message", "content", List.of(Map.of("type", "output_text", "text",
                "{\"decision\":\"" + decision + "\",\"confidence\":" + confidence + ",\"reason\":\"Kết quả kiểm tra\",\"matchedListingId\":\"" + matched + "\"}"))))));
    }

    @Test
    void onlyConfidentValidResultsPublishOrReject() throws Exception {
        assertThat(reviewer.parse(response("APPROVE", 0.98, "")).status()).isEqualTo("PUBLISHED");
        assertThat(reviewer.parse(response("APPROVE", 0.7, "")).status()).isEqualTo("PENDING_REVIEW");
        assertThat(reviewer.parse(response("UNCERTAIN", 1, "")).status()).isEqualTo("PENDING_REVIEW");
        assertThat(reviewer.parse(response("REJECT", 0.99, "")).status()).isEqualTo("REJECTED");
        assertThatThrownBy(() -> reviewer.parse(response("PUBLISH_EVERYTHING", 1, ""))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reviewer.parse(response("APPROVE", 2, ""))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicatesMustReferenceAnActualCandidateAndIncompleteResponsesCannotPublish() throws Exception {
        String id = UUID.randomUUID().toString();
        assertThat(reviewer.parse(response("DUPLICATE", 0.99, id), List.of(id)).status()).isEqualTo("REJECTED");
        assertThatThrownBy(() -> reviewer.parse(response("DUPLICATE", 0.99, id))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> reviewer.parse(json.readTree("{\"status\":\"incomplete\"}"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unavailableAiGoesToAdmin() {
        assertThat(reviewer.review(Map.of("title", "Canon R6"), List.of()).status()).isEqualTo("PENDING_REVIEW");
    }
}

