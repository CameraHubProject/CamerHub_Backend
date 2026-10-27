package vn.camerahub.marketplace.repository;

import vn.camerahub.marketplace.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationBatchJpaRepository extends JpaRepository<ReservationBatch, UUID> {
}
