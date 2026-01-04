# 토스 페이먼츠 크레딧 결제 시스템 구현 프롬프트

> 이 프롬프트는 Spring Boot 백엔드에 토스 페이먼츠 기반 크레딧 충전 시스템을 구현하기 위한 상세 가이드입니다.

---

## 🎯 구현 목표

StoLink 플랫폼에 **크레딧 기반 결제 시스템**을 구현합니다.
- 사용자가 토스 페이먼츠를 통해 크레딧을 충전
- 충전된 크레딧으로 AI 기능 사용
- 결제 내역 조회 및 환불 처리

---

## 📋 기술 스택 및 제약사항

```yaml
Backend: Spring Boot 3.4.1, Java 21
Database: PostgreSQL 16
ORM: Spring Data JPA, Hibernate 6.x
Payment: 토스 페이먼츠 API v1
Validation: Jakarta Validation
```

### 기존 프로젝트 규칙 준수
- `CLAUDE.md`의 모든 코딩 규칙 적용
- Controller → Service → Repository 레이어 분리
- DTO와 Entity 분리 (Entity 직접 노출 금지)
- `ApiResponse<T>` 래퍼 사용
- 예외는 `GlobalExceptionHandler`에서 전역 처리

---

## 🗄️ 데이터베이스 스키마 설계

### 1. Credit (크레딧 잔액 테이블)

```sql
CREATE TABLE credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    total_charged BIGINT NOT NULL DEFAULT 0,      -- 총 충전 금액
    total_used BIGINT NOT NULL DEFAULT 0,         -- 총 사용 금액
    version BIGINT NOT NULL DEFAULT 0,            -- 낙관적 락
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id)
);

CREATE INDEX idx_credits_user_id ON credits(user_id);
```

### 2. Payment (결제 테이블)

```sql
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    
    -- 주문 정보
    order_id VARCHAR(64) NOT NULL UNIQUE,         -- 가맹점 주문 ID (UUID)
    order_name VARCHAR(100) NOT NULL,             -- 주문명 (예: "크레딧 1000점 충전")
    
    -- 결제 금액
    amount BIGINT NOT NULL CHECK (amount > 0),    -- 결제 금액 (원)
    credit_amount BIGINT NOT NULL,                -- 충전될 크레딧
    
    -- 토스 페이먼츠 정보
    payment_key VARCHAR(200),                     -- 토스 결제 고유 키
    payment_method VARCHAR(50),                   -- 결제 수단 (카드, 가상계좌 등)
    
    -- 상태 관리
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    -- PENDING: 결제 대기
    -- READY: 결제창 호출됨
    -- IN_PROGRESS: 결제 진행 중
    -- DONE: 결제 완료 (크레딧 충전됨)
    -- CANCELED: 전체 취소
    -- PARTIAL_CANCELED: 부분 취소
    -- FAILED: 결제 실패
    -- EXPIRED: 만료됨
    
    -- 취소 정보
    canceled_amount BIGINT DEFAULT 0,             -- 취소된 금액
    cancel_reason VARCHAR(200),                   -- 취소 사유
    
    -- 실패 정보
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),
    
    -- 멱등성 키
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,
    
    -- 타임스탬프
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP,
    canceled_at TIMESTAMP,
    expired_at TIMESTAMP,                         -- 결제 만료 시간
    
    -- 메타데이터
    metadata JSONB,                               -- 추가 정보
    version BIGINT NOT NULL DEFAULT 0,
    
    CONSTRAINT chk_canceled_amount CHECK (canceled_amount <= amount)
);

CREATE INDEX idx_payments_user_id ON payments(user_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_order_id ON payments(order_id);
CREATE INDEX idx_payments_payment_key ON payments(payment_key);
CREATE INDEX idx_payments_requested_at ON payments(requested_at DESC);
```

### 3. CreditTransaction (크레딧 거래 내역)

```sql
CREATE TABLE credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    credit_id UUID NOT NULL REFERENCES credits(id),
    payment_id UUID REFERENCES payments(id),      -- 충전 시에만 연결
    
    -- 거래 정보
    type VARCHAR(30) NOT NULL,
    -- CHARGE: 충전
    -- USE: 사용
    -- REFUND: 환불
    -- EXPIRE: 만기 소멸
    -- ADMIN_ADJUST: 관리자 조정
    
    amount BIGINT NOT NULL,                       -- 거래 금액 (양수: 증가, 음수: 감소)
    balance_before BIGINT NOT NULL,               -- 거래 전 잔액
    balance_after BIGINT NOT NULL,                -- 거래 후 잔액
    
    -- 상세 정보
    description VARCHAR(200),                     -- 거래 설명
    reference_type VARCHAR(50),                   -- 참조 타입 (AI_JOB 등)
    reference_id VARCHAR(100),                    -- 참조 ID
    
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_balance_consistency CHECK (balance_after = balance_before + amount)
);

CREATE INDEX idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX idx_credit_transactions_credit_id ON credit_transactions(credit_id);
CREATE INDEX idx_credit_transactions_type ON credit_transactions(type);
CREATE INDEX idx_credit_transactions_created_at ON credit_transactions(created_at DESC);
```

### 4. PaymentWebhookLog (웹훅 로그)

```sql
CREATE TABLE payment_webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- 웹훅 정보
    event_type VARCHAR(50) NOT NULL,              -- PAYMENT_STATUS_CHANGED 등
    payment_key VARCHAR(200),
    order_id VARCHAR(64),
    
    -- 처리 정보
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    -- RECEIVED: 수신됨
    -- PROCESSED: 처리 완료
    -- FAILED: 처리 실패
    -- DUPLICATE: 중복 처리 (멱등성)
    
    -- 요청/응답 데이터
    request_body JSONB NOT NULL,
    response_body JSONB,
    error_message VARCHAR(500),
    
    -- 재시도 정보
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,
    
    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX idx_webhook_logs_payment_key ON payment_webhook_logs(payment_key);
CREATE INDEX idx_webhook_logs_status ON payment_webhook_logs(status);
```

---

## 🏗️ Entity 설계

### Credit.java

