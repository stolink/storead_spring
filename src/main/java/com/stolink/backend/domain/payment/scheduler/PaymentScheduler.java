package com.stolink.backend.domain.payment.scheduler;

import com.stolink.backend.domain.payment.entity.*;
import com.stolink.backend.domain.payment.repository.CreditRepository;
import com.stolink.backend.domain.payment.repository.CreditTransactionRepository;
import com.stolink.backend.domain.payment.repository.PaymentCompensationRepository;
import com.stolink.backend.domain.payment.repository.PaymentRepository;
import com.stolink.backend.domain.payment.repository.PaymentWebhookLogRepository;
import com.stolink.backend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduler {

    private static final int MAX_COMPENSATION_RETRY = 5;

    private final PaymentRepository paymentRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final PaymentCompensationRepository compensationRepository;
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentService paymentService;
    private final TransactionTemplate transactionTemplate;

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
     * handleWebhook()이 자체적으로 새 로그를 생성/관리하므로,
     * 스케줄러는 기존 실패 로그의 retryCount만 업데이트
     */
    @Scheduled(fixedRate = 300000)
    public void retryFailedWebhooks() {
        List<PaymentWebhookLog> failedLogs = webhookLogRepository.findByStatusAndRetryCountLessThan(
                WebhookStatus.FAILED, 3);

        for (PaymentWebhookLog webhookLog : failedLogs) {
            webhookLog.incrementRetryCount();
            try {
                paymentService.handleWebhook(webhookLog.getEventType(), webhookLog.getRequestBody());
                log.info("웹훅 재시도 성공: paymentKey={}, eventType={}",
                        webhookLog.getPaymentKey(), webhookLog.getEventType());
            } catch (Exception e) {
                webhookLog.setNextRetryAt(LocalDateTime.now().plusMinutes(5L * webhookLog.getRetryCount()));
                log.error("웹훅 재시도 실패: paymentKey={}, retryCount={}, error={}",
                        webhookLog.getPaymentKey(), webhookLog.getRetryCount(), e.getMessage());
            }
            webhookLogRepository.save(webhookLog);
        }

        if (!failedLogs.isEmpty()) {
            log.info("웹훅 재시도 처리 완료: count={}", failedLogs.size());
        }
    }

    /**
     * 실패한 보상 트랜잭션 재시도 (10분마다 실행)
     * PG 환불 성공 후 내부 크레딧 차감이 실패한 경우 재시도
     */
    @Scheduled(fixedRate = 600000)
    public void retryFailedCompensations() {
        List<PaymentCompensation> pendingCompensations =
                compensationRepository.findByStatusAndRetryCountLessThan(
                        CompensationStatus.PENDING, MAX_COMPENSATION_RETRY);

        for (PaymentCompensation compensation : pendingCompensations) {
            compensation.incrementRetryCount();

            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Credit credit = creditRepository.findByUserIdWithLock(compensation.getUserId())
                            .orElse(null);

                    if (credit == null || credit.getBalance() < compensation.getCreditAmount()) {
                        String msg = credit == null
                                ? "크레딧 정보 없음"
                                : String.format("잔액 부족: 현재=%d, 필요=%d",
                                        credit.getBalance(), compensation.getCreditAmount());
                        compensation.updateErrorMessage(msg);
                        throw new IllegalStateException(msg);
                    }

                    Long balanceBefore = credit.getBalance();
                    credit.cancelCharge(compensation.getCreditAmount());
                    creditRepository.save(credit);

                    Payment payment = paymentRepository.findById(compensation.getPaymentId())
                            .orElse(null);

                    CreditTransaction transaction = CreditTransaction.createRefundTransaction(
                            compensation.getUserId(),
                            credit.getId(),
                            compensation.getPaymentId(),
                            compensation.getCreditAmount(),
                            balanceBefore,
                            String.format("보상 트랜잭션: 결제 취소 크레딧 차감 (paymentId=%s)",
                                    compensation.getPaymentId()));
                    creditTransactionRepository.save(transaction);

                    compensation.markAsResolved();
                });

                log.info("보상 트랜잭션 성공: paymentId={}, creditAmount={}",
                        compensation.getPaymentId(), compensation.getCreditAmount());
            } catch (Exception e) {
                log.error("보상 트랜잭션 실패: paymentId={}, retryCount={}, error={}",
                        compensation.getPaymentId(), compensation.getRetryCount(), e.getMessage());

                if (compensation.getRetryCount() >= MAX_COMPENSATION_RETRY) {
                    compensation.markAsRequiresManual(
                            "최대 재시도 횟수 초과: " + e.getMessage());
                    log.error("보상 트랜잭션 수동 처리 필요: paymentId={}, userId={}",
                            compensation.getPaymentId(), compensation.getUserId());
                }
            }

            compensationRepository.save(compensation);
        }

        if (!pendingCompensations.isEmpty()) {
            log.info("보상 트랜잭션 재시도 처리 완료: count={}", pendingCompensations.size());
        }
    }
}
