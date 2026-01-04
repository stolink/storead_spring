package com.stolink.backend.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credit_transactions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CreditTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "credit_id", nullable = false)
    private UUID creditId;

    @Column(name = "payment_id")
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CreditTransactionType type;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "balance_before", nullable = false)
    private Long balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @Column(length = 200)
    private String description;

    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 충전 거래 생성
     */
    public static CreditTransaction createChargeTransaction(
            UUID userId, UUID creditId, UUID paymentId,
            Long amount, Long balanceBefore, String description) {
        return CreditTransaction.builder()
            .userId(userId)
            .creditId(creditId)
            .paymentId(paymentId)
            .type(CreditTransactionType.CHARGE)
            .amount(amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceBefore + amount)
            .description(description)
            .build();
    }

    /**
     * 사용 거래 생성
     */
    public static CreditTransaction createUseTransaction(
            UUID userId, UUID creditId,
            Long amount, Long balanceBefore,
            String description, String referenceType, String referenceId) {
        return CreditTransaction.builder()
            .userId(userId)
            .creditId(creditId)
            .type(CreditTransactionType.USE)
            .amount(-amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceBefore - amount)
            .description(description)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .build();
    }

    /**
     * 환불 거래 생성
     */
    public static CreditTransaction createRefundTransaction(
            UUID userId, UUID creditId, UUID paymentId,
            Long amount, Long balanceBefore, String description) {
        return CreditTransaction.builder()
            .userId(userId)
            .creditId(creditId)
            .paymentId(paymentId)
            .type(CreditTransactionType.REFUND)
            .amount(-amount)
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceBefore - amount)
            .description(description)
            .build();
    }
}
