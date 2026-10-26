package vn.camerahub.marketplace.repository;

import java.util.*;

import vn.camerahub.marketplace.entity.*;
import vn.camerahub.micro.persistence.*;

public interface MarketplaceEventRepositoryOperations {
    int applyPublicAccountProjection(Object... args);

    int applyModerationDecision(Object... args);

    int insertProductStatusHistory(Object... args);

    <T> List<T> findIndexPage(Class<T> type, Object... args);
}
