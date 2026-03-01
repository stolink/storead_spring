package com.stolink.backend.domain.payment.entity;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "credit_amount", nullable = false)
    private Long creditAmount;

    @Column(name = "payment_key", length = 200)
    private String paymentKey;

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "canceled_amount")
    @Builder.Default
    private Long canceledAmount = 0L;

    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 500)
    private String failureMessage;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private JsonNode metadata;

    @Version
    private Long version;

    /**
     * 결제창 호출됨 (PENDING -> READY)
     */
    public void markAsReady() {
        validateStatusTransition(PaymentStatus.PENDING, PaymentStatus.READY);
        this.status = PaymentStatus.READY;
    }

    /**
     * 결제 진행 중 (READY -> IN_PROGRESS)
     */
    public void markAsInProgress(String paymentKey) {
        if (this.status != PaymentStatus.PENDING && this.status != PaymentStatus.READY) {
            throw new IllegalStateException(
                    String.format("잘못된 상태 전이입니다. 현재: %s, 대상: %s", this.status, PaymentStatus.IN_PROGRESS));
        }
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.IN_PROGRESS;
    }

    /**
     * 결제 완료 (PENDING/READY/IN_PROGRESS -> DONE)
     */
    public void approve(String paymentKey, String paymentMethod) {
        if (this.status != PaymentStatus.PENDING &&
                this.status != PaymentStatus.READY &&
                this.status != PaymentStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    String.format("결제 승인 불가 상태입니다. 현재: %s", this.status));
        }
        this.paymentKey = paymentKey;
        this.paymentMethod = paymentMethod;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    /**
     * 결제 취소
     */
    public void cancel(Long cancelAmount, String reason) {
        if (this.status != PaymentStatus.DONE && this.status != PaymentStatus.PARTIAL_CANCELED) {
            throw new IllegalStateException(
                    String.format("취소 불가 상태입니다. 현재: %s", this.status));
        }

        Long remainingAmount = this.amount - this.canceledAmount;
        if (cancelAmount > remainingAmount) {
            throw new IllegalArgumentException(
                    String.format("취소 금액이 남은 금액을 초과합니다. 남은 금액: %d, 취소 요청: %d",
                            remainingAmount, cancelAmount));
        }

        this.canceledAmount += cancelAmount;
        this.cancelReason = reason;
        this.canceledAt = LocalDateTime.now();

        if (this.canceledAmount.equals(this.amount)) {
            this.status = PaymentStatus.CANCELED;
        } else {
            this.status = PaymentStatus.PARTIAL_CANCELED;
        }
    }

    /**
     * 결제 실패
     */
    public void fail(String errorCode, String errorMessage) {
        this.status = PaymentStatus.FAILED;
        this.failureCode = errorCode;
        this.failureMessage = errorMessage;
    }

    /**
     * 결제 만료
     */
    public void expire() {
        if (this.status == PaymentStatus.DONE) {
            throw new IllegalStateException("완료된 결제는 만료될 수 없습니다.");
        }
        this.status = PaymentStatus.EXPIRED;
    }

    private void validateStatusTransition(PaymentStatus expected, PaymentStatus target) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("잘못된 상태 전이입니다. 현재: %s, 예상: %s, 대상: %s",
                            this.status, expected, target));
        }
    }

    /**
     * 취소 가능 금액 조회
     */
    public Long getCancelableAmount() {
        if (this.status != PaymentStatus.DONE && this.status != PaymentStatus.PARTIAL_CANCELED) {
            return 0L;
        }
        return this.amount - this.canceledAmount;
    }

    /**
     * 환불 가능한 크레딧 계산
     */
    public Long getRefundableCredit() {
        if (this.status != PaymentStatus.DONE && this.status != PaymentStatus.PARTIAL_CANCELED) {
            return 0L;
        }
        if (this.amount == null || this.amount == 0) {
            return 0L;
        }
        return this.creditAmount - (this.canceledAmount * this.creditAmount / this.amount);
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING || this.status == PaymentStatus.READY;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.DONE;
    }

    public boolean isCanceled() {
        return this.status == PaymentStatus.CANCELED;
    }
}
