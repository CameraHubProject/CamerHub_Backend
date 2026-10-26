package vn.camerahub.micro.persistence;

@FunctionalInterface
public interface JpaRowMapper<T> {
    T map(JpaRow row, int index);
}
