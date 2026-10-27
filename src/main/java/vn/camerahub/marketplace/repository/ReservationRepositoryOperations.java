package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import vn.camerahub.shared.persistence.*;

public interface ReservationRepositoryOperations {
    List<Map<String, Object>> lockRequest(Object... args);

    List<Map<String, Object>> lockBatch(Object... args);

    int insertBatch(Object... args);

    int holdProduct(Object... args);

    int markReserved(Object... args);

    int insertProductStatusHistory(Object... args);

    int confirmReservations(Object... args);

    int confirmBatch(Object... args);

    <T> List<T> lockBatchProducts(Class<T> type, Object... args);

    int releaseBatchInventory(Object... args);

    int releaseBatchProduct(Object... args);

    int releaseBatch(Object... args);

    <T> List<T> findSnapshots(Class<T> type, Object... args);

    List<Map<String, Object>> lockReservation(Object... args);

    int releaseOrderInventory(Object... args);

    int releaseOrderProduct(Object... args);
}
