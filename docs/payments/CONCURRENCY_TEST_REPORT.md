# 결제 시스템 동시성 테스트 보고서

**작성일:** 2026.02.17
**작성자:** Dongha (StoRead Developer)

---

## 1. 테스트 개요

### 목적

결제 시스템의 핵심인 **크레딧 차감(Credit Deduction)** 로직이 고동시성 환경에서 데이터 정합성을 유지하는지 검증합니다.
특히 **비관적 락(Pessimistic Lock)** 적용 시 데드락 발생 여부, 잔액 부족 시의 방어 로직, 그리고 실제 처리 성능(TPS)을 측정하여 아키텍처의 신뢰성을 증명합니다.

### 테스트 시나리오

1.  **정합성 검증:** 10,000원 보유 계정에 100원 차감 요청 100회 (Expect: 잔액 0원)
2.  **잔액 부족 검증:** 5,000원 보유 계정에 100원 차감 요청 100회 (Expect: 50건 성공, 50건 실패, 잔액 0원 유지)

---

## 2. 테스트 환경

- **Framework:** Spring Boot 3.4
- **Database:** **PostgreSQL 16 (Testcontainers)**
  - 실제 운영 환경과 동일한 격리 수준(Read Committed) 및 Lock 메커니즘 위에서 검증.
- **Hardware:** Apple M1 (Local Development Environment)
- **Concurrency Tool:** `ExecutorService` (32 threads), `CountDownLatch`
- **Measurement:** JUnit 테스트 내 실행 시간 측정 기반 TPS 산출 (`성공 횟수 / 소요 시간`)
- **Locking Strategy:** `PESSIMISTIC_WRITE` (SELECT ... FOR UPDATE)

---

## 3. 핵심 구현 로직

### 3-1. Service Layer (`CreditService.java`)

트랜잭션 진입 시점부터 락을 획득하여, 잔액 검증과 차감 과정의 원자성(Atomicity)을 보장합니다.

```java
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;

    @Transactional
    public void useCredit(UUID userId, CreditUseRequest request) {
        // 1. 락 획득과 함께 조회 (PESSIMISTIC_WRITE)
        Credit credit = creditRepository.findByUserIdWithLock(userId)
            .orElseThrow(() -> new PaymentExceptions.CreditNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 비즈니스 로직 수행 (잔액 검증 및 차감)
        // 엔티티 내부에서 balance < amount 일 경우 예외 발생
        Long balanceBefore = credit.getBalance();
        credit.use(request.amount());

        // 3. 변경 사항 저장 (트랜잭션 커밋 시 Lock 해제)
        creditRepository.save(credit);

        // ... (로그 및 트랜잭션 기록 생략)
    }
}
```

### 3-2. Repository Layer (`CreditRepository.java`)

```java
public interface CreditRepository extends JpaRepository<Credit, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Credit c WHERE c.userId = :userId")
    Optional<Credit> findByUserIdWithLock(@Param("userId") UUID userId);
}
```

---

## 4. 테스트 코드 (`CreditConcurrencyTest.java`)

### 4-1. Case 1: 데이터 정합성 완벽 보장

