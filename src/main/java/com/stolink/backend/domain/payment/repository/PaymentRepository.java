package com.stolink.backend.domain.payment.repository;

import com.stolink.backend.domain.payment.entity.Payment;
import com.stolink.backend.domain.payment.entity.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByOrderId(String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") UUID id);

    boolean existsByIdempotencyKey(String idempotencyKey);

    Page<Payment> findByUserIdOrderByRequestedAtDesc(UUID userId, Pageable pageable);

    List<Payment> findByStatusInAndExpiredAtBefore(List<PaymentStatus> statuses, LocalDateTime expiredAt);

    boolean existsByUserIdAndAmountAndStatusAndRequestedAtAfter(
            UUID userId, Long amount, PaymentStatus status, LocalDateTime after);

    @Modifying
    @Query("UPDATE Payment p SET p.status = :targetStatus, p.updatedAt = :now WHERE p.status IN :sourceStatuses AND p.expiredAt < :expiredAt")
    int updateStatusForExpiredPayments(
            @Param("sourceStatuses") List<PaymentStatus> sourceStatuses,
            @Param("targetStatus") PaymentStatus targetStatus,
            @Param("expiredAt") LocalDateTime expiredAt,
            @Param("now") LocalDateTime now);
}
