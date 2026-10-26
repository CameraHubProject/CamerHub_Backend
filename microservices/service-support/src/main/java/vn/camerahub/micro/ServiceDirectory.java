package vn.camerahub.micro;

import java.net.URI;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ServiceDirectory {
    private final Map<String, String> urls;

    public ServiceDirectory(@Value("${camerahub.services.identity}") String identity,
                            @Value("${camerahub.services.marketplace}") String marketplace,
                            @Value("${camerahub.services.moderation}") String moderation,
                            @Value("${camerahub.services.commerce}") String commerce,
                            @Value("${camerahub.services.advisor}") String advisor) {
        urls = Map.of("identity", identity, "marketplace", marketplace, "moderation", moderation, "commerce", commerce, "advisor", advisor);
        urls.values().forEach(value -> {
            var uri = URI.create(value);
            if (!java.util.Set.of("http", "https").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                throw new IllegalArgumentException("Invalid internal service URL");
        });
    }

    public URI uri(String service, String path) {
        if (!urls.containsKey(service) || !path.startsWith("/") || path.startsWith("//") || path.contains(".."))
            throw new IllegalArgumentException("Invalid service route");
        return URI.create(urls.get(service) + path);
    }
}

