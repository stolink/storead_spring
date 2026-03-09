package com.stolink.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlements", indexes = {
        @Index(name = "idx_settlement_author", columnList = "author_id"),
        @Index(name = "idx_settlement_status", columnList = "status"),
        @Index(name = "idx_settlement_period", columnList = "author_id, period_start, period_end")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_settlement_author_period",
                columnNames = {"author_id", "period_start", "period_end"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "gross_amount", nullable = false)
    private Long grossAmount;

    @Column(name = "platform_fee_total", nullable = false)
    private Long platformFeeTotal;

    @Column(name = "net_amount", nullable = false)
    private Long netAmount;

    @Column(name = "transaction_count", nullable = false)
    private Integer transactionCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void confirm() {
        validateStatusTransition(SettlementStatus.PENDING, SettlementStatus.CONFIRMED);
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        validateStatusTransition(SettlementStatus.PENDING, SettlementStatus.REJECTED);
        this.status = SettlementStatus.REJECTED;
        this.rejectReason = reason;
    }

    public void startProcessing() {
        if (this.status != SettlementStatus.CONFIRMED && this.status != SettlementStatus.FAILED) {
            throw new IllegalStateException(
                    String.format("정산 처리를 시작할 수 없는 상태입니다. 현재: %s", this.status));
        }
        this.status = SettlementStatus.PROCESSING;
    }

    public void complete() {
        validateStatusTransition(SettlementStatus.PROCESSING, SettlementStatus.COMPLETED);
        this.status = SettlementStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void fail(String reason) {
        validateStatusTransition(SettlementStatus.PROCESSING, SettlementStatus.FAILED);
        this.status = SettlementStatus.FAILED;
        this.rejectReason = reason;
    }

    private void validateStatusTransition(SettlementStatus expected, SettlementStatus target) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    String.format("잘못된 상태 전이입니다. 현재: %s, 예상: %s, 대상: %s",
                            this.status, expected, target));
        }
    }

    public boolean isSettleable() {
        return this.status == SettlementStatus.CONFIRMED || this.status == SettlementStatus.FAILED;
    }
}
