package vn.camerahub.ai.repository;

import java.util.*;

import vn.camerahub.ai.entity.*;
import vn.camerahub.micro.persistence.*;

public interface ConversationRepositoryOperations {
    int createIfAbsent(Object... args);

    int acquireLease(Object... args);

    int saveMessages(Object... args);

    int releaseLease(Object... args);

    <T> T countActiveLease(Class<T> type, Object... args);

    int resetWhenIdle(Object... args);
}
