package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import vn.camerahub.shared.persistence.*;

public interface ProductMediaRepositoryOperations {
    List<Map<String, Object>> findProducts(Object... args);

    <T> List<T> findProductMedia(Class<T> type, Object... args);

    <T> T findProductMedia2(Class<T> type, Object... args);

    int insertProductMedia(Object... args);

    List<Map<String, Object>> findProductMedia3(Object... args);

    <T> List<T> findProductMedia4(Class<T> type, Object... args);

    List<Map<String, Object>> findProducts2(Object... args);

    int deleteProductMedia(Object... args);

    int markMediaEdited(Object... args);
}