```java
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
    private Long balance;
    
    @Column(name = "total_charged", nullable = false)
    private Long totalCharged;
    
    @Column(name = "total_used", nullable = false)
    private Long totalUsed;
    
    @Version
    private Long version;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    // === 비즈니스 메서드 ===
    
    /**
     * 크레딧 충전
     * @throws IllegalArgumentException 충전 금액이 0 이하인 경우
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
     * @throws IllegalStateException 잔액 부족
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
    
    // === 정적 팩토리 메서드 ===
    
    public static Credit createForUser(UUID userId) {
        return Credit.builder()
            .userId(userId)
            .balance(0L)
            .totalCharged(0L)
            .totalUsed(0L)
            .build();
    }
}
```

### Payment.java

```java
package com.stolink.backend.domain.payment.entity;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

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
    
    // 주문 정보
    @Column(name = "order_id", nullable = false, unique = true, length = 64)
    private String orderId;
    
    @Column(name = "order_name", nullable = false, length = 100)
    private String orderName;
    
    // 금액 정보
    @Column(nullable = false)
    private Long amount;
    
    @Column(name = "credit_amount", nullable = false)
    private Long creditAmount;
    
    // 토스 페이먼츠 정보
    @Column(name = "payment_key", length = 200)
    private String paymentKey;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    // 상태
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;
    
    // 취소 정보
    @Column(name = "canceled_amount")
    @Builder.Default
    private Long canceledAmount = 0L;
    
    @Column(name = "cancel_reason", length = 200)
    private String cancelReason;
    
    // 실패 정보
    @Column(name = "failure_code", length = 100)
    private String failureCode;
    
    @Column(name = "failure_message", length = 500)
    private String failureMessage;
    
    // 멱등성 키
    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;
    
    // 타임스탬프
    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;
    
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
    
    // 메타데이터
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode metadata;
    
    @Version
    private Long version;
    
    // === 상태 전이 메서드 ===
    
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
        validateStatusTransition(PaymentStatus.READY, PaymentStatus.IN_PROGRESS);
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.IN_PROGRESS;
    }
    
    /**
     * 결제 완료 (IN_PROGRESS -> DONE)
     */
    public void approve(String paymentKey, String paymentMethod) {
        if (this.status != PaymentStatus.IN_PROGRESS && this.status != PaymentStatus.READY) {
            throw new IllegalStateException(
                String.format("결제 승인 불가 상태입니다. 현재: %s", this.status)
            );
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
                String.format("취소 불가 상태입니다. 현재: %s", this.status)
            );
        }
        
        Long remainingAmount = this.amount - this.canceledAmount;
        if (cancelAmount > remainingAmount) {
            throw new IllegalArgumentException(
                String.format("취소 금액이 남은 금액을 초과합니다. 남은 금액: %d, 취소 요청: %d", 
                    remainingAmount, cancelAmount)
            );
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
    
    // === 유틸리티 메서드 ===
    
    private void validateStatusTransition(PaymentStatus expected, PaymentStatus target) {
        if (this.status != expected) {
            throw new IllegalStateException(
                String.format("잘못된 상태 전이입니다. 현재: %s, 예상: %s, 대상: %s", 
                    this.status, expected, target)
            );
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
        // 취소된 금액에 비례하여 크레딧 계산
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
```

### PaymentStatus.java (Enum)

```java
package com.stolink.backend.domain.payment.entity;

public enum PaymentStatus {
    PENDING("결제 대기"),
    READY("결제창 호출됨"),
    IN_PROGRESS("결제 진행 중"),
    DONE("결제 완료"),
    CANCELED("전체 취소"),
    PARTIAL_CANCELED("부분 취소"),
    FAILED("결제 실패"),
    EXPIRED("만료됨");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    public boolean isTerminal() {
        return this == DONE || this == CANCELED || this == FAILED || this == EXPIRED;
    }
    
    public boolean isCancelable() {
        return this == DONE || this == PARTIAL_CANCELED;
    }
}
```

### CreditTransaction.java

```java
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
    
    // === 정적 팩토리 메서드 ===
    
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
    
    public static CreditTransaction createUseTransaction(
            UUID userId, UUID creditId,
            Long amount, Long balanceBefore,
            String description, String referenceType, String referenceId) {
        return CreditTransaction.builder()
            .userId(userId)
            .creditId(creditId)
            .type(CreditTransactionType.USE)
            .amount(-amount)  // 사용은 음수
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceBefore - amount)
            .description(description)
            .referenceType(referenceType)
            .referenceId(referenceId)
            .build();
    }
    
    public static CreditTransaction createRefundTransaction(
            UUID userId, UUID creditId, UUID paymentId,
            Long amount, Long balanceBefore, String description) {
        return CreditTransaction.builder()
            .userId(userId)
            .creditId(creditId)
            .paymentId(paymentId)
            .type(CreditTransactionType.REFUND)
            .amount(-amount)  // 환불로 인한 차감은 음수
            .balanceBefore(balanceBefore)
            .balanceAfter(balanceBefore - amount)
            .description(description)
            .build();
    }
}
```

### CreditTransactionType.java

```java
package com.stolink.backend.domain.payment.entity;

public enum CreditTransactionType {
    CHARGE("충전"),
    USE("사용"),
    REFUND("환불"),
    EXPIRE("만기 소멸"),
    ADMIN_ADJUST("관리자 조정");
    
    private final String description;
    
    CreditTransactionType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
```

---

## 📡 DTO 설계

### Request DTOs

```java
// PaymentPrepareRequest.java
package com.stolink.backend.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PaymentPrepareRequest(
    @NotNull(message = "크레딧 패키지 ID는 필수입니다")
    Long packageId
) {}

// PaymentConfirmRequest.java
public record PaymentConfirmRequest(
    @NotNull(message = "주문 ID는 필수입니다")
    String orderId,
    
    @NotNull(message = "결제 키는 필수입니다")
    String paymentKey,
    
    @NotNull(message = "결제 금액은 필수입니다")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다")
    Long amount
) {}

// PaymentCancelRequest.java
public record PaymentCancelRequest(
    @NotNull(message = "취소 사유는 필수입니다")
    String cancelReason,
    
    Long cancelAmount  // null이면 전액 취소
) {}

// CreditUseRequest.java
public record CreditUseRequest(
    @NotNull(message = "사용 금액은 필수입니다")
    @Min(value = 1, message = "사용 금액은 1 이상이어야 합니다")
    Long amount,
    
    @NotNull(message = "사용 설명은 필수입니다")
    String description,
    
    String referenceType,  // AI_JOB 등
    String referenceId
) {}
```

### Response DTOs

