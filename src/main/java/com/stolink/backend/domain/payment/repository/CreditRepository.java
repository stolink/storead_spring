package com.stolink.backend.domain.payment.repository;

import com.stolink.backend.domain.payment.entity.Credit;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CreditRepository extends JpaRepository<Credit, UUID> {

    Optional<Credit> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credit c WHERE c.userId = :userId")
    Optional<Credit> findByUserIdWithLock(@Param("userId") UUID userId);
}
