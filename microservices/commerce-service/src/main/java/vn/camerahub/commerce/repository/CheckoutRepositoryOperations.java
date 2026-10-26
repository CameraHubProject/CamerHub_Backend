package vn.camerahub.commerce.repository;

import java.util.*;

import vn.camerahub.commerce.entity.*;
import vn.camerahub.micro.persistence.*;

public interface CheckoutRepositoryOperations {
    List<Map<String, Object>> findShowrooms(Object... args);

    int insertCheckoutBatches(Object... args);

    Map<String, Object> findCheckoutBatches(Object... args);

    <T> T findCheckoutBatches2(Class<T> type, Object... args);

    <T> List<T> findOrders(Class<T> type, Object... args);

    List<Map<String, Object>> findCheckoutBatches3(Object... args);

    Map<String, Object> findCheckoutBatches4(Object... args);

    int insertOrders(Object... args);

    int insertOrderDeposits(Object... args);

    int updateCheckoutBatches(Object... args);

    int updateCheckoutBatches2(Object... args);

    int updateCheckoutBatches3(Object... args);

    int updateCheckoutBatches4(Object... args);

    List<Map<String, Object>> findShowrooms2(Object... args);

    List<Map<String, Object>> findOrders2(Object... args);

    int updateOrders(Object... args);

    int updateOrderDeposits(Object... args);

    int updateCheckoutBatches5(Object... args);

    Map<String, Object> findOrders3(Object... args);

    int updateOrders2(Object... args);

    <T> List<T> findCheckoutBatches5(Class<T> type, Object... args);

    <T> List<T> findCheckoutBatches6(Class<T> type, Object... args);

    <T> List<T> findOrders4(Class<T> type, Object... args);

    <T> List<T> findCheckoutBatches7(Class<T> type, Object... args);

    int updateCheckoutBatches6(Object... args);
}