```java
// PaymentPrepareResponse.java
package com.stolink.backend.domain.payment.dto.response;

public record PaymentPrepareResponse(
    String orderId,
    String orderName,
    Long amount,
    Long creditAmount,
    String customerKey,        // 토스 페이먼츠 고객 키
    String successUrl,
    String failUrl
) {}

// PaymentResponse.java
public record PaymentResponse(
    String id,
    String orderId,
    String orderName,
    Long amount,
    Long creditAmount,
    String paymentKey,
    String paymentMethod,
    String status,
    Long canceledAmount,
    String cancelReason,
    LocalDateTime requestedAt,
    LocalDateTime approvedAt,
    LocalDateTime canceledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
            payment.getId().toString(),
            payment.getOrderId(),
            payment.getOrderName(),
            payment.getAmount(),
            payment.getCreditAmount(),
            payment.getPaymentKey(),
            payment.getPaymentMethod(),
            payment.getStatus().name(),
            payment.getCanceledAmount(),
            payment.getCancelReason(),
            payment.getRequestedAt(),
            payment.getApprovedAt(),
            payment.getCanceledAt()
        );
    }
}

// CreditResponse.java
public record CreditResponse(
    String id,
    Long balance,
    Long totalCharged,
    Long totalUsed,
    LocalDateTime updatedAt
) {
    public static CreditResponse from(Credit credit) {
        return new CreditResponse(
            credit.getId().toString(),
            credit.getBalance(),
            credit.getTotalCharged(),
            credit.getTotalUsed(),
            credit.getUpdatedAt()
        );
    }
}

// CreditTransactionResponse.java
public record CreditTransactionResponse(
    String id,
    String type,
    Long amount,
    Long balanceBefore,
    Long balanceAfter,
    String description,
    String referenceType,
    String referenceId,
    LocalDateTime createdAt
) {
    public static CreditTransactionResponse from(CreditTransaction tx) {
        return new CreditTransactionResponse(
            tx.getId().toString(),
            tx.getType().name(),
            tx.getAmount(),
            tx.getBalanceBefore(),
            tx.getBalanceAfter(),
            tx.getDescription(),
            tx.getReferenceType(),
            tx.getReferenceId(),
            tx.getCreatedAt()
        );
    }
}

// CreditPackageResponse.java (크레딧 상품 목록)
public record CreditPackageResponse(
    Long id,
    String name,
    Long price,           // 원
    Long creditAmount,    // 크레딧
    Long bonusCredit,     // 보너스 크레딧
    boolean isPopular
) {}
```

---

## 🔧 Service 설계

### PaymentService.java

