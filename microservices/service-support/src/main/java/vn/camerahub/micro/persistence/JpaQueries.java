package vn.camerahub.micro.persistence;

import jakarta.persistence.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.*;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Native PostgreSQL fragments for leases, snapshots and atomic ON CONFLICT operations.
 */
@Component
@Transactional
public class JpaQueries {
    @PersistenceContext
    private EntityManager entityManager;

    private String numbered(String sql) {
        var result = new StringBuilder();
        boolean quoted = false;
        int parameter = 0;
        for (int n = 0; n < sql.length(); n++) {
            char c = sql.charAt(n);
            if (c == '\'') {
                result.append(c);
                if (quoted && n + 1 < sql.length() && sql.charAt(n + 1) == '\'') {
                    result.append(sql.charAt(++n));
                    continue;
                }
                quoted = !quoted;
                continue;
            }
            if (c == '?' && !quoted) result.append('?').append(++parameter);
            else result.append(c);
        }
        return result.toString().replace("::", "\\:\\:");
    }

    private Query bind(Query query, Object[] args) {
        for (int n = 0; n < args.length; n++) query.setParameter(n + 1, args[n]);
        return query;
    }

    public int update(String sql, Object... args) {
        entityManager.flush();
        int changed = bind(entityManager.createNativeQuery(numbered(sql)), args).executeUpdate();
        entityManager.clear();
        return changed;
    }

    public void execute(String sql) {
        queryForList(sql);
    }

    public List<Map<String, Object>> queryForList(String sql, Object... args) {
        entityManager.flush();
        var query = bind(entityManager.createNativeQuery(numbered(sql), Tuple.class), args);
        var rows = new ArrayList<Map<String, Object>>();
        for (Object raw : query.getResultList()) {
            var tuple = (Tuple) raw;
            var row = new LinkedHashMap<String, Object>();
            for (var element : tuple.getElements()) {
                Object value = tuple.get(element);
                if (value instanceof OffsetDateTime instant) value = Timestamp.from(instant.toInstant());
                else if (value instanceof Instant instant) value = Timestamp.from(instant);
                row.put(element.getAlias(), value);
            }
            rows.add(row);
        }
        return rows;
    }

    public Map<String, Object> queryForMap(String sql, Object... args) {
        var rows = queryForList(sql, args);
        if (rows.size() != 1) throw new IllegalStateException("Expected one database record");
        return rows.getFirst();
    }

    public <T> List<T> query(String sql, JpaRowMapper<T> mapper, Object... args) {
        var rows = queryForList(sql, args);
        var result = new ArrayList<T>();
        for (int n = 0; n < rows.size(); n++) result.add(mapper.map(new JpaRow(rows.get(n)), n));
        return result;
    }

    public <T> T queryForObject(String sql, JpaRowMapper<T> mapper, Object... args) {
        var rows = query(sql, mapper, args);
        if (rows.size() != 1) throw new IllegalStateException("Expected one database record");
        return rows.getFirst();
    }

    public <T> List<T> queryForList(String sql, Class<T> type, Object... args) {
        entityManager.flush();
        var result = new ArrayList<T>();
        for (Object value : bind(entityManager.createNativeQuery(numbered(sql)), args).getResultList())
            result.add(convert(value, type));
        return result;
    }

    public <T> T queryForObject(String sql, Class<T> type, Object... args) {
        var rows = queryForList(sql, type, args);
        if (rows.size() != 1) throw new IllegalStateException("Expected one database record");
        return rows.getFirst();
    }

    @SuppressWarnings("unchecked")
    private <T> T convert(Object value, Class<T> type) {
        if (value == null) return null;
        if (type == Integer.class && value instanceof Number n) return (T) Integer.valueOf(n.intValue());
        if (type == Long.class && value instanceof Number n) return (T) Long.valueOf(n.longValue());
        if (type == String.class) return (T) value.toString();
        if (type == UUID.class && value instanceof String text) return (T) UUID.fromString(text);
        return type.cast(value);
    }
}
