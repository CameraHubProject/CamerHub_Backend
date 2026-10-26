package vn.camerahub.commerce.repository;

import vn.camerahub.commerce.entity.*;

import java.util.*;
import java.time.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BuybackRequestJpaRepository extends JpaRepository<BuybackRequest, UUID> {
}