```java
package com.stolink.backend.domain.payment.service;

import com.stolink.backend.domain.payment.dto.request.*;
import com.stolink.backend.domain.payment.dto.response.*;
import com.stolink.backend.domain.payment.entity.*;
import com.stolink.backend.domain.payment.repository.*;
import com.stolink.backend.global.common.exception.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {
    
    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final TossPaymentClient tossPaymentClient;
    private final CreditPackageService creditPackageService;
    
    /**
     * 결제 준비 (주문 생성)
     * 1. 크레딧 패키지 검증
     * 2. 주문 ID 생성 (UUID)
     * 3. Payment 엔티티 생성 (PENDING)
     * 4. 결제창 호출에 필요한 정보 반환
     */
    @Transactional
    public PaymentPrepareResponse preparePayment(UUID userId, PaymentPrepareRequest request) {
        // 1. 크레딧 패키지 조회
        CreditPackage creditPackage = creditPackageService.getPackage(request.packageId());
        
        // 2. 주문 ID 및 멱등성 키 생성
        String orderId = generateOrderId();
        String idempotencyKey = generateIdempotencyKey(userId, orderId);
        
        // 3. 중복 주문 검사 (멱등성)
        if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
            throw new DuplicatePaymentException("이미 처리 중인 결제가 있습니다.");
        }
        
        // 4. Payment 엔티티 생성
        Payment payment = Payment.builder()
            .userId(userId)
            .orderId(orderId)
            .orderName(creditPackage.getName())
            .amount(creditPackage.getPrice())
            .creditAmount(creditPackage.getTotalCredit())
            .status(PaymentStatus.PENDING)
            .idempotencyKey(idempotencyKey)
            .expiredAt(LocalDateTime.now().plusMinutes(30))  // 30분 후 만료
            .build();
        
        paymentRepository.save(payment);
        log.info("결제 준비 완료: orderId={}, userId={}, amount={}", 
            orderId, userId, creditPackage.getPrice());
        
        // 5. 응답 생성
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
     * 결제 승인 (토스 페이먼츠 확인)
     * 
     * ⚠️ 핵심 트랜잭션 - 데이터 정합성 필수
     * 1. 주문 조회 및 검증
     * 2. 토스 페이먼츠 승인 API 호출
     * 3. Payment 상태 업데이트
     * 4. 크레딧 충전
     * 5. 거래 내역 기록
     */
    @Transactional
    public PaymentResponse confirmPayment(UUID userId, PaymentConfirmRequest request) {
        // 1. 주문 조회
        Payment payment = paymentRepository.findByOrderIdWithLock(request.orderId())
            .orElseThrow(() -> new PaymentNotFoundException("주문을 찾을 수 없습니다: " + request.orderId()));
        
        // 2. 검증
        validatePaymentOwner(payment, userId);
        validatePaymentStatus(payment);
        validatePaymentAmount(payment, request.amount());
        validateNotExpired(payment);
        
        // 3. 토스 페이먼츠 승인 API 호출
        TossPaymentConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirmPayment(
                request.paymentKey(),
                request.orderId(),
                request.amount()
            );
        } catch (TossPaymentException e) {
            // 토스 결제 실패 시 상태 업데이트
            payment.fail(e.getErrorCode(), e.getMessage());
            paymentRepository.save(payment);
            throw e;
        }
        
        // 4. Payment 상태 업데이트
        payment.approve(request.paymentKey(), tossResponse.method());
        paymentRepository.save(payment);
        
        // 5. 크레딧 충전 (별도 트랜잭션으로 분리 가능)
        Credit credit = getOrCreateCredit(userId);
        Long balanceBefore = credit.getBalance();
        credit.charge(payment.getCreditAmount());
        creditRepository.save(credit);
        
        // 6. 거래 내역 기록
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
     * 
     * ⚠️ 부분 취소 지원
     * 1. 결제 조회 및 검증
     * 2. 크레딧 잔액 검증 (환불 가능 여부)
     * 3. 토스 페이먼츠 취소 API 호출
     * 4. 크레딧 차감
     * 5. 거래 내역 기록
     */
    @Transactional
    public PaymentResponse cancelPayment(UUID userId, String paymentId, PaymentCancelRequest request) {
        // 1. 결제 조회
        Payment payment = paymentRepository.findByIdWithLock(UUID.fromString(paymentId))
            .orElseThrow(() -> new PaymentNotFoundException("결제를 찾을 수 없습니다: " + paymentId));
        
        // 2. 검증
        validatePaymentOwner(payment, userId);
        if (!payment.getStatus().isCancelable()) {
            throw new PaymentNotCancelableException("취소할 수 없는 결제 상태입니다: " + payment.getStatus());
        }
        
        // 3. 취소 금액 결정
        Long cancelAmount = request.cancelAmount() != null 
            ? request.cancelAmount() 
            : payment.getCancelableAmount();
        
        if (cancelAmount > payment.getCancelableAmount()) {
            throw new InvalidCancelAmountException(
                String.format("취소 가능 금액 초과: 요청=%d, 가능=%d", cancelAmount, payment.getCancelableAmount())
            );
        }
        
        // 4. 환불할 크레딧 계산
        Long creditToDeduct = calculateCreditToDeduct(payment, cancelAmount);
        
        // 5. 크레딧 잔액 검증
        Credit credit = creditRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CreditNotFoundException("크레딧 정보를 찾을 수 없습니다."));
        
        if (credit.getBalance() < creditToDeduct) {
            throw new InsufficientCreditException(
                String.format("환불할 크레딧이 부족합니다. 잔액=%d, 필요=%d", credit.getBalance(), creditToDeduct)
            );
        }
        
        // 6. 토스 페이먼츠 취소 API 호출
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
        
        // 7. Payment 상태 업데이트
        payment.cancel(cancelAmount, request.cancelReason());
        paymentRepository.save(payment);
        
        // 8. 크레딧 차감
        Long balanceBefore = credit.getBalance();
        credit.cancelCharge(creditToDeduct);
        creditRepository.save(credit);
        
        // 9. 거래 내역 기록
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
     * 웹훅 처리 (토스 페이먼츠에서 호출)
     */
    @Transactional
    public void handleWebhook(String eventType, JsonNode payload) {
        String paymentKey = payload.path("paymentKey").asText();
        String orderId = payload.path("orderId").asText();
        
        log.info("웹훅 수신: eventType={}, paymentKey={}", eventType, paymentKey);
        
        // 웹훅 로그 저장 (멱등성 검사용)
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
            switch (eventType) {
                case "PAYMENT_STATUS_CHANGED" -> handlePaymentStatusChanged(payload);
                case "VIRTUAL_ACCOUNT_DEPOSIT" -> handleVirtualAccountDeposit(payload);
                default -> log.warn("알 수 없는 웹훅 이벤트: {}", eventType);
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
    
    // === Private 헬퍼 메서드 ===
    
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
    
    private void validatePaymentOwner(Payment payment, UUID userId) {
        if (!payment.getUserId().equals(userId)) {
            throw new UnauthorizedPaymentAccessException("결제 접근 권한이 없습니다.");
        }
    }
    
    private void validatePaymentStatus(Payment payment) {
        if (!payment.isPending()) {
            throw new InvalidPaymentStatusException(
                "결제 승인 불가 상태입니다: " + payment.getStatus()
            );
        }
    }
    
    private void validatePaymentAmount(Payment payment, Long amount) {
        if (!payment.getAmount().equals(amount)) {
            throw new PaymentAmountMismatchException(
                String.format("결제 금액 불일치: 예상=%d, 실제=%d", payment.getAmount(), amount)
            );
        }
    }
    
    private void validateNotExpired(Payment payment) {
        if (payment.getExpiredAt() != null && payment.getExpiredAt().isBefore(LocalDateTime.now())) {
            payment.expire();
            paymentRepository.save(payment);
            throw new PaymentExpiredException("결제 유효 시간이 만료되었습니다.");
        }
    }
    
    private Long calculateCreditToDeduct(Payment payment, Long cancelAmount) {
        // 비례 계산: (취소금액 / 결제금액) * 충전크레딧
        return (cancelAmount * payment.getCreditAmount()) / payment.getAmount();
    }
}
```

### CreditService.java

```java
package com.stolink.backend.domain.payment.service;

import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.*;
import com.stolink.backend.domain.payment.entity.*;
import com.stolink.backend.domain.payment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditService {
    
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    
    /**
     * 크레딧 잔액 조회
     */
    @Transactional(readOnly = true)
    public CreditResponse getCredit(UUID userId) {
        Credit credit = creditRepository.findByUserId(userId)
            .orElseGet(() -> Credit.createForUser(userId));
        return CreditResponse.from(credit);
    }
    
    /**
     * 크레딧 사용
     * - AI 기능 사용 시 호출
     * - 동시성 제어를 위해 비관적 락 사용
     */
    @Transactional
    public CreditResponse useCredit(UUID userId, CreditUseRequest request) {
        Credit credit = creditRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CreditNotFoundException("크레딧 정보를 찾을 수 없습니다."));
        
        Long balanceBefore = credit.getBalance();
        
        // 잔액 검증 및 차감
        credit.use(request.amount());
        creditRepository.save(credit);
        
        // 거래 내역 기록
        CreditTransaction transaction = CreditTransaction.createUseTransaction(
            userId,
            credit.getId(),
            request.amount(),
            balanceBefore,
            request.description(),
            request.referenceType(),
            request.referenceId()
        );
        creditTransactionRepository.save(transaction);
        
        log.info("크레딧 사용: userId={}, amount={}, balance={}", 
            userId, request.amount(), credit.getBalance());
        
        return CreditResponse.from(credit);
    }
    
    /**
     * 크레딧 사용 가능 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean canUseCredit(UUID userId, Long amount) {
        return creditRepository.findByUserId(userId)
            .map(credit -> credit.canUse(amount))
            .orElse(false);
    }
    
    /**
     * 크레딧 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> getTransactions(UUID userId, Pageable pageable) {
        return creditTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(CreditTransactionResponse::from);
    }
    
    /**
     * 특정 타입의 거래 내역 조회
     */
    @Transactional(readOnly = true)
    public Page<CreditTransactionResponse> getTransactionsByType(
            UUID userId, CreditTransactionType type, Pageable pageable) {
        return creditTransactionRepository.findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable)
            .map(CreditTransactionResponse::from);
    }
}
```