```java
@Test
@DisplayName("동시성 제어: 100개 스레드 동시 차감 요청 시 데이터 정합성 보장 및 데드락 방지")
void concurrencyCreditUseTest() throws InterruptedException {
    // given
    UUID userId = UUID.randomUUID();
    Credit credit = Credit.createForUser(userId);
    credit.charge(10000L); // 100원 * 100회 = 10000원
    creditRepository.save(credit);

    int threadCount = 100;
    ExecutorService executorService = Executors.newFixedThreadPool(32);
    CountDownLatch latch = new CountDownLatch(threadCount);

    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failCount = new AtomicInteger();

    long startTime = System.nanoTime();

    // when
    for (int i = 0; i < threadCount; i++) {
        executorService.submit(() -> {
            try {
                creditService.useCredit(userId, new CreditUseRequest(100L, "Test", null, null));
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            } finally {
                latch.countDown();
            }
        });
    }

    // 타임아웃 설정으로 데드락 발생 시 테스트 실패 처리
    boolean completed = latch.await(30, TimeUnit.SECONDS);
    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);

    executorService.shutdown();

    // then
    assertThat(completed).as("30초 내 모든 트랜잭션 종료").isTrue();

    Credit finalCredit = creditRepository.findByUserId(userId).orElseThrow();
    assertThat(finalCredit.getBalance()).as("잔액은 정확히 0원이어야 함").isEqualTo(0L);
    assertThat(successCount.get()).as("100건 모두 성공").isEqualTo(threadCount);

    System.out.printf("총 소요 시간: %dms, 평균 TPS: %.1f%n", elapsed, threadCount / (elapsed / 1000.0));
}
```

### 4-2. Case 2: 잔액 부족 시 초과 차감 방지

```java
@Test
@DisplayName("동시성 제어: 잔액 부족 시 초과 차감 방지 (음수 잔액 불가)")
void concurrencyCreditUse_insufficientBalance() throws InterruptedException {
    // given
    UUID userId = UUID.randomUUID();
    Credit credit = Credit.createForUser(userId);
    credit.charge(5000L); // 5000원 보유
    creditRepository.save(credit);

    // 100원 * 100회 요청 (총 10,000원 요구 -> 5,000원 부족)
    int threadCount = 100;
    // ... (Executor, Latch 설정 동일) ...

    // when
    // ... (동시 요청 수행) ...

    latch.await(30, TimeUnit.SECONDS);

    // then
    Credit finalCredit = creditRepository.findByUserId(userId).orElseThrow();

    // 잔액이 음수가 도지 않았는지 검증
    assertThat(finalCredit.getBalance()).as("잔액은 0원 이상이어야 함").isGreaterThanOrEqualTo(0L);

    // 정확히 가능한 횟수(50회)만 성공했는지 검증
    assertThat(successCount.get()).isEqualTo(50);
    assertThat(failCount.get()).isEqualTo(50);
}
```

---

## 5. 테스트 결과 및 성능 분석

### 실행 결과 로그

```
> Task :test

CreditConcurrencyTest > 동시성 제어: 100개 스레드 동시 차감 요청 시 데이터 정합성 보장 및 데드락 방지 PASSED
총 소요 시간: 426ms, 평균 TPS: 234.7

CreditConcurrencyTest > 동시성 제어: 잔액 부족 시 초과 차감 방지 PASSED
[Insufficient Balance Test] 소요 시간: 256ms, 평균 TPS: 390.6
```

### 분석

1.  **정합성(Consistency):** `PESSIMISTIC_WRITE` 락을 통해 100개의 동시 요청이 DB 레벨에서 직렬화(Serialization)되어 처리됨을 확인했습니다. 갱신 분실(Lost Update)은 발생하지 않았습니다.
2.  **안정성(Stability):** 잔액 부족 시나리오에서 정확히 50건만 승인되고 나머지 50건은 예외 처리되어, **마이너스 통장(Negative Balance) 현상**을 완벽하게 방어했습니다.
3.  **락 경합 비용(Overhead):** 테스트 환경(Docker via Testcontainers) 기준 평균 **234~390 TPS**를 기록했습니다. 이는 Docker 컨테이너의 오버헤드를 고려하더라도, 일반적인 금융 트랜잭션 처리에 충분한 성능을 보여줍니다. (초기 실행 시 JVM Warm-up 및 컨테이너 초기화 비용으로 낮게 측정될 수 있으나 반복 실행 시 안정화됨)

### 결론

본 테스트를 통해 `CreditService`의 결제 로직이 고동시성 환경에서도 **[데이터 정합성 보장, 초과 인출 방지, 데드락 회피]** 라는 요구사항을 충족함을 운영 환경과 동일한 DB 엔진 위에서 검증했습니다.
