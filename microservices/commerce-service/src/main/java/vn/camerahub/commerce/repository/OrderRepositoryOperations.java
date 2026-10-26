package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.commerce.entity.*;
import vn.camerahub.micro.persistence.*;

public interface OrderRepositoryOperations {
    List<Map<String, Object>> findDatabase(Object... args);

    List<Map<String, Object>> findDatabase2(Object... args);

    List<Map<String, Object>> findDatabase3(Object... args);

    List<Map<String, Object>> findPaymentRecords(Object... args);

    List<Map<String, Object>> findSellerReviews(Object... args);

    <T> T findPaymentRecords2(Class<T> type, Object... args);

    int insertPaymentRecords(Object... args);

    int updateOrderDeposits(Object... args);

    int updateOrders(Object... args);

    List<Map<String, Object>> findBuybackRequests(Object... args);

    int insertBuybackRequests(Object... args);
}
