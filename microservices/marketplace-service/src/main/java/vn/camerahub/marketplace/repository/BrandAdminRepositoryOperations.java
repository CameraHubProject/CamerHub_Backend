package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import vn.camerahub.micro.persistence.*;

public interface BrandAdminRepositoryOperations {
    List<Map<String, Object>> findCameraModels(Object... args);

    <T> List<T> findBrands(Class<T> type, Object... args);

    <T> T findBrands2(Class<T> type, Object... args);

    int updateBrands(Object... args);

    int deleteBrands(Object... args);

    <T> List<T> findBrands3(Class<T> type, Object... args);

    void lockDatabase(Object... args);
}
