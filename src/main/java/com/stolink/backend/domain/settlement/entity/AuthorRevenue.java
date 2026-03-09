package com.stolink.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "author_revenues", indexes = {
        @Index(name = "idx_author_revenue_author", columnList = "author_id", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AuthorRevenue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "author_id", nullable = false, unique = true)
    private UUID authorId;

    @Column(name = "total_earned", nullable = false)
    @Builder.Default
    private Long totalEarned = 0L;

    @Column(name = "total_settled", nullable = false)
    @Builder.Default
    private Long totalSettled = 0L;

    @Column(name = "pending_balance", nullable = false)
    @Builder.Default
    private Long pendingBalance = 0L;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static AuthorRevenue createForAuthor(UUID authorId) {
        return AuthorRevenue.builder()
                .authorId(authorId)
                .totalEarned(0L)
                .totalSettled(0L)
                .pendingBalance(0L)
                .build();
    }

    public void addEarning(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("수익 금액은 0보다 커야 합니다.");
        }
        this.totalEarned += amount;
        this.pendingBalance += amount;
    }

    public void deductEarning(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }
        if (this.pendingBalance < amount) {
            throw new IllegalStateException(
                    String.format("정산 대기 잔액이 부족합니다. 현재: %d, 차감 요청: %d", this.pendingBalance, amount));
        }
        this.totalEarned -= amount;
        this.pendingBalance -= amount;
    }

    public void settle(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("정산 금액은 0보다 커야 합니다.");
        }
        if (this.pendingBalance < amount) {
            throw new IllegalStateException(
                    String.format("정산 가능 잔액이 부족합니다. 현재: %d, 정산 요청: %d", this.pendingBalance, amount));
        }
        this.totalSettled += amount;
        this.pendingBalance -= amount;
    }

    public void rollbackSettlement(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("롤백 금액은 0보다 커야 합니다.");
        }
        this.totalSettled -= amount;
        this.pendingBalance += amount;
    }
}
