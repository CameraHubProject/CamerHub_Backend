package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import vn.camerahub.micro.persistence.*;

public interface ProductRepositoryOperations {
    List<Map<String, Object>> findPublished(Object... args);

    List<Map<String, Object>> findPublicDetail(Object... args);

    <T> List<T> findImageUrls(Class<T> type, Object... args);

    List<Map<String, Object>> findCameraModels(Object... args);

    List<Map<String, Object>> findBrands(Object... args);

    List<Map<String, Object>> findSellerListings(Object... args);

    List<Map<String, Object>> findEditableDraft(Object... args);

    <T> T findModelId(Class<T> type, Object... args);

    <T> T findPrivateSerial(Class<T> type, Object... args);

    List<Map<String, Object>> findDraftMedia(Object... args);

    List<Map<String, Object>> lockEditableDraft(Object... args);

    <T> T countInspectionChecks(Class<T> type, Object... args);

    int saveDraft(Object... args);

    List<Map<String, Object>> findPublicSellerListings(Object... args);

    int insertDraft(Object... args);

    int insertProductStatusHistory(Object... args);

    <T> T countModel(Class<T> type, Object... args);

    <T> T countBrand(Class<T> type, Object... args);

    void lockBrandCatalog(Object... args);

    <T> List<T> findBrandNames(Class<T> type, Object... args);

    <T> T upsertBrand(Class<T> type, Object... args);

    <T> T upsertUnspecifiedModel(Class<T> type, Object... args);

    List<Map<String, Object>> lockBrandSelection(Object... args);

    <T> T countExistingInspectionChecks(Class<T> type, Object... args);

    int changeBrandAndRequeue(Object... args);

    int restoreDraftStatus(Object... args);
}
