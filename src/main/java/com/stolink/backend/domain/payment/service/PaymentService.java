package com.stolink.backend.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.stolink.backend.domain.payment.client.TossPaymentClient;
import com.stolink.backend.domain.payment.dto.request.PaymentCancelRequest;
import com.stolink.backend.domain.payment.dto.request.PaymentConfirmRequest;
import com.stolink.backend.domain.payment.dto.request.PaymentPrepareRequest;
import com.stolink.backend.domain.payment.dto.response.CreditPackageResponse;
import com.stolink.backend.domain.payment.dto.response.PaymentPrepareResponse;
import com.stolink.backend.domain.payment.dto.response.PaymentResponse;
import com.stolink.backend.domain.payment.dto.toss.TossPaymentConfirmResponse;
import com.stolink.backend.domain.payment.entity.*;
import com.stolink.backend.domain.payment.exception.PaymentExceptions;
import com.stolink.backend.domain.payment.exception.TossPaymentException;
import com.stolink.backend.domain.payment.repository.CreditRepository;
import com.stolink.backend.domain.payment.repository.CreditTransactionRepository;
import com.stolink.backend.domain.payment.repository.PaymentRepository;
import com.stolink.backend.domain.payment.repository.PaymentWebhookLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final PaymentWebhookLogRepository webhookLogRepository;
    private final CreditPackageService creditPackageService;
    private final TransactionTemplate transactionTemplate;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 결제 준비 (주문 생성)
     */
    @Transactional
    public PaymentPrepareResponse preparePayment(UUID userId, PaymentPrepareRequest request) {
        CreditPackage creditPackage = creditPackageService.getPackage(request.packageId());

        String orderId = generateOrderId();
        String idempotencyKey = generateIdempotencyKey(userId, orderId);

        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new PaymentExceptions.DuplicatePaymentException("이미 처리 중인 결제가 있습니다.");
        }

        Payment payment = Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .orderName(creditPackage.getName())
                .amount(creditPackage.getPrice())
                .creditAmount(creditPackage.getTotalCredit())
                .status(PaymentStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .expiredAt(LocalDateTime.now().plusMinutes(30))
                .build();

        paymentRepository.save(payment);
        log.info("결제 준비 완료: orderId={}, userId={}, amount={}",
                orderId, userId, creditPackage.getPrice());

        return new PaymentPrepareResponse(
                orderId,
                creditPackage.getName(),
                creditPackage.getPrice(),
                creditPackage.getTotalCredit(),
                generateCustomerKey(userId),
                buildSuccessUrl(orderId),
                buildFailUrl(orderId));
    }

    /**
     * 결제 승인 (트랜잭션 분리: 외부 API 호출 최소화)
     */
    public PaymentResponse confirmPayment(UUID userId, PaymentConfirmRequest request) {
        // 1. (TX1) 결제 정보 조회 및 검증, 상태 변경 (READY -> IN_PROGRESS)
        Payment payment = transactionTemplate.execute(status -> {
            Payment p = paymentRepository.findByOrderIdWithLock(request.orderId())
                    .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException(
                            "주문을 찾을 수 없습니다: " + request.orderId()));

            validatePaymentOwner(p, userId);
            validatePaymentAmount(p, request.amount());
            validateNotExpired(p);

            if (p.isCompleted()) {
                return p;
            }

            // 상태가 이미 IN_PROGRESS여도 재시도 가능하도록 허용하거나 검증
            if (!p.isCompleted() && p.getStatus() != PaymentStatus.IN_PROGRESS) {
                p.markAsInProgress(request.paymentKey());
                paymentRepository.save(p);
            }
            return p;
        });

        if (payment != null && payment.isCompleted()) {
            return PaymentResponse.from(payment);
        }

        // 2. (Non-TX) 토스 API 호출
        TossPaymentConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirmPayment(
                    request.paymentKey(),
                    request.orderId(),
                    request.amount());
        } catch (TossPaymentException e) {
            // 3. (TX2-Fail) 실패 처리
            transactionTemplate.executeWithoutResult(status -> {
                Payment p = paymentRepository.findByOrderIdWithLock(request.orderId()).orElse(null);
                if (p != null) {
                    p.fail(e.getErrorCode(), e.getMessage());
                    paymentRepository.save(p);
                }
            });
            throw e;
        }

        // 4. (TX2-Success) 성공 처리 및 크레딧 지급
        return completePaymentProcess(request.orderId(), request.paymentKey(), tossResponse.method());
    }

    private PaymentResponse completePaymentProcess(String orderId, String paymentKey, String method) {
        return transactionTemplate.execute(status -> {
            Payment p = paymentRepository.findByOrderIdWithLock(orderId)
                    .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException("주문을 찾을 수 없습니다: " + orderId));

            if (p.isCompleted()) {
                log.info("이미 처리된 결제입니다: orderId={}", orderId);
                return PaymentResponse.from(p);
            }

            p.approve(paymentKey, method);
            paymentRepository.save(p);

            Credit credit = getOrCreateCredit(p.getUserId());
            Long balanceBefore = credit.getBalance();
            credit.charge(p.getCreditAmount());
            creditRepository.save(credit);

            CreditTransaction transaction = CreditTransaction.createChargeTransaction(
                    p.getUserId(),
                    credit.getId(),
                    p.getId(),
                    p.getCreditAmount(),
                    balanceBefore,
                    String.format("%s 결제", p.getOrderName()));
            creditTransactionRepository.save(transaction);

            log.info("결제 승인 처리 완료: orderId={}, paymentKey={}, method={}", orderId, paymentKey, method);
            return PaymentResponse.from(p);
        });
    }

    /**
     * 결제 취소 (트랜잭션 분리)
     */
    public PaymentResponse cancelPayment(UUID userId, String paymentId, PaymentCancelRequest request) {
        // 1. (TX1) 검증 및 데이터 준비
        record CancelContext(Payment payment, Long cancelAmount, Long creditToDeduct, Credit credit) {
        }
        CancelContext ctx = transactionTemplate.execute(status -> {
            Payment p = paymentRepository.findByIdWithLock(UUID.fromString(paymentId))
                    .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException("결제를 찾을 수 없습니다: " + paymentId));

            validatePaymentOwner(p, userId);
            if (!p.getStatus().isCancelable()) {
                throw new PaymentExceptions.PaymentNotCancelableException("취소할 수 없는 결제 상태입니다: " + p.getStatus());
            }

            Long cancelAmt = request.cancelAmount() != null
                    ? request.cancelAmount()
                    : p.getCancelableAmount();

            if (cancelAmt > p.getCancelableAmount()) {
                throw new PaymentExceptions.InvalidCancelAmountException(
                        String.format("취소 가능 금액 초과: 요청=%d, 가능=%d", cancelAmt, p.getCancelableAmount()));
            }

            Long creditToDed = calculateCreditToDeduct(p, cancelAmt);

            Credit c = creditRepository.findByUserIdWithLock(userId)
                    .orElseThrow(() -> new PaymentExceptions.CreditNotFoundException("크레딧 정보를 찾을 수 없습니다."));

            if (c.getBalance() < creditToDed) {
                throw new PaymentExceptions.InsufficientCreditException(
                        String.format("환불할 크레딧이 부족합니다. 잔액=%d, 필요=%d", c.getBalance(), creditToDed));
            }

            return new CancelContext(p, cancelAmt, creditToDed, c);
        });

        // 2. (Non-TX) 토스 API 호출
        try {
            tossPaymentClient.cancelPayment(
                    ctx.payment().getPaymentKey(),
                    request.cancelReason(),
                    ctx.cancelAmount());
        } catch (TossPaymentException e) {
            log.error("토스 결제 취소 실패: paymentKey={}, error={}", ctx.payment().getPaymentKey(), e.getMessage());
            throw e;
        }

        // 3. (TX2) 성공 결과 반영 및 크레딧 차감
        return transactionTemplate.execute(status -> {
            Payment p = paymentRepository.findByIdWithLock(ctx.payment().getId()).orElseThrow();
            Credit c = creditRepository.findByUserIdWithLock(userId).orElseThrow();

            p.cancel(ctx.cancelAmount(), request.cancelReason());
            paymentRepository.save(p);

            Long balanceBefore = c.getBalance();
            c.cancelCharge(ctx.creditToDeduct());
            creditRepository.save(c);

            CreditTransaction transaction = CreditTransaction.createRefundTransaction(
                    userId,
                    c.getId(),
                    p.getId(),
                    ctx.creditToDeduct(),
                    balanceBefore,
                    String.format("%s 결제 취소", p.getOrderName()));
            creditTransactionRepository.save(transaction);

            log.info("결제 취소 처리 완료: orderId={}, cancelAmount={}, creditDeducted={}",
                    p.getOrderId(), ctx.cancelAmount(), ctx.creditToDeduct());

            return PaymentResponse.from(p);
        });
    }

    /**
     * 결제 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(UUID userId, Pageable pageable) {
        return paymentRepository.findByUserIdOrderByRequestedAtDesc(userId, pageable)
                .map(PaymentResponse::from);
    }

    /**
     * 결제 상세 조회
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID userId, String paymentId) {
        Payment payment = paymentRepository.findById(UUID.fromString(paymentId))
                .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException("결제를 찾을 수 없습니다: " + paymentId));

        validatePaymentOwner(payment, userId);
        return PaymentResponse.from(payment);
    }

    /**
     * 크레딧 패키지 목록 조회
     */
    public List<CreditPackageResponse> getCreditPackages() {
        return creditPackageService.getAllPackages();
    }

    /**
     * 웹훅 처리
     */
    @Transactional
    public void handleWebhook(String eventType, JsonNode payload) {
        String paymentKey = payload.path("paymentKey").asText();
        String orderId = payload.path("orderId").asText();

        log.info("웹훅 수신: eventType={}, paymentKey={}", eventType, paymentKey);

        if (webhookLogRepository.existsByPaymentKeyAndEventType(paymentKey, eventType)) {
            log.info("중복 웹훅 무시: paymentKey={}, eventType={}", paymentKey, eventType);
            return;
        }

        PaymentWebhookLog webhookLog = PaymentWebhookLog.builder()
                .eventType(eventType)
                .paymentKey(paymentKey)
                .orderId(orderId)
                .requestBody(payload)
                .status(WebhookStatus.RECEIVED)
                .build();
        webhookLogRepository.save(webhookLog);

        try {
            if ("PAYMENT_STATUS_CHANGED".equals(eventType)) {
                handlePaymentStatusChanged(payload);
            }
            webhookLog.markAsProcessed();
        } catch (Exception e) {
            log.error("웹훅 처리 실패: eventType={}, error={}", eventType, e.getMessage());
            webhookLog.markAsFailed(e.getMessage());
            throw e;
        } finally {
            webhookLogRepository.save(webhookLog);
        }
    }

    private void handlePaymentStatusChanged(JsonNode payload) {
        String status = payload.path("status").asText();
        String orderId = payload.path("orderId").asText();
        String paymentKey = payload.path("paymentKey").asText();
        String method = payload.path("method").asText();

        log.info("결제 상태 변경 웹훅 처리: orderId={}, status={}", orderId, status);

        if ("DONE".equals(status)) {
            try {
                completePaymentProcess(orderId, paymentKey, method);
            } catch (Exception e) {
                log.error("웹훅 결제 완료 처리 실패: orderId={}, error={}", orderId, e.getMessage());
                throw e;
            }
        }
    }

    private Credit getOrCreateCredit(UUID userId) {
        return creditRepository.findByUserIdWithLock(userId)
                .orElseGet(() -> creditRepository.save(Credit.createForUser(userId)));
    }

    private String generateOrderId() {
        return "SL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String generateIdempotencyKey(UUID userId, String orderId) {
        // DB 컬럼 길이(64) 제한 준수: userId(36) + orderId(23) + timestamp > 64
        // orderId가 이미 고유하므로 간단한 prefix만 추가
        return "IDEM-" + orderId;
    }

    private String generateCustomerKey(UUID userId) {
        return "CUST-" + userId.toString().replace("-", "").substring(0, 16);
    }

    private String buildSuccessUrl(String orderId) {
        return String.format("/payments/success?orderId=%s", orderId);
    }

    private String buildFailUrl(String orderId) {
        return String.format("/payments/fail?orderId=%s", orderId);
    }

    private void validatePaymentOwner(Payment payment, UUID userId) {
        if (!payment.getUserId().equals(userId)) {
            throw new PaymentExceptions.UnauthorizedPaymentAccessException("결제 접근 권한이 없습니다.");
        }
    }

    private void validatePaymentStatus(Payment payment) {
        if (!payment.isPending()) {
            throw new PaymentExceptions.InvalidPaymentStatusException(
                    "결제 승인 불가 상태입니다: " + payment.getStatus());
        }
    }

    private void validatePaymentAmount(Payment payment, Long amount) {
        if (!payment.getAmount().equals(amount)) {
            throw new PaymentExceptions.PaymentAmountMismatchException(
                    String.format("결제 금액 불일치: 예상=%d, 실제=%d", payment.getAmount(), amount));
        }
    }

    private void validateNotExpired(Payment payment) {
        if (payment.getExpiredAt() != null && payment.getExpiredAt().isBefore(LocalDateTime.now())) {
            payment.expire();
            paymentRepository.save(payment);
            throw new PaymentExceptions.PaymentExpiredException("결제 유효 시간이 만료되었습니다.");
        }
    }

    private Long calculateCreditToDeduct(Payment payment, Long cancelAmount) {
        if (payment.getAmount() == null || payment.getAmount() == 0) {
            throw new PaymentExceptions.InvalidPaymentStateException("결제 금액이 올바르지 않습니다.");
        }

        // 정수 나눗셈에 의한 정밀도 손실 방지를 위해 BigDecimal 사용 (반올림 정책 적용)
        BigDecimal bCancelAmount = BigDecimal.valueOf(cancelAmount);
        BigDecimal bCreditAmount = BigDecimal.valueOf(payment.getCreditAmount());
        BigDecimal bTotalAmount = BigDecimal.valueOf(payment.getAmount());

        return bCancelAmount.multiply(bCreditAmount)
                .divide(bTotalAmount, 0, RoundingMode.HALF_UP)
                .longValue();
    }
}
