package com.stolink.backend.domain.payment.repository;

import com.stolink.backend.domain.payment.entity.PaymentWebhookLog;
import com.stolink.backend.domain.payment.entity.WebhookStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentWebhookLogRepository extends JpaRepository<PaymentWebhookLog, UUID> {

    boolean existsByPaymentKeyAndEventType(String paymentKey, String eventType);

    List<PaymentWebhookLog> findByStatusAndRetryCountLessThan(WebhookStatus status, Integer maxRetryCount);
}
