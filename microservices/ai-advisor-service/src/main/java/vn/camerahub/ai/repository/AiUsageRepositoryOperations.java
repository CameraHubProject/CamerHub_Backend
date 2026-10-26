package vn.camerahub.ai.repository;

import java.util.*;
import java.time.*;

import vn.camerahub.ai.entity.*;
import vn.camerahub.micro.persistence.*;

public interface AiUsageRepositoryOperations {
    void lockBudget();

    int daily(LocalDate day);

    int minute(String key, long minute);

    void increment(LocalDate day, String key, long minute);
}

