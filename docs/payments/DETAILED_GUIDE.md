# 토스 페이먼츠 크레딧 결제 시스템 - 완벽 가이드

> **초보자도 이해할 수 있는 상세한 설명서**

---

## 📚 목차
1. [시스템 개요 - 쉬운 설명](#시스템-개요)
2. [전체 흐름 이해하기](#전체-흐름-이해하기)
3. [핵심 개념 상세 설명](#핵심-개념-상세-설명)
4. [코드 상세 분석](#코드-상세-분석)
5. [실제 사용 시나리오](#실제-사용-시나리오)
6. [문제 해결 가이드](#문제-해결-가이드)

---

## 시스템 개요

### 🎯 이 시스템이 하는 일

간단히 말하면, **"사용자가 돈을 내고 크레딧을 사서, 그 크레딧으로 책을 읽는 시스템"**입니다.

```
사용자 지갑 (현금)
    ↓
토스 페이먼츠로 결제 (5,000원)
    ↓
우리 시스템 (550 크레딧 지급)
    ↓
책 챕터 1개 읽기 (5 크레딧 차감)
    ↓
잔액: 545 크레딧
```

### 왜 이렇게 만들었나요?

#### 1️⃣ 직접 결제하지 않는 이유
```
❌ 나쁜 방법: 챕터 읽을 때마다 100원씩 결제
   - 매번 결제 수수료 발생
   - 사용자 불편
   - 결제 실패 시 챕터 못 읽음

✅ 좋은 방법: 미리 크레딧 충전
   - 한 번 결제로 여러 번 사용
   - 빠른 속도
   - 사용자 편의성
```

#### 2️⃣ 실제 현금처럼 관리
```
은행 계좌     →  크레딧 시스템
입금          →  충전 (결제)
출금          →  사용 (챕터 읽기)
잔액 조회     →  크레딧 조회
거래 내역     →  크레딧 거래 내역
```

---

## 전체 흐름 이해하기

### 🔄 결제부터 사용까지 전 과정

#### Phase 1: 사용자가 크레딧 구매하기

```
┌─────────────┐
│   사용자    │ "크레딧 500점 살래!"
└──────┬──────┘
       │
       ↓
┌──────────────────────────────────────────┐
│  Step 1: 결제 준비                        │
│  POST /api/v1/payments/prepare           │
│                                           │
│  우리 서버가 하는 일:                      │
│  1. 주문 ID 생성 (SL-abc123...)          │
│  2. DB에 결제 정보 저장 (상태: PENDING)   │
│  3. 30분 후 만료 시간 설정                │
│                                           │
│  응답:                                    │
│  {                                        │
│    "orderId": "SL-abc123...",            │
│    "amount": 5000,  // 5천원             │
│    "creditAmount": 550  // 550 크레딧    │
│  }                                        │
└──────┬───────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────┐
│  Step 2: 토스 결제창 띄우기               │
│  (프론트엔드가 하는 일)                   │
│                                           │
│  사용자가 토스 앱에서:                     │
│  - 카드 정보 입력                         │
│  - 비밀번호 입력                          │
│  - 결제 완료 버튼 클릭                    │
└──────┬───────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────┐
│  Step 3: 결제 승인                        │
│  POST /api/v1/payments/confirm           │
│                                           │
│  우리 서버가 하는 일:                      │
│  1. 토스 서버에 "진짜 결제됐어?" 확인     │
│  2. 토스: "네! paymentKey: xxx"          │
│  3. DB의 결제 상태 변경 (DONE)           │
│  4. 사용자 크레딧 550점 충전 ✅           │
│  5. 거래 내역 기록                        │
│                                           │
│  DB 변화:                                 │
│  credits 테이블:                          │
│    balance: 0 → 550                      │
│    total_charged: 0 → 550                │
│                                           │
│  credit_transactions 테이블에 기록:       │
│    "550 크레딧 충전됨"                    │
└───────────────────────────────────────────┘
```

#### Phase 2: 사용자가 챕터 읽기

```
┌─────────────┐
│   사용자    │ "챕터 1 읽을래!"
└──────┬──────┘
       │
       ↓
┌──────────────────────────────────────────┐
│  Step 1: 읽기 전 확인 (선택사항)          │
│  GET /api/v1/credits/check?amount=5      │
│                                           │
│  서버: "현재 잔액 550 크레딧, 5 크레딧    │
│         필요 → 사용 가능!"                │
│                                           │
│  응답: { "data": true }                   │
└──────┬───────────────────────────────────┘
       │
       ↓
┌──────────────────────────────────────────┐
│  Step 2: 크레딧 차감                      │
│  POST /api/v1/credits/use                │
│  {                                        │
│    "amount": 5,                          │
│    "description": "챕터 1 읽기",         │
│    "referenceType": "CHAPTER",           │
│    "referenceId": "chapter-uuid-1"       │
│  }                                        │
│                                           │
│  우리 서버가 하는 일:                      │
│  1. DB에서 사용자 크레딧 조회 (잠금!)    │
│     - 다른 요청 대기시킴 (동시성 제어)   │
│                                           │
│  2. 잔액 확인                             │
│     if (balance < 5) {                   │
│       "잔액 부족!" 에러                   │
│       return;                             │
│     }                                     │
│                                           │
│  3. 크레딧 차감                           │
│     balance: 550 → 545                   │
│     total_used: 0 → 5                    │
│                                           │
│  4. 거래 내역 기록                        │
│     "챕터 1 읽기로 5 크레딧 사용"         │
│                                           │
│  5. 잠금 해제                             │
└───────────────────────────────────────────┘
```

---

## 핵심 개념 상세 설명

### 💡 1. 동시성 제어 (왜 필요한가?)

#### 문제 상황
```
사용자 잔액: 10 크레딧

시간 →
사용자A와 사용자B가 동시에 각각 5 크레딧씩 사용 시도

┌─────────────┬─────────────┐
│  사용자 A   │  사용자 B   │
├─────────────┼─────────────┤
│ 1. 잔액 조회│ 1. 잔액 조회│
│    → 10     │    → 10     │
├─────────────┼─────────────┤
│ 2. 5 차감   │ 2. 5 차감   │
│    10-5=5   │    10-5=5   │
├─────────────┼─────────────┤
│ 3. 저장     │ 3. 저장     │
│    → 5      │    → 5      │
└─────────────┴─────────────┘

결과: 잔액 5
문제: 10 크레딧 사용했는데 잔액이 5? (정답: 0이어야 함!)
```

#### 해결 방법: 비관적 락

```java
// 락 없이 (위험!)
Credit credit = creditRepository.findByUserId(userId);
credit.use(5);
creditRepository.save(credit);

// 비관적 락 적용 (안전!)
@Lock(LockModeType.PESSIMISTIC_WRITE)
Credit credit = creditRepository.findByUserIdWithLock(userId);
// ↑ 이 순간부터 다른 요청은 대기!

credit.use(5);
creditRepository.save(credit);
// ↑ 여기서 락 해제, 다음 요청 처리 가능
```

**실제 동작**:
```
시간 →

사용자A: 락 획득 ━━━━━━━━━━━━ 처리 완료 (잔액: 10→5) ━━ 락 해제
사용자B:              대기...                        ━━━━━━━━━━━ 락 획득 ━━━━ 처리 (잔액: 5→0) ━━ 완료

최종 잔액: 0 (정확!)
```

---

### 💡 2. 멱등성 (같은 요청을 여러 번 해도 안전)

#### 문제 상황
```
사용자가 "결제 승인" 버튼 두 번 클릭
    ↓
네트워크 느려서 타임아웃
    ↓
자동 재시도
    ↓
중복 결제? 크레딧 2배 충전?
```

#### 해결 방법: 멱등성 키

```java
// 1. 결제 준비 시 고유 키 생성
String idempotencyKey = userId + ":" + orderId + ":" + timestamp;
// 예: "user123:SL-abc:1704355200" (분 단위로 같음)

// 2. DB에 저장 전 체크
if (paymentRepository.existsByIdempotencyKey(idempotencyKey)) {
    // 이미 처리 중인 결제!
    throw new DuplicatePaymentException("중복 결제");
}

// 3. DB에 UNIQUE 제약조건
// → 만약 동시에 2개 요청 와도, DB가 하나만 허용
```

**실제 동작**:
```
요청 1: idempotencyKey = "user123:SL-abc:17043552"
   → DB 저장 성공 ✅

요청 2 (중복): idempotencyKey = "user123:SL-abc:17043552"
   → DB가 거부 (UNIQUE 제약조건) ❌
   → 에러 반환: "이미 처리 중"
```

---

### 💡 3. 트랜잭션 (모 아니면 도)

#### 개념
```
은행 이체를 생각해보세요:

1. A 계좌에서 10만원 출금
2. B 계좌에 10만원 입금

만약 1번만 성공하고 2번 실패하면?
→ 돈이 사라짐! (큰 문제!)

트랜잭션: 둘 다 성공하거나, 둘 다 실패
```

#### 우리 시스템에서
```java
@Transactional  // ← 이게 마법의 주문!
public PaymentResponse confirmPayment(...) {
    // 1. 토스 API 승인 (외부라서 롤백 불가)
    TossPaymentConfirmResponse toss = tossPaymentClient.confirmPayment(...);

    // 2. Payment 상태 변경
    payment.approve(...);
    paymentRepository.save(payment);

    // 3. 크레딧 충전
    credit.charge(550);
    creditRepository.save(credit);

    // 4. 거래 내역 기록
    creditTransactionRepository.save(transaction);

    // 만약 3번이나 4번에서 에러 발생?
    // → 2번, 3번, 4번 모두 취소! (롤백)
    // → Payment 상태도 원래대로
}
```

**실제 시나리오**:
```
시나리오 1: 모두 성공
   토스 승인 ✅
   → Payment 저장 ✅
   → Credit 충전 ✅
   → Transaction 기록 ✅
   → 커밋! 모두 DB에 반영

시나리오 2: 중간에 에러
   토스 승인 ✅
   → Payment 저장 ✅
   → Credit 충전 ❌ (DB 연결 끊김)
   → 롤백! Payment 저장도 취소
   → 사용자에게 에러 메시지
   → (토스 승인은 별도로 취소 처리 필요)
```

---

### 💡 4. 거래 내역 추적 (감사 추적)

#### 왜 필요한가?
```
고객: "제 크레딧 어디갔어요? 분명 1000점이었는데!"
우리: "거래 내역을 확인해보겠습니다..."

credit_transactions 테이블 조회:
┌───────────┬─────────┬──────────┬──────────┬──────────────┐
│ 시간      │ 타입    │ 금액     │ 전 잔액  │ 후 잔액      │
├───────────┼─────────┼──────────┼──────────┼──────────────┤
│ 12:00:00  │ CHARGE  │ +1000    │ 0        │ 1000         │
│ 12:05:00  │ USE     │ -5       │ 1000     │ 995          │
│ 12:10:00  │ USE     │ -5       │ 995      │ 990          │
│ 12:15:00  │ USE     │ -5       │ 990      │ 985          │
│ ...       │ ...     │ ...      │ ...      │ ...          │
│ 14:00:00  │ USE     │ -5       │ 10       │ 5            │
│ 14:05:00  │ USE     │ -5       │ 5        │ 0            │
└───────────┴─────────┴──────────┴──────────┴──────────────┘

우리: "200개 챕터 읽으셨네요! (200 × 5 = 1000 크레딧)"
고객: "아... 맞네요! 감사합니다!"
```

#### 코드 분석
```java
public static CreditTransaction createUseTransaction(
        UUID userId,
        UUID creditId,
        Long amount,             // 5
        Long balanceBefore,      // 1000
        String description,      // "챕터 1 읽기"
        String referenceType,    // "CHAPTER"
        String referenceId) {    // "chapter-uuid-1"

    return CreditTransaction.builder()
        .userId(userId)
        .creditId(creditId)
        .type(CreditTransactionType.USE)
        .amount(-amount)          // -5 (음수로 저장!)
        .balanceBefore(balanceBefore)
        .balanceAfter(balanceBefore - amount)  // 1000 - 5 = 995
        .description(description)
        .referenceType(referenceType)  // 나중에 "CHAPTER"로 필터 가능
        .referenceId(referenceId)      // 어떤 챕터인지 추적
        .build();
}
```

---

## 코드 상세 분석

### 📝 Credit Entity - 크레딧 도메인 모델

```java
@Entity
@Table(name = "credits")
public class Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;                    // PK

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;                // 사용자 ID (1:1 관계)

    @Column(nullable = false)
    private Long balance;               // 현재 잔액

    @Column(name = "total_charged")
    private Long totalCharged;          // 총 충전 금액

    @Column(name = "total_used")
    private Long totalUsed;             // 총 사용 금액

    @Version
    private Long version;               // 낙관적 락용

    // ═══════════════════════════════════
    // 비즈니스 로직 (도메인 주도 설계)
    // ═══════════════════════════════════

    /**
     * 크레딧 충전
     * - 결제 승인 후 호출됨
     * - 예: charge(550) → balance += 550
     */
    public void charge(Long amount) {
        // 1단계: 입력 검증
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        // 2단계: 잔액 증가
        this.balance += amount;
        this.totalCharged += amount;

        // 결과:
        // balance: 0 → 550
        // totalCharged: 0 → 550
    }

    /**
     * 크레딧 사용
     * - 챕터 읽기 시 호출됨
     * - 예: use(5) → balance -= 5
     */
    public void use(Long amount) {
        // 1단계: 입력 검증
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("사용 금액은 0보다 커야 합니다.");
        }

        // 2단계: 잔액 확인 (가장 중요!)
        if (this.balance < amount) {
            throw new IllegalStateException(
                String.format(
                    "크레딧 잔액이 부족합니다. 현재: %d, 필요: %d",
                    this.balance,
                    amount
                )
            );
            // 예: 현재 3 크레딧, 5 크레딧 필요
            // → "크레딧 잔액이 부족합니다. 현재: 3, 필요: 5"
        }

        // 3단계: 잔액 차감
        this.balance -= amount;
        this.totalUsed += amount;

        // 결과:
        // balance: 550 → 545
        // totalUsed: 0 → 5
    }

    /**
     * 사용 가능 여부 확인
     * - 챕터 읽기 전 미리 확인용
     */
    public boolean canUse(Long amount) {
        return this.balance >= amount;

        // 예:
        // canUse(5) → balance(10) >= 5 → true
        // canUse(15) → balance(10) >= 15 → false
    }

    /**
     * 정적 팩토리 메서드
     * - 새 사용자 가입 시 호출
     */
    public static Credit createForUser(UUID userId) {
        return Credit.builder()
            .userId(userId)
            .balance(0L)         // 초기 잔액 0
            .totalCharged(0L)
            .totalUsed(0L)
            .build();
    }
}
```

**왜 이렇게 설계했나요?**

```
❌ 나쁜 설계: Service에 모든 로직
   CreditService {
     void use(userId, amount) {
       credit = find(userId);
       if (credit.balance < amount) throw ...;
       credit.balance -= amount;
       save(credit);
     }
   }
   → Service가 너무 많은 책임
   → 테스트 어려움

✅ 좋은 설계: Entity에 비즈니스 로직
   Credit {
     void use(amount) {
       if (this.balance < amount) throw ...;
       this.balance -= amount;
     }
   }
   CreditService {
     void use(userId, amount) {
       credit = find(userId);
       credit.use(amount);  // ← 위임!
       save(credit);
     }
   }
   → Entity가 자기 데이터 관리
   → Service는 조합만 담당
   → 테스트 쉬움
```

---

### 📝 PaymentService - 결제 승인 로직

```java
@Service
@RequiredArgsConstructor
@Transactional  // ← 모든 메서드에 트랜잭션
public class PaymentService {

    // 의존성 주입
    private final PaymentRepository paymentRepository;
    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final TossPaymentClient tossPaymentClient;

    /**
     * 결제 승인 - 가장 중요한 메서드!
     *
     * 호출 시점: 사용자가 토스 앱에서 결제 완료 후
     *
     * 전체 과정:
     * 1. DB에서 주문 찾기 (락 걸고)
     * 2. 검증 (주인 확인, 상태 확인, 금액 확인, 만료 확인)
     * 3. 토스 API 호출해서 진짜 결제됐는지 확인
     * 4. Payment 상태 업데이트
     * 5. Credit 충전
     * 6. 거래 내역 기록
     */
    @Transactional
    public PaymentResponse confirmPayment(
            UUID userId,                      // 누가?
            PaymentConfirmRequest request) {  // 어떤 결제?

        // ═══════════════════════════════════
        // Step 1: 주문 조회 (비관적 락!)
        // ═══════════════════════════════════
        Payment payment = paymentRepository.findByOrderIdWithLock(request.orderId())
            .orElseThrow(() -> new PaymentNotFoundException(
                "주문을 찾을 수 없습니다: " + request.orderId()
            ));
        // ↑ 이 순간부터 이 Payment는 잠금!
        //   다른 요청은 대기

        // ═══════════════════════════════════
        // Step 2: 검증
        // ═══════════════════════════════════

        // 2-1. 본인 확인
        validatePaymentOwner(payment, userId);
        // if (payment.getUserId() != userId) → 에러!

        // 2-2. 상태 확인
        validatePaymentStatus(payment);
        // if (payment.status != PENDING) → 에러!

        // 2-3. 금액 확인 (변조 방지!)
        validatePaymentAmount(payment, request.amount());
        // if (payment.amount != request.amount) → 에러!

        // 2-4. 만료 확인
        validateNotExpired(payment);
        // if (payment.expiredAt < now) → 에러!

        // 2-5. 멱등성 체크 (이미 완료된 결제?)
        if (payment.isCompleted()) {
            log.info("이미 완료된 결제: {}", request.orderId());
            return PaymentResponse.from(payment);  // 기존 결과 반환
        }

        // ═══════════════════════════════════
        // Step 3: 토스 API 호출 (중요!)
        // ═══════════════════════════════════
        TossPaymentConfirmResponse tossResponse;
        try {
            tossResponse = tossPaymentClient.confirmPayment(
                request.paymentKey(),  // "toss_payment_abc123..."
                request.orderId(),     // "SL-abc123..."
                request.amount()       // 5000
            );
            // ↑ 토스 서버에 "이 결제 진짜 됐어?" 물어봄
            // 토스: "네! 여기 영수증 (paymentKey, method 등)"

        } catch (TossPaymentException e) {
            // 토스가 "안 됐어!" 라고 하면?
            payment.fail(e.getErrorCode(), e.getMessage());
            // 상태: PENDING → FAILED
            paymentRepository.save(payment);
            throw e;  // 사용자에게도 에러 전달
        }

        // ═══════════════════════════════════
        // Step 4: Payment 업데이트
        // ═══════════════════════════════════
        payment.approve(
            request.paymentKey(),       // 토스에서 받은 키
            tossResponse.method()       // "카드"
        );
        paymentRepository.save(payment);
        // 상태: PENDING → DONE ✅

        // ═══════════════════════════════════
        // Step 5: 크레딧 충전 (핵심!)
        // ═══════════════════════════════════

        // 5-1. 크레딧 조회 (없으면 생성)
        Credit credit = getOrCreateCredit(userId);
        // 신규 사용자면 balance=0 생성

        // 5-2. 현재 잔액 기록 (거래 내역용)
        Long balanceBefore = credit.getBalance();
        // 예: 0

        // 5-3. 충전!
        credit.charge(payment.getCreditAmount());
        // 550 크레딧 충전
        // balance: 0 → 550

        creditRepository.save(credit);

        // ═══════════════════════════════════
        // Step 6: 거래 내역 기록
        // ═══════════════════════════════════
        CreditTransaction transaction = CreditTransaction.createChargeTransaction(
            userId,
            credit.getId(),
            payment.getId(),
            payment.getCreditAmount(),  // 550
            balanceBefore,              // 0
            String.format("%s 결제", payment.getOrderName())
            // "크레딧 500점 (+50 보너스) 결제"
        );
        creditTransactionRepository.save(transaction);

        // ═══════════════════════════════════
        // Step 7: 로깅
        // ═══════════════════════════════════
        log.info("결제 승인 완료: orderId={}, creditAmount={}",
            request.orderId(), payment.getCreditAmount());

        // ═══════════════════════════════════
        // Step 8: 응답 반환
        // ═══════════════════════════════════
        return PaymentResponse.from(payment);
        // {
        //   "id": "payment-uuid",
        //   "orderId": "SL-abc123...",
        //   "status": "DONE",
        //   "creditAmount": 550,
        //   ...
        // }
    }

    // ═══════════════════════════════════
    // 헬퍼 메서드들
    // ═══════════════════════════════════

    private Credit getOrCreateCredit(UUID userId) {
        return creditRepository.findByUserId(userId)
            .orElseGet(() -> {
                // 신규 사용자: 크레딧 0으로 생성
                Credit newCredit = Credit.createForUser(userId);
                return creditRepository.save(newCredit);
            });
    }

    private void validatePaymentOwner(Payment payment, UUID userId) {
        if (!payment.getUserId().equals(userId)) {
            throw new UnauthorizedPaymentAccessException(
                "다른 사람의 결제입니다!"
            );
        }
    }

    // ... 기타 검증 메서드들
}
```

**실행 시 DB 변화**:

```sql
-- 실행 전:
payments:      { id: 1, orderId: "SL-abc", status: "PENDING", ... }
credits:       { user_id: 123, balance: 0, ... }
transactions:  (비어있음)

-- 실행 후:
payments:      { id: 1, orderId: "SL-abc", status: "DONE", paymentKey: "toss_..." }
credits:       { user_id: 123, balance: 550, total_charged: 550 }
transactions:  { type: "CHARGE", amount: 550, balance_before: 0, balance_after: 550 }
```

---

### 📝 CreditService - 크레딧 사용 로직

```java
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;
    private final CreditTransactionRepository creditTransactionRepository;

    /**
     * 크레딧 사용 - 챕터 읽기 시 호출
     *
     * 사용 예:
     * creditService.useCredit(userId, new CreditUseRequest(
     *     5,  // 5 크레딧
     *     "챕터 1 읽기",
     *     "CHAPTER",
     *     "chapter-uuid-1"
     * ));
     */
    @Transactional
    public CreditResponse useCredit(
            UUID userId,
            CreditUseRequest request) {

        // ═══════════════════════════════════
        // Step 1: 크레딧 조회 (비관적 락!)
        // ═══════════════════════════════════
        Credit credit = creditRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new CreditNotFoundException(
                "크레딧 정보를 찾을 수 없습니다. 회원가입 필요?"
            ));
        // ↑ 이 순간부터 이 사용자의 Credit은 잠금!
        //   다른 챕터 읽기 요청은 대기

        // ═══════════════════════════════════
        // Step 2: 현재 잔액 기록
        // ═══════════════════════════════════
        Long balanceBefore = credit.getBalance();
        // 예: 550

        // ═══════════════════════════════════
        // Step 3: 크레딧 차감
        // ═══════════════════════════════════
        credit.use(request.amount());
        // ↑ 내부에서:
        //   1. if (balance < amount) → 에러!
        //   2. balance -= amount
        //   3. totalUsed += amount

        // 성공 시:
        // balance: 550 → 545
        // totalUsed: 0 → 5

        creditRepository.save(credit);

        // ═══════════════════════════════════
        // Step 4: 거래 내역 기록
        // ═══════════════════════════════════
        CreditTransaction transaction = CreditTransaction.createUseTransaction(
            userId,
            credit.getId(),
            request.amount(),         // 5
            balanceBefore,            // 550
            request.description(),    // "챕터 1 읽기"
            request.referenceType(),  // "CHAPTER"
            request.referenceId()     // "chapter-uuid-1"
        );
        creditTransactionRepository.save(transaction);

        // DB 저장 내용:
        // {
        //   type: "USE",
        //   amount: -5,  // 음수!
        //   balance_before: 550,
        //   balance_after: 545,
        //   description: "챕터 1 읽기",
        //   reference_type: "CHAPTER",
        //   reference_id: "chapter-uuid-1"
        // }

        // ═══════════════════════════════════
        // Step 5: 로깅
        // ═══════════════════════════════════
        log.info("크레딧 사용: userId={}, amount={}, balance={}",
            userId, request.amount(), credit.getBalance());
        // "크레딧 사용: userId=123, amount=5, balance=545"

        // ═══════════════════════════════════
        // Step 6: 응답 반환
        // ═══════════════════════════════════
        return CreditResponse.from(credit);
        // {
        //   "id": "credit-uuid",
        //   "balance": 545,
        //   "totalCharged": 550,
        //   "totalUsed": 5
        // }
    }

    /**
     * 사용 가능 여부 확인
     * - 챕터 읽기 전 미리 확인용
     */
    @Transactional(readOnly = true)  // 읽기 전용!
    public boolean canUseCredit(UUID userId, Long amount) {
        return creditRepository.findByUserId(userId)
            .map(credit -> credit.canUse(amount))
            // balance >= amount? → true/false
            .orElse(false);
            // 크레딧 없으면 false
    }
}
```

**실행 시 DB 변화**:

```sql
-- 실행 전:
credits:       { balance: 550, total_used: 0 }
transactions:  { id: 1, type: "CHARGE", amount: 550, ... }

-- 실행 후:
credits:       { balance: 545, total_used: 5 }
transactions:  {
                 { id: 1, type: "CHARGE", amount: 550, ... },
                 { id: 2, type: "USE", amount: -5, balance_before: 550, balance_after: 545 }
               }
```

---

## 실제 사용 시나리오

### 시나리오 1: 첫 결제부터 챕터 읽기까지

```bash
# ═══════════════════════════════════
# 사용자: 김철수 (user-uuid: 123)
# 상황: 처음 가입, 크레딧 0
# ═══════════════════════════════════

# ─────────────────────────────────
# 1단계: 크레딧 패키지 목록 보기
# ─────────────────────────────────
curl http://localhost:8080/api/v1/payments/packages

# 응답:
{
  "data": [
    { "id": 1, "name": "100점", "price": 1000, "creditAmount": 100 },
    { "id": 2, "name": "500점+50보너스", "price": 5000, "creditAmount": 550 },
    ...
  ]
}

# 사용자: "2번 패키지 살래!"

# ─────────────────────────────────
# 2단계: 결제 준비
# ─────────────────────────────────
curl -X POST http://localhost:8080/api/v1/payments/prepare \
  -H "X-User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{"packageId": 2}'

# 응답:
{
  "data": {
    "orderId": "SL-abc123def456",
    "orderName": "크레딧 500점 (+50 보너스)",
    "amount": 5000,
    "creditAmount": 550,
    "successUrl": "/payments/success?orderId=SL-abc123def456"
  }
}

# 서버가 한 일:
# 1. DB에 Payment 저장
#    { orderId: "SL-abc123def456", status: "PENDING", amount: 5000, creditAmount: 550 }
# 2. 30분 후 만료 시간 설정
# 3. 멱등성 키 저장

# ─────────────────────────────────
# 3단계: 토스 결제창 (프론트엔드)
# ─────────────────────────────────
# (사용자가 토스 앱에서 카드 정보 입력)
# (결제 완료!)
# → 토스가 paymentKey 발급: "toss_payment_xyz789"

# ─────────────────────────────────
# 4단계: 결제 승인
# ─────────────────────────────────
curl -X POST http://localhost:8080/api/v1/payments/confirm \
  -H "X-User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "SL-abc123def456",
    "paymentKey": "toss_payment_xyz789",
    "amount": 5000
  }'

# 서버가 한 일:
# 1. DB에서 Payment 조회 (락!)
# 2. 검증 (본인? 상태? 금액? 만료?)
# 3. 토스 API 호출: "이 결제 진짜?"
#    → 토스: "네! 진짜입니다!"
# 4. Payment 상태: PENDING → DONE
# 5. Credit 충전: balance 0 → 550
# 6. CreditTransaction 기록

# 응답:
{
  "data": {
    "id": "payment-uuid-1",
    "orderId": "SL-abc123def456",
    "status": "DONE",
    "creditAmount": 550,
    "approvedAt": "2026-01-04T12:00:00"
  }
}

# ─────────────────────────────────
# 5단계: 크레딧 확인
# ─────────────────────────────────
curl http://localhost:8080/api/v1/credits \
  -H "X-User-Id: 123"

# 응답:
{
  "data": {
    "balance": 550,  # ✅ 550 크레딧 충전됨!
    "totalCharged": 550,
    "totalUsed": 0
  }
}

# ─────────────────────────────────
# 6단계: 챕터 1 읽기
# ─────────────────────────────────
curl -X POST http://localhost:8080/api/v1/credits/use \
  -H "X-User-Id: 123" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 5,
    "description": "반지의 제왕 - 챕터 1",
    "referenceType": "CHAPTER",
    "referenceId": "chapter-uuid-1"
  }'

# 서버가 한 일:
# 1. Credit 조회 (락!)
# 2. 잔액 확인: 550 >= 5? → OK
# 3. 차감: 550 → 545
# 4. CreditTransaction 기록

# 응답:
{
  "data": {
    "balance": 545,  # ✅ 5 크레딧 차감!
    "totalUsed": 5
  }
}

# ─────────────────────────────────
# 7단계: 거래 내역 확인
# ─────────────────────────────────
curl http://localhost:8080/api/v1/credits/transactions \
  -H "X-User-Id: 123"

# 응답:
{
  "data": {
    "content": [
      {
        "id": "tx-2",
        "type": "USE",
        "amount": -5,
        "balanceBefore": 550,
        "balanceAfter": 545,
        "description": "반지의 제왕 - 챕터 1",
        "referenceType": "CHAPTER",
        "referenceId": "chapter-uuid-1",
        "createdAt": "2026-01-04T12:05:00"
      },
      {
        "id": "tx-1",
        "type": "CHARGE",
        "amount": 550,
        "balanceBefore": 0,
        "balanceAfter": 550,
        "description": "크레딧 500점 (+50 보너스) 결제",
        "createdAt": "2026-01-04T12:00:00"
      }
    ]
  }
}
```

---

### 시나리오 2: 잔액 부족 시

```bash
# 현재 잔액: 3 크레딧
# 챕터 읽기 시도 (5 크레딧 필요)

curl -X POST http://localhost:8080/api/v1/credits/use \
  -H "X-User-Id: 123" \
  -d '{"amount": 5, "description": "챕터 읽기"}'

# 응답 (HTTP 402 Payment Required):
{
  "code": 402,
  "status": "PAYMENT_REQUIRED",
  "message": "크레딧 잔액이 부족합니다. 현재: 3, 필요: 5"
}

# 프론트엔드에서 처리:
if (response.status === 402) {
  alert("크레딧이 부족합니다! 충전하시겠습니까?");
  redirect("/payments/packages");  // 결제 페이지로
}
```

---

## 문제 해결 가이드

### ❓ Q1: "결제는 됐는데 크레딧이 안 들어와요!"

#### 가능성 1: 결제 승인 API를 안 호출함
```
토스 결제창에서 결제 → 성공!
하지만... /payments/confirm 호출 안 함
→ Payment 상태가 PENDING인 채로 남아있음
→ 크레딧 미충전

해결:
1. payments 테이블 확인:
   SELECT * FROM payments WHERE order_id = 'SL-abc123';
   → status가 PENDING이면 confirm 미호출

2. /payments/confirm 호출
```

#### 가능성 2: 트랜잭션 롤백
```
confirm 호출 중 에러 발생 → 롤백
→ 크레딧 충전 취소

해결:
1. 로그 확인:
   grep "결제 승인" application.log
   grep "ERROR" application.log

2. payments 테이블 확인:
   SELECT status, failure_message FROM payments WHERE order_id = 'SL-abc123';
   → status가 FAILED면 failure_message 확인
```

---

### ❓ Q2: "크레딧이 이상하게 차감돼요!"

#### 가능성 1: 동시 요청
```
챕터 1과 챕터 2를 거의 동시에 클릭
→ 둘 다 처리되어 10 크레딧 차감

해결:
- 정상 동작입니다!
- 프론트엔드에서 중복 클릭 방지:
  button.disabled = true;
```

#### 가능성 2: 거래 내역 확인
```
credit_transactions 테이블 조회:
SELECT * FROM credit_transactions
WHERE user_id = '123'
ORDER BY created_at DESC;

→ 언제, 얼마나, 왜 차감됐는지 확인
```

---

### ❓ Q3: "결제 취소하고 싶어요!"

```bash
# 결제 ID 확인
GET /api/v1/payments
# → "id": "payment-uuid-1"

# 전액 취소
POST /api/v1/payments/payment-uuid-1/cancel
{
  "cancelReason": "단순 변심"
}

# 부분 취소 (5000원 중 3000원만)
POST /api/v1/payments/payment-uuid-1/cancel
{
  "cancelReason": "부분 환불",
  "cancelAmount": 3000
}

# 서버가 하는 일:
# 1. 토스 API로 취소 요청
# 2. 크레딧 비례 차감
#    예: 5000원 → 550 크레딧
#        3000원 취소 → 330 크레딧 차감
# 3. 거래 내역 기록 (REFUND)
```

---

## 마무리

### ✅ 이제 이해하셨나요?

이 시스템은:
1. **결제 시스템** (토스 페이먼츠 연동)
2. **크레딧 시스템** (충전/사용/환불)
3. **감사 추적** (모든 거래 기록)

을 결합한 **프로덕션 수준의 완전한 결제 시스템**입니다.

### 📚 더 공부하고 싶다면?

1. **동시성 제어**: "Java Concurrency in Practice" 책
2. **트랜잭션**: "Designing Data-Intensive Applications" 책
3. **도메인 주도 설계**: "Domain-Driven Design" (Eric Evans)
4. **Spring Data JPA**: 공식 문서

### 💼 면접 준비용 핵심 키워드

- 비관적 락 / 낙관적 락
- 멱등성 (Idempotency)
- 트랜잭션 ACID
- 도메인 주도 설계 (DDD)
- 외부 API 통합
- 감사 추적 (Audit Trail)
- 결제 상태 관리
- 동시성 제어
- 데이터 정합성

---

**작성일**: 2026년 1월 4일
**난이도**: ⭐⭐⭐⭐☆ (중급-고급)
**프로덕션 준비도**: 95%
