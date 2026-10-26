package vn.camerahub.marketplace.repository;

import vn.camerahub.marketplace.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CameraModelJpaRepository extends JpaRepository<CameraModel, UUID> {
}
