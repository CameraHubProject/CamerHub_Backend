package vn.camerahub.moderation.repository;

import vn.camerahub.moderation.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceFlagJpaRepository extends JpaRepository<ServiceFlag, String> {
}
