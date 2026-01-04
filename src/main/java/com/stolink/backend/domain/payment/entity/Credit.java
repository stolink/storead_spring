package com.stolink.backend.domain.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "credits")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private Long balance = 0L;

    @Column(name = "total_charged", nullable = false)
    @Builder.Default
    private Long totalCharged = 0L;

    @Column(name = "total_used", nullable = false)
    @Builder.Default
    private Long totalUsed = 0L;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 크레딧 충전
     */
    public void charge(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }
        this.balance += amount;
        this.totalCharged += amount;
    }

    /**
     * 크레딧 사용
     */
    public void use(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }
        if (this.balance < amount) {
            throw new IllegalStateException(
                String.format("크레딧 잔액이 부족합니다. 현재: %d, 필요: %d", this.balance, amount)
            );
        }
        this.balance -= amount;
        this.totalUsed += amount;
    }

    /**
     * 크레딧 환불 (사용 취소)
     */
    public void refund(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("환불 금액은 0보다 커야 합니다.");
        }
        this.balance += amount;
        this.totalUsed -= amount;
    }

    /**
     * 충전 취소 (결제 취소 시)
     */
    public void cancelCharge(Long amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("취소 금액은 0보다 커야 합니다.");
        }
        if (this.balance < amount) {
            throw new IllegalStateException(
                String.format("취소 가능한 크레딧이 부족합니다. 현재: %d, 취소 요청: %d", this.balance, amount)
            );
        }
        this.balance -= amount;
        this.totalCharged -= amount;
    }

    /**
     * 크레딧 사용 가능 여부 확인
     */
    public boolean canUse(Long amount) {
        return this.balance >= amount;
    }

    /**
     * 정적 팩토리 메서드
     */
    public static Credit createForUser(UUID userId) {
        return Credit.builder()
            .userId(userId)
            .balance(0L)
            .totalCharged(0L)
            .totalUsed(0L)
            .build();
    }
}
