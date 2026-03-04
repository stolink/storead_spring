package com.stolink.backend.domain.payment.repository;

import com.stolink.backend.domain.payment.entity.CompensationStatus;
import com.stolink.backend.domain.payment.entity.PaymentCompensation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentCompensationRepository extends JpaRepository<PaymentCompensation, UUID> {

    List<PaymentCompensation> findByStatusAndRetryCountLessThan(
            CompensationStatus status, Integer maxRetryCount);
}
