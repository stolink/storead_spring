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
    private final TossPaymentClient tossPaymentClient;
    private final CreditPackageService creditPackageService;

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
            buildFailUrl(orderId)
        );
    }

    /**
     * 결제 승인
     */
    @Transactional
    public PaymentResponse confirmPayment(UUID userId, PaymentConfirmRequest request) {
        Payment payment = paymentRepository.findByOrderIdWithLock(request.orderId())
            .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException("주문을 찾을 수 없습니다: " + request.orderId()));

        validatePaymentOwner(payment, userId);
        validatePaymentStatus(payment);
        validatePaymentAmount(payment, request.amount());
        validateNotExpired(payment);

        if (payment.isCompleted()) {
            log.info("이미 완료된 결제: orderId={}", request.orderId());
            return PaymentResponse.from(payment);
        }

        TossPaymentConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirmPayment(
                request.paymentKey(),
                request.orderId(),
                request.amount()
            );
        } catch (TossPaymentException e) {
            payment.fail(e.getErrorCode(), e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }

        payment.approve(request.paymentKey(), tossResponse.method());
        paymentRepository.save(payment);

        Credit credit = getOrCreateCredit(userId);
        Long balanceBefore = credit.getBalance();
        credit.charge(payment.getCreditAmount());
        creditRepository.save(credit);

        CreditTransaction transaction = CreditTransaction.createChargeTransaction(
            userId,
            credit.getId(),
            payment.getId(),
            payment.getCreditAmount(),
            balanceBefore,
            String.format("%s 결제", payment.getOrderName())
        );
        creditTransactionRepository.save(transaction);

        log.info("결제 승인 완료: orderId={}, paymentKey={}, creditAmount={}",
            request.orderId(), request.paymentKey(), payment.getCreditAmount());

        return PaymentResponse.from(payment);
    }

    /**
     * 결제 취소
     */
    @Transactional
    public PaymentResponse cancelPayment(UUID userId, String paymentId, PaymentCancelRequest request) {
        Payment payment = paymentRepository.findByIdWithLock(UUID.fromString(paymentId))
            .orElseThrow(() -> new PaymentExceptions.PaymentNotFoundException("결제를 찾을 수 없습니다: " + paymentId));

        validatePaymentOwner(payment, userId);
        if (!payment.getStatus().isCancelable()) {
            throw new PaymentExceptions.PaymentNotCancelableException("취소할 수 없는 결제 상태입니다: " + payment.getStatus());
        }

        Long cancelAmount = request.cancelAmount() != null
            ? request.cancelAmount()
            : payment.getCancelableAmount();

        if (cancelAmount > payment.getCancelableAmount()) {
            throw new PaymentExceptions.InvalidCancelAmountException(
                String.format("취소 가능 금액 초과: 요청=%d, 가능=%d", cancelAmount, payment.getCancelableAmount())
            );
        }

        Long creditToDeduct = calculateCreditToDeduct(payment, cancelAmount);

        Credit credit = creditRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new PaymentExceptions.CreditNotFoundException("크레딧 정보를 찾을 수 없습니다."));

        if (credit.getBalance() < creditToDeduct) {
            throw new PaymentExceptions.InsufficientCreditException(
                String.format("환불할 크레딧이 부족합니다. 잔액=%d, 필요=%d", credit.getBalance(), creditToDeduct)
            );
        }

        try {
            tossPaymentClient.cancelPayment(
                payment.getPaymentKey(),
                request.cancelReason(),
                cancelAmount
            );
        } catch (TossPaymentException e) {
            log.error("토스 결제 취소 실패: paymentKey={}, error={}", payment.getPaymentKey(), e.getMessage());
            throw e;
        }

        payment.cancel(cancelAmount, request.cancelReason());
        paymentRepository.save(payment);

        Long balanceBefore = credit.getBalance();
        credit.cancelCharge(creditToDeduct);
        creditRepository.save(credit);

        CreditTransaction transaction = CreditTransaction.createRefundTransaction(
            userId,
            credit.getId(),
            payment.getId(),
            creditToDeduct,
            balanceBefore,
            String.format("%s 결제 취소", payment.getOrderName())
        );
        creditTransactionRepository.save(transaction);

        log.info("결제 취소 완료: paymentId={}, cancelAmount={}, creditDeducted={}",
            paymentId, cancelAmount, creditToDeduct);

        return PaymentResponse.from(payment);
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
        log.info("결제 상태 변경 웹훅 처리: {}", payload);
    }

    private Credit getOrCreateCredit(UUID userId) {
        return creditRepository.findByUserId(userId)
            .orElseGet(() -> creditRepository.save(Credit.createForUser(userId)));
    }

    private String generateOrderId() {
        return "SL-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String generateIdempotencyKey(UUID userId, String orderId) {
        return String.format("%s:%s:%d", userId, orderId, System.currentTimeMillis() / 60000);
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
                "결제 승인 불가 상태입니다: " + payment.getStatus()
            );
        }
    }

    private void validatePaymentAmount(Payment payment, Long amount) {
        if (!payment.getAmount().equals(amount)) {
            throw new PaymentExceptions.PaymentAmountMismatchException(
                String.format("결제 금액 불일치: 예상=%d, 실제=%d", payment.getAmount(), amount)
            );
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
        return (cancelAmount * payment.getCreditAmount()) / payment.getAmount();
    }
}
