package com.stolink.backend.domain.payment.repository;

import com.stolink.backend.domain.payment.entity.CreditTransaction;
import com.stolink.backend.domain.payment.entity.CreditTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {

    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<CreditTransaction> findByUserIdAndTypeOrderByCreatedAtDesc(
        UUID userId, CreditTransactionType type, Pageable pageable);
}
