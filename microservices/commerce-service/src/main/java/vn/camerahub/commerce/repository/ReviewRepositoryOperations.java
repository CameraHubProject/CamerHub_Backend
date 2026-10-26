package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.commerce.entity.*;
import vn.camerahub.micro.persistence.*;

public interface ReviewRepositoryOperations {
    List<Map<String, Object>> findCheckoutBatches(Object... args);

    List<Map<String, Object>> findOrders(Object... args);

    Map<String, Object> findSellerReviews(Object... args);

    List<Map<String, Object>> findSellerReviews2(Object... args);

    <T> T findSellerReviews3(Class<T> type, Object... args);

    List<Map<String, Object>> findOrders2(Object... args);

    int insertSellerReviews(Object... args);
}
