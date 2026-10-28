package vn.camerahub.shared;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** In-process boundary between business modules. */
public abstract class InternalClient {
    public JsonNode get(String module, String path) { return call(module, path, "GET", null, Map.of()); }
    public JsonNode post(String module, String path, Object body) { return call(module, path, "POST", body, Map.of()); }
    public abstract JsonNode call(String module, String path, String method, Object body, Map<String, String> headers);
    public abstract byte[] bytes(String module, String path);
}
