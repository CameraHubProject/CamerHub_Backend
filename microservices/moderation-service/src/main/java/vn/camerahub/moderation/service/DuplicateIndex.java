package vn.camerahub.moderation.service;

import vn.camerahub.moderation.repository.DuplicateIndexRepository;
import vn.camerahub.moderation.util.ImageFingerprint;
import vn.camerahub.moderation.service.ListingAiReviewer;
import vn.camerahub.moderation.util.TextFingerprint;

import com.fasterxml.jackson.databind.*;

import java.util.*;

import org.springframework.stereotype.Service;

@Service
@lombok.RequiredArgsConstructor
public class DuplicateIndex {
    private final DuplicateIndexRepository repository;
    private final ObjectMapper json;

    public String reason(JsonNode snapshot) {
        var own = images(snapshot);
        for (int i = 0; i < own.size(); i++)
            for (int j = 0; j < i; j++)
                if (own.get(i).similar(own.get(j)))
                    return "Ảnh trong tin bị trùng hoặc quá giống nhau. Hãy dùng ảnh ở các góc khác nhau.";
        for (var other : others(snapshot)) {
            String serial = snapshot.path("serialToken").asText();
            if (!serial.isBlank() && serial.equals(other.path("serialToken").asText()))
                return "Tin đăng trùng số sê-ri với một tin đã gửi.";
            if (TextFingerprint.duplicateText(text(snapshot), text(other)))
                return "Tin đăng trùng nội dung với một tin đã gửi.";
            for (var a : own)
                for (var b : images(other))
                    if (a.similar(b))
                        return "Ảnh đã được sử dụng hoặc quá giống ảnh của tin khác. Vui lòng chụp ảnh mới của thiết bị.";
        }
        return null;
    }

    public List<JsonNode> others(JsonNode current) {
        var result = new ArrayList<JsonNode>();
        for (String value : repository.findListingIndex(String.class, UUID.fromString(current.path("id").asText()))) {
            try {
                var other = json.readTree(value);
                if ("AI_REVIEW".equals(other.path("listingStatus").asText()) &&
                        (other.path("createdAt").asText() + other.path("id").asText()).compareTo(current.path("createdAt").asText() + current.path("id").asText()) > 0)
                    continue;
                result.add(other);
            } catch (Exception e) {
                throw new IllegalStateException("Invalid duplicate index", e);
            }
        }
        return result;
    }

    public List<ListingAiReviewer.Candidate> candidates(JsonNode current) {
        String title = TextFingerprint.normalize(current.path("title").asText());
        return others(current).stream().filter(other -> current.path("brand").asText().equals(other.path("brand").asText()))
                .sorted(Comparator.<JsonNode>comparingDouble(other -> TextFingerprint.similarity(title, TextFingerprint.normalize(other.path("title").asText()))).reversed())
                .filter(other -> TextFingerprint.similarity(title, TextFingerprint.normalize(other.path("title").asText())) >= 0.3).limit(3)
                .map(other -> new ListingAiReviewer.Candidate(UUID.fromString(other.path("id").asText()), json.convertValue(other, Map.class), imageIds(other).stream().limit(2).toList())).toList();
    }

    private Map<String, Object> text(JsonNode node) {
        return Map.of("seller_id", node.path("sellerId").asText(), "title", node.path("title").asText(), "description", node.path("description").asText());
    }

    private List<ImageFingerprint> images(JsonNode snapshot) {
        var result = new ArrayList<ImageFingerprint>();
        for (var image : snapshot.path("images"))
            if (!image.path("sha256").isNull() && image.path("sha256").asText().length() == 64)
                result.add(new ImageFingerprint(image.path("sha256").asText(), image.path("dhash").asLong(), image.path("signature").asText()));
        return result;
    }

    public static List<UUID> imageIds(JsonNode snapshot) {
        var ids = new ArrayList<UUID>();
        snapshot.path("images").forEach(image -> ids.add(UUID.fromString(image.path("id").asText())));
        return ids;
    }
}

