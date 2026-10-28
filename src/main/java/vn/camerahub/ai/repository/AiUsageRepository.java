package vn.camerahub.ai.repository;

import java.util.*;

import vn.camerahub.ai.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiUsageRepository extends JpaRepository<AdvisorUsage, AdvisorUsageId>, AiUsageRepositoryOperations {
}
