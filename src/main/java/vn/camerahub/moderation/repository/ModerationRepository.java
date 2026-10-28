package vn.camerahub.moderation.repository;

import java.util.*;

import vn.camerahub.moderation.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationRepository extends JpaRepository<ReviewJob, ReviewJobId>, ModerationRepositoryOperations {
}
