package vn.camerahub.shared.persistence;

@FunctionalInterface
public interface JpaRowMapper<T> {
    T map(JpaRow row, int index);
}