### TossPaymentClient.java (토스 API 클라이언트)

```java
package com.stolink.backend.domain.payment.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.stolink.backend.domain.payment.dto.toss.*;
import com.stolink.backend.global.common.exception.TossPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class TossPaymentClient {
    
    private static final String TOSS_API_URL = "https://api.tosspayments.com/v1";
    
    private final WebClient webClient;
    private final String secretKey;
    
    public TossPaymentClient(
            WebClient.Builder webClientBuilder,
            @Value("${toss.payments.secret-key}") String secretKey) {
        this.webClient = webClientBuilder
            .baseUrl(TOSS_API_URL)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
        this.secretKey = secretKey;
    }
    
    /**
     * 결제 승인
     */
    public TossPaymentConfirmResponse confirmPayment(String paymentKey, String orderId, Long amount) {
        log.info("토스 결제 승인 요청: paymentKey={}, orderId={}, amount={}", paymentKey, orderId, amount);
        
        try {
            return webClient.post()
                .uri("/payments/confirm")
                .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                .bodyValue(Map.of(
                    "paymentKey", paymentKey,
                    "orderId", orderId,
                    "amount", amount
                ))
                .retrieve()
                .bodyToMono(TossPaymentConfirmResponse.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("토스 결제 승인 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        }
    }
    
    /**
     * 결제 취소
     */
    public TossPaymentCancelResponse cancelPayment(String paymentKey, String cancelReason, Long cancelAmount) {
        log.info("토스 결제 취소 요청: paymentKey={}, reason={}, amount={}", paymentKey, cancelReason, cancelAmount);
        
        try {
            Map<String, Object> requestBody = cancelAmount != null
                ? Map.of("cancelReason", cancelReason, "cancelAmount", cancelAmount)
                : Map.of("cancelReason", cancelReason);
            
            return webClient.post()
                .uri("/payments/{paymentKey}/cancel", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(TossPaymentCancelResponse.class)
                .block();
        } catch (WebClientResponseException e) {
            log.error("토스 결제 취소 실패: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw parseTossError(e);
        }
    }
    
    /**
     * 결제 조회
     */
    public TossPaymentResponse getPayment(String paymentKey) {
        try {
            return webClient.get()
                .uri("/payments/{paymentKey}", paymentKey)
                .header(HttpHeaders.AUTHORIZATION, buildAuthorizationHeader())
                .retrieve()
                .bodyToMono(TossPaymentResponse.class)
                .block();
        } catch (WebClientResponseException e) {
            throw parseTossError(e);
        }
    }
    
    private String buildAuthorizationHeader() {
        String credentials = secretKey + ":";
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
    
    private TossPaymentException parseTossError(WebClientResponseException e) {
        try {
            JsonNode errorBody = e.getResponseBodyAs(JsonNode.class);
            String code = errorBody.path("code").asText("UNKNOWN_ERROR");
            String message = errorBody.path("message").asText("알 수 없는 오류가 발생했습니다.");
            return new TossPaymentException(code, message, e.getStatusCode().value());
        } catch (Exception parseError) {
            return new TossPaymentException("PARSE_ERROR", e.getMessage(), e.getStatusCode().value());
        }
    }
}
```

---

## 🎮 Controller 설계

### PaymentController.java

```java
package com.stolink.backend.domain.payment.controller;

import com.stolink.backend.domain.payment.dto.request.*;
import com.stolink.backend.domain.payment.dto.response.*;
import com.stolink.backend.domain.payment.service.PaymentService;
import com.stolink.backend.global.common.ApiResponse;
import com.stolink.backend.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    
    private final PaymentService paymentService;
    
    /**
     * 결제 준비 (주문 생성)
     * POST /api/v1/payments/prepare
     */
    @PostMapping("/prepare")
    public ResponseEntity<ApiResponse<PaymentPrepareResponse>> preparePayment(
            @CurrentUser UUID userId,
            @Valid @RequestBody PaymentPrepareRequest request) {
        PaymentPrepareResponse response = paymentService.preparePayment(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 결제 승인 (토스 콜백 후 호출)
     * POST /api/v1/payments/confirm
     */
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @CurrentUser UUID userId,
            @Valid @RequestBody PaymentConfirmRequest request) {
        PaymentResponse response = paymentService.confirmPayment(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 결제 취소
     * POST /api/v1/payments/{paymentId}/cancel
     */
    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @CurrentUser UUID userId,
            @PathVariable String paymentId,
            @Valid @RequestBody PaymentCancelRequest request) {
        PaymentResponse response = paymentService.cancelPayment(userId, paymentId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 결제 내역 조회
     * GET /api/v1/payments
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @CurrentUser UUID userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PaymentResponse> response = paymentService.getPayments(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 결제 상세 조회
     * GET /api/v1/payments/{paymentId}
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(
            @CurrentUser UUID userId,
            @PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPayment(userId, paymentId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 크레딧 패키지 목록 조회 (결제 상품)
     * GET /api/v1/payments/packages
     */
    @GetMapping("/packages")
    public ResponseEntity<ApiResponse<List<CreditPackageResponse>>> getCreditPackages() {
        List<CreditPackageResponse> response = paymentService.getCreditPackages();
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### CreditController.java

```java
package com.stolink.backend.domain.payment.controller;

