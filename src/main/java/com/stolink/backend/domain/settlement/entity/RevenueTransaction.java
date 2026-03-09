package com.stolink.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "revenue_transactions", indexes = {
        @Index(name = "idx_revenue_tx_author", columnList = "author_id"),
        @Index(name = "idx_revenue_tx_author_period", columnList = "author_id, created_at"),
        @Index(name = "idx_revenue_tx_work", columnList = "work_id"),
        @Index(name = "idx_revenue_tx_settlement", columnList = "settlement_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_revenue_tx_purchase", columnNames = {"purchase_id", "type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RevenueTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "work_id", nullable = false)
    private UUID workId;

    @Column(name = "chapter_id", nullable = false)
    private UUID chapterId;

    @Column(name = "buyer_user_id", nullable = false)
    private UUID buyerUserId;

    @Column(name = "purchase_id", nullable = false)
    private UUID purchaseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RevenueTransactionType type;

    @Column(name = "credit_amount", nullable = false)
    private Long creditAmount;

    @Column(name = "platform_fee_rate", nullable = false)
    private Double platformFeeRate;

    @Column(name = "platform_fee", nullable = false)
    private Long platformFee;

    @Column(name = "author_share", nullable = false)
    private Long authorShare;

    @Column(name = "settlement_id")
    private UUID settlementId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static RevenueTransaction createSaleTransaction(
            UUID authorId, UUID workId, UUID chapterId,
            UUID buyerUserId, UUID purchaseId,
            Integer creditAmount, Double platformFeeRate) {

        long fee = Math.round(creditAmount * platformFeeRate);
        long share = creditAmount - fee;

        return RevenueTransaction.builder()
                .authorId(authorId)
                .workId(workId)
                .chapterId(chapterId)
                .buyerUserId(buyerUserId)
                .purchaseId(purchaseId)
                .type(RevenueTransactionType.CHAPTER_SALE)
                .creditAmount(creditAmount.longValue())
                .platformFeeRate(platformFeeRate)
                .platformFee(fee)
                .authorShare(share)
                .build();
    }

    public static RevenueTransaction createRefundTransaction(
            UUID authorId, UUID workId, UUID chapterId,
            UUID buyerUserId, UUID purchaseId,
            Integer creditAmount, Double platformFeeRate) {

        long fee = Math.round(creditAmount * platformFeeRate);
        long share = creditAmount - fee;

        return RevenueTransaction.builder()
                .authorId(authorId)
                .workId(workId)
                .chapterId(chapterId)
                .buyerUserId(buyerUserId)
                .purchaseId(purchaseId)
                .type(RevenueTransactionType.REFUND)
                .creditAmount((long) -creditAmount)
                .platformFeeRate(platformFeeRate)
                .platformFee(-fee)
                .authorShare(-share)
                .build();
    }

    public void assignToSettlement(UUID settlementId) {
        this.settlementId = settlementId;
    }
}
