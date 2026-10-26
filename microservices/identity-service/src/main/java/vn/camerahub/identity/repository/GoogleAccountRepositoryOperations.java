package vn.camerahub.identity.repository;

import java.util.*;

import vn.camerahub.identity.entity.*;
import vn.camerahub.micro.persistence.*;

public interface GoogleAccountRepositoryOperations {
    <T> List<T> findAuthIdentities(Class<T> type, Object... args);

    <T> List<T> findUsers(Class<T> type, Object... args);

    int updateUsers(Object... args);

    int insertAuthIdentities(Object... args);

    int updateUsers2(Object... args);

    int insertAuthIdentities2(Object... args);
}