import com.stolink.backend.domain.payment.dto.request.CreditUseRequest;
import com.stolink.backend.domain.payment.dto.response.*;
import com.stolink.backend.domain.payment.entity.CreditTransactionType;
import com.stolink.backend.domain.payment.service.CreditService;
import com.stolink.backend.global.common.ApiResponse;
import com.stolink.backend.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {
    
    private final CreditService creditService;
    
    /**
     * 크레딧 잔액 조회
     * GET /api/v1/credits
     */
    @GetMapping
    public ResponseEntity<ApiResponse<CreditResponse>> getCredit(@CurrentUser UUID userId) {
        CreditResponse response = creditService.getCredit(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 크레딧 사용 (내부 API - AI 서비스에서 호출)
     * POST /api/v1/credits/use
     */
    @PostMapping("/use")
    public ResponseEntity<ApiResponse<CreditResponse>> useCredit(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreditUseRequest request) {
        CreditResponse response = creditService.useCredit(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    /**
     * 크레딧 사용 가능 여부 확인
     * GET /api/v1/credits/check?amount={amount}
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Boolean>> canUseCredit(
            @CurrentUser UUID userId,
            @RequestParam Long amount) {
        boolean canUse = creditService.canUseCredit(userId, amount);
        return ResponseEntity.ok(ApiResponse.success(canUse));
    }
    
    /**
     * 크레딧 거래 내역 조회
     * GET /api/v1/credits/transactions
     */
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<CreditTransactionResponse>>> getTransactions(
            @CurrentUser UUID userId,
            @RequestParam(required = false) CreditTransactionType type,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CreditTransactionResponse> response = type != null
            ? creditService.getTransactionsByType(userId, type, pageable)
            : creditService.getTransactions(userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

### WebhookController.java

```java
package com.stolink.backend.domain.payment.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.stolink.backend.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {
    
    private final PaymentService paymentService;
    
    /**
     * 토스 페이먼츠 웹훅 수신
     * POST /api/v1/webhooks/toss
     */
    @PostMapping("/toss")
    public ResponseEntity<Void> handleTossWebhook(
            @RequestHeader("TossPayments-Signature") String signature,
            @RequestBody JsonNode payload) {
        
        // TODO: 시그니처 검증
        // verifySignature(signature, payload);
        
        String eventType = payload.path("eventType").asText();
        log.info("토스 웹훅 수신: eventType={}", eventType);
        
        paymentService.handleWebhook(eventType, payload);
        
        return ResponseEntity.ok().build();
    }
}
```

---

## 🛡️ 예외 처리

### PaymentExceptions.java

```java
package com.stolink.backend.domain.payment.exception;

// 결제 관련 예외 클래스들

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String message) {
        super(message);
    }
}

public class DuplicatePaymentException extends RuntimeException {
    public DuplicatePaymentException(String message) {
        super(message);
    }
}

public class InvalidPaymentStatusException extends RuntimeException {
    public InvalidPaymentStatusException(String message) {
        super(message);
    }
}

public class PaymentAmountMismatchException extends RuntimeException {
    public PaymentAmountMismatchException(String message) {
        super(message);
    }
}

public class PaymentExpiredException extends RuntimeException {
    public PaymentExpiredException(String message) {
        super(message);
    }
}

public class PaymentNotCancelableException extends RuntimeException {
    public PaymentNotCancelableException(String message) {
        super(message);
    }
}

public class InvalidCancelAmountException extends RuntimeException {
    public InvalidCancelAmountException(String message) {
        super(message);
    }
}

public class UnauthorizedPaymentAccessException extends RuntimeException {
    public UnauthorizedPaymentAccessException(String message) {
        super(message);
    }
}

// 크레딧 관련 예외
public class CreditNotFoundException extends RuntimeException {
    public CreditNotFoundException(String message) {
        super(message);
    }
}

public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(String message) {
        super(message);
    }
}

// 토스 페이먼츠 예외
public class TossPaymentException extends RuntimeException {
    private final String errorCode;
    private final int statusCode;
    
    public TossPaymentException(String errorCode, String message, int statusCode) {
        super(message);
        this.errorCode = errorCode;
        this.statusCode = statusCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
```

### GlobalExceptionHandler에 추가

```java
// GlobalExceptionHandler.java에 추가

@ExceptionHandler(PaymentNotFoundException.class)
public ResponseEntity<ApiResponse<Void>> handlePaymentNotFound(PaymentNotFoundException e) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiResponse.error("PAYMENT_NOT_FOUND", e.getMessage()));
}

@ExceptionHandler(DuplicatePaymentException.class)
public ResponseEntity<ApiResponse<Void>> handleDuplicatePayment(DuplicatePaymentException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiResponse.error("DUPLICATE_PAYMENT", e.getMessage()));
}

@ExceptionHandler(InvalidPaymentStatusException.class)
public ResponseEntity<ApiResponse<Void>> handleInvalidPaymentStatus(InvalidPaymentStatusException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("INVALID_PAYMENT_STATUS", e.getMessage()));
}

@ExceptionHandler(PaymentAmountMismatchException.class)
public ResponseEntity<ApiResponse<Void>> handlePaymentAmountMismatch(PaymentAmountMismatchException e) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("PAYMENT_AMOUNT_MISMATCH", e.getMessage()));
}

@ExceptionHandler(PaymentExpiredException.class)
public ResponseEntity<ApiResponse<Void>> handlePaymentExpired(PaymentExpiredException e) {
    return ResponseEntity.status(HttpStatus.GONE)
        .body(ApiResponse.error("PAYMENT_EXPIRED", e.getMessage()));
}

@ExceptionHandler(InsufficientCreditException.class)
public ResponseEntity<ApiResponse<Void>> handleInsufficientCredit(InsufficientCreditException e) {
    return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
        .body(ApiResponse.error("INSUFFICIENT_CREDIT", e.getMessage()));
}

@ExceptionHandler(TossPaymentException.class)
public ResponseEntity<ApiResponse<Void>> handleTossPaymentException(TossPaymentException e) {
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ApiResponse.error("TOSS_PAYMENT_ERROR", e.getMessage()));
}
```

---

## 🔒 데이터 정합성 및 동시성 제어

### 1. 낙관적 락 (Optimistic Locking)

```java
// Credit, Payment 엔티티에 @Version 필드 추가됨
// 동시 수정 시 OptimisticLockingFailureException 발생
```

### 2. 비관적 락 (Pessimistic Locking)

```java
// CreditRepository.java
public interface CreditRepository extends JpaRepository<Credit, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credit c WHERE c.userId = :userId")
    Optional<Credit> findByUserIdWithLock(@Param("userId") UUID userId);
}

// PaymentRepository.java
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") String orderId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") UUID id);
}
```

### 3. 멱등성 보장

```java
// 1. 주문 생성 시 idempotencyKey로 중복 방지
// 2. 웹훅 처리 시 paymentKey + eventType으로 중복 방지
// 3. 결제 승인 시 상태 체크로 중복 승인 방지

// PaymentService.java
@Transactional
public PaymentResponse confirmPayment(UUID userId, PaymentConfirmRequest request) {
    Payment payment = paymentRepository.findByOrderIdWithLock(request.orderId())
        .orElseThrow(() -> new PaymentNotFoundException("..."));
    
    // 이미 처리된 결제인 경우 멱등성 유지
    if (payment.isCompleted()) {
        log.info("이미 완료된 결제: orderId={}", request.orderId());
        return PaymentResponse.from(payment);
    }
    
    // ... 결제 처리
}
```

### 4. 트랜잭션 분리 (필요 시)

```java
// 결제 승인과 크레딧 충전을 분리해야 하는 경우
// (토스 승인 성공 후 크레딧 충전 실패 시 롤백 문제)

@Service
@RequiredArgsConstructor
public class PaymentFacadeService {
    
    private final PaymentService paymentService;
    private final CreditService creditService;
    private final TransactionTemplate transactionTemplate;
    
    public PaymentResponse confirmPaymentWithCredit(UUID userId, PaymentConfirmRequest request) {
        // 1. 토스 결제 승인 (외부 API 호출)
        TossPaymentConfirmResponse tossResponse = tossPaymentClient.confirmPayment(...);
        
        // 2. 내부 트랜잭션으로 상태 업데이트 + 크레딧 충전
        return transactionTemplate.execute(status -> {
            try {
                Payment payment = paymentRepository.findByOrderIdWithLock(request.orderId())
                    .orElseThrow(...);
                
                payment.approve(request.paymentKey(), tossResponse.method());
                paymentRepository.save(payment);
                
                // 크레딧 충전
                creditService.chargeCredit(userId, payment.getCreditAmount(), payment.getId());
                
                return PaymentResponse.from(payment);
            } catch (Exception e) {
                // 내부 처리 실패 시 토스 결제 취소 시도
                log.error("결제 승인 후 처리 실패, 취소 시도: {}", e.getMessage());
                tossPaymentClient.cancelPayment(request.paymentKey(), "시스템 오류", null);
                throw e;
            }
        });
    }
}
```

---

## ⚠️ 엣지케이스 처리

### 1. 중복 결제 방지

```java
// 결제 준비 시
if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
    throw new DuplicatePaymentException("이미 처리 중인 결제가 있습니다.");
}

// 추가: 최근 N분 내 동일 금액 결제 경고
boolean recentDuplicate = paymentRepository.existsByUserIdAndAmountAndStatusAndRequestedAtAfter(
    userId, amount, PaymentStatus.DONE, LocalDateTime.now().minusMinutes(5)
);
if (recentDuplicate) {
    log.warn("최근 동일 금액 결제 감지: userId={}, amount={}", userId, amount);
    // 프론트엔드에 경고 응답
}
```

### 2. 결제 만료 처리

```java
// 스케줄러로 만료된 결제 처리
@Scheduled(fixedRate = 60000)  // 1분마다
@Transactional
public void expireOldPayments() {
    List<Payment> expiredPayments = paymentRepository.findByStatusInAndExpiredAtBefore(
        List.of(PaymentStatus.PENDING, PaymentStatus.READY),
        LocalDateTime.now()
    );
    
    for (Payment payment : expiredPayments) {
        payment.expire();
        log.info("결제 만료 처리: orderId={}", payment.getOrderId());
    }
    
    paymentRepository.saveAll(expiredPayments);
}
```

### 3. 결제창 이탈 후 복귀

```java
// 동일 orderId로 재시도 허용
@GetMapping("/resume/{orderId}")
public ResponseEntity<ApiResponse<PaymentPrepareResponse>> resumePayment(
        @CurrentUser UUID userId,
        @PathVariable String orderId) {
    
    Payment payment = paymentRepository.findByOrderId(orderId)
        .orElseThrow(() -> new PaymentNotFoundException("..."));
    
    if (!payment.isPending()) {
        throw new InvalidPaymentStatusException("재개할 수 없는 결제 상태입니다.");
    }
    
    if (payment.getExpiredAt().isBefore(LocalDateTime.now())) {
        throw new PaymentExpiredException("결제 유효 시간이 만료되었습니다.");
    }
    
    // 기존 결제 정보 반환
    return ResponseEntity.ok(ApiResponse.success(
        new PaymentPrepareResponse(
            payment.getOrderId(),
            payment.getOrderName(),
            payment.getAmount(),
            payment.getCreditAmount(),
            generateCustomerKey(userId),
            buildSuccessUrl(orderId),
            buildFailUrl(orderId)
        )
    ));
}
```

### 4. 부분 취소 처리

```java
// Payment.cancel() 메서드에서 처리
// DONE -> PARTIAL_CANCELED (부분 취소)
// PARTIAL_CANCELED -> CANCELED (전액 취소)

// 부분 취소 시 크레딧 비례 차감
Long creditToDeduct = (cancelAmount * payment.getCreditAmount()) / payment.getAmount();
```

### 5. 웹훅 재시도 처리

```java
// 웹훅 처리 실패 시 재시도 로직
@Scheduled(fixedRate = 300000)  // 5분마다
@Transactional
public void retryFailedWebhooks() {
    List<PaymentWebhookLog> failedLogs = webhookLogRepository.findByStatusAndRetryCountLessThan(
        WebhookStatus.FAILED, 3  // 최대 3회 재시도
    );
    
    for (PaymentWebhookLog log : failedLogs) {
        try {
            paymentService.handleWebhook(log.getEventType(), log.getRequestBody());
            log.markAsProcessed();
        } catch (Exception e) {
            log.incrementRetryCount();
            log.setNextRetryAt(LocalDateTime.now().plusMinutes(5 * log.getRetryCount()));
        }
        webhookLogRepository.save(log);
    }
}
```

### 6. 네트워크 오류 복구

```java
// 토스 API 호출 시 재시도 정책
@Bean
public WebClient tossWebClient() {
    return WebClient.builder()
        .baseUrl(TOSS_API_URL)
        .filter(ExchangeFilterFunction.ofRequestProcessor(request -> {
            // Retry 정책
            return Mono.just(request)
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                    .filter(ex -> ex instanceof WebClientRequestException)
                    .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
        }))
        .build();
}
```

### 7. 잔액 부족 시 결제 유도

```java
// CreditService.useCredit에서 잔액 부족 시
public CreditResponse useCredit(UUID userId, CreditUseRequest request) {
    Credit credit = creditRepository.findByUserIdWithLock(userId)
        .orElseThrow(() -> new CreditNotFoundException("..."));
    
    if (!credit.canUse(request.amount())) {
        Long shortage = request.amount() - credit.getBalance();
        throw new InsufficientCreditException(
            String.format("크레딧이 %d 부족합니다. 충전이 필요합니다.", shortage)
        );
    }
    
    // ... 정상 처리
}
```

---

## 📦 Repository 인터페이스

```java
// PaymentRepository.java
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    
    Optional<Payment> findByOrderId(String orderId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.orderId = :orderId")
    Optional<Payment> findByOrderIdWithLock(@Param("orderId") String orderId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") UUID id);
    
    boolean existsByIdempotencyKey(String idempotencyKey);
    
    Page<Payment> findByUserIdOrderByRequestedAtDesc(UUID userId, Pageable pageable);
    
    List<Payment> findByStatusInAndExpiredAtBefore(List<PaymentStatus> statuses, LocalDateTime expiredAt);
    
    boolean existsByUserIdAndAmountAndStatusAndRequestedAtAfter(
        UUID userId, Long amount, PaymentStatus status, LocalDateTime after);
}

// CreditRepository.java
public interface CreditRepository extends JpaRepository<Credit, UUID> {
    
    Optional<Credit> findByUserId(UUID userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credit c WHERE c.userId = :userId")
    Optional<Credit> findByUserIdWithLock(@Param("userId") UUID userId);
}

// CreditTransactionRepository.java
public interface CreditTransactionRepository extends JpaRepository<CreditTransaction, UUID> {
    
    Page<CreditTransaction> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<CreditTransaction> findByUserIdAndTypeOrderByCreatedAtDesc(
        UUID userId, CreditTransactionType type, Pageable pageable);
}
```

---

## ⚙️ 설정

### application.yml

```yaml
toss:
  payments:
    secret-key: ${TOSS_SECRET_KEY:test_sk_xxxx}
    client-key: ${TOSS_CLIENT_KEY:test_ck_xxxx}
    webhook-secret: ${TOSS_WEBHOOK_SECRET:webhook_secret}
    
# 크레딧 패키지 설정 (하드코딩 또는 DB)
credit:
  packages:
    - id: 1
      name: "크레딧 100점"
      price: 1000
      credit-amount: 100
      bonus-credit: 0
    - id: 2
      name: "크레딧 500점 (+50 보너스)"
      price: 5000
      credit-amount: 500
      bonus-credit: 50
      is-popular: true
    - id: 3
      name: "크레딧 1000점 (+150 보너스)"
      price: 10000
      credit-amount: 1000
      bonus-credit: 150
```

---

## 🧪 테스트 시나리오

### 1. 정상 플로우
- [ ] 결제 준비 → 결제창 → 승인 → 크레딧 충전
- [ ] 결제 내역 조회
- [ ] 크레딧 사용 → 잔액 차감

### 2. 취소 플로우
- [ ] 전액 취소 → 크레딧 차감
- [ ] 부분 취소 → 비례 크레딧 차감

### 3. 엣지케이스
- [ ] 중복 결제 시도 → 409 Conflict
- [ ] 결제 만료 → 410 Gone
- [ ] 금액 변조 → 400 Bad Request
- [ ] 잔액 부족 취소 → 400 Bad Request
- [ ] 동시 크레딧 사용 → 낙관적 락 예외 처리

### 4. 웹훅 테스트
- [ ] 정상 웹훅 처리
- [ ] 중복 웹훅 무시 (멱등성)
- [ ] 실패 웹훅 재시도

---

## 📁 파일 구조

```
src/main/java/com/stolink/backend/domain/payment/
├── controller/
│   ├── PaymentController.java
│   ├── CreditController.java
│   └── WebhookController.java
├── service/
│   ├── PaymentService.java
│   ├── CreditService.java
│   ├── CreditPackageService.java
│   └── PaymentScheduler.java
├── repository/
│   ├── PaymentRepository.java
│   ├── CreditRepository.java
│   ├── CreditTransactionRepository.java
│   └── PaymentWebhookLogRepository.java
├── entity/
│   ├── Payment.java
│   ├── PaymentStatus.java
│   ├── Credit.java
│   ├── CreditTransaction.java
│   ├── CreditTransactionType.java
│   └── PaymentWebhookLog.java
├── dto/
│   ├── request/
│   │   ├── PaymentPrepareRequest.java
│   │   ├── PaymentConfirmRequest.java
│   │   ├── PaymentCancelRequest.java
│   │   └── CreditUseRequest.java
│   ├── response/
│   │   ├── PaymentPrepareResponse.java
│   │   ├── PaymentResponse.java
│   │   ├── CreditResponse.java
│   │   ├── CreditTransactionResponse.java
│   │   └── CreditPackageResponse.java
│   └── toss/
│       ├── TossPaymentConfirmResponse.java
│       ├── TossPaymentCancelResponse.java
│       └── TossPaymentResponse.java
├── client/
│   └── TossPaymentClient.java
├── exception/
│   ├── PaymentNotFoundException.java
│   ├── DuplicatePaymentException.java
│   ├── ... (기타 예외 클래스들)
│   └── TossPaymentException.java
└── config/
    └── PaymentConfig.java
```

---

## 🚨 보안 고려사항

1. **시크릿 키 관리**: 환경 변수 또는 Vault 사용
2. **웹훅 시그니처 검증**: 토스에서 제공하는 시그니처 검증 필수
3. **금액 검증**: 클라이언트 전송 금액과 서버 저장 금액 일치 여부 확인
4. **사용자 인증**: 모든 API에 인증 필수 (`@CurrentUser`)
5. **Rate Limiting**: 결제 API에 요청 제한 적용
6. **로깅**: 민감 정보 (카드 번호 등) 로깅 금지

---

## 📝 구현 체크리스트

- [ ] Entity 생성 (Payment, Credit, CreditTransaction)
- [ ] Repository 생성
- [ ] TossPaymentClient 구현
- [ ] PaymentService 구현
- [ ] CreditService 구현
- [ ] Controller 구현
- [ ] 예외 처리 추가
- [ ] GlobalExceptionHandler 업데이트
- [ ] application.yml 설정
- [ ] 단위 테스트
- [ ] 통합 테스트
- [ ] API 문서 작성

---

**작성일**: 2025년 1월
**버전**: 1.0
