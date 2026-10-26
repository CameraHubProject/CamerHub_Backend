package vn.camerahub.moderation.repository;

import java.util.*;

import vn.camerahub.moderation.entity.*;
import vn.camerahub.micro.persistence.*;

public interface DuplicateIndexRepositoryOperations {
    <T> List<T> findListingIndex(Class<T> type, Object... args);
}
