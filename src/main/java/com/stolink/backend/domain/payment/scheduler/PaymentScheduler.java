package com.stolink.backend.domain.payment.scheduler;

import com.stolink.backend.domain.payment.entity.Payment;
import com.stolink.backend.domain.payment.entity.PaymentStatus;
import com.stolink.backend.domain.payment.entity.PaymentWebhookLog;
import com.stolink.backend.domain.payment.entity.WebhookStatus;
import com.stolink.backend.domain.payment.repository.PaymentRepository;
import com.stolink.backend.domain.payment.repository.PaymentWebhookLogRepository;
import com.stolink.backend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final PaymentService paymentService;

    /**
     * 만료된 결제 처리 (1분마다 실행)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expireOldPayments() {
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = paymentRepository.updateStatusForExpiredPayments(
                List.of(PaymentStatus.PENDING, PaymentStatus.READY),
                PaymentStatus.EXPIRED,
                now);

        if (updatedCount > 0) {
            log.info("만료된 결제 벌크 처리 완료: count={}", updatedCount);
        }
    }

    /**
     * 실패한 웹훅 재시도 (5분마다 실행)
     */
    @Scheduled(fixedRate = 300000)
    public void retryFailedWebhooks() {
        List<PaymentWebhookLog> failedLogs = webhookLogRepository.findByStatusAndRetryCountLessThan(
                WebhookStatus.FAILED, 3);

        if (!failedLogs.isEmpty()) {
            for (PaymentWebhookLog log : failedLogs) {
                try {
                    paymentService.handleWebhook(log.getEventType(), log.getRequestBody());
                    log.markAsProcessed();
                    this.log.info("웹훅 재시도 성공: paymentKey={}, eventType={}",
                            log.getPaymentKey(), log.getEventType());
                } catch (Exception e) {
                    log.incrementRetryCount();
                    log.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * log.getRetryCount()));
                    this.log.error("웹훅 재시도 실패: paymentKey={}, retryCount={}, error={}",
                            log.getPaymentKey(), log.getRetryCount(), e.getMessage());
                }
                webhookLogRepository.save(log);
            }
            log.info("웹훅 재시도 처리 완료: count={}", failedLogs.size());
        }
    }
}
