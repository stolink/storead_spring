package com.stolink.backend.domain.settlement.repository;

import com.stolink.backend.domain.settlement.entity.Settlement;
import com.stolink.backend.domain.settlement.entity.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {

    Page<Settlement> findByAuthorIdOrderByPeriodStartDesc(UUID authorId, Pageable pageable);

    Optional<Settlement> findByAuthorIdAndPeriodStartAndPeriodEnd(
            UUID authorId, LocalDate periodStart, LocalDate periodEnd);

    boolean existsByAuthorIdAndPeriodStartAndPeriodEnd(
            UUID authorId, LocalDate periodStart, LocalDate periodEnd);

    List<Settlement> findByStatus(SettlementStatus status);

    Optional<Settlement> findByIdAndAuthorId(UUID id, UUID authorId);
}
