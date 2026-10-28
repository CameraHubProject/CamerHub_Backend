package vn.camerahub.moderation.repository;

import vn.camerahub.moderation.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewDecisionJpaRepository extends JpaRepository<ReviewDecision, UUID> {
}
