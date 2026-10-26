package vn.camerahub.micro.persistence;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

/**
 * A native-query projection. JDBC objects never cross the repository boundary.
 */
public record JpaRow(Map<String, Object> values) {
    public Object getObject(String key) {
        return values.get(key);
    }

    public <T> T getObject(String key, Class<T> type) {
        return type.cast(getObject(key));
    }

    public String getString(String key) {
        Object value = getObject(key);
        return value == null ? null : value.toString();
    }

    public long getLong(String key) {
        Object value = getObject(key);
        return value == null ? 0 : ((Number) value).longValue();
    }

    public boolean getBoolean(String key) {
        return Boolean.TRUE.equals(getObject(key));
    }

    public Timestamp getTimestamp(String key) {
        Object value = getObject(key);
        if (value == null) return null;
        if (value instanceof Timestamp t) return t;
        if (value instanceof Instant t) return Timestamp.from(t);
        return Timestamp.from(((OffsetDateTime) value).toInstant());
    }
}
