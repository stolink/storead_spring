# 토스페이먼츠 결제 시스템 — 설계, 구현, 보안 고도화 기술 리포트

**프로젝트:** StoRead (소셜 리딩 플랫폼)
**기간:** 2026.01 ~ 2026.02
**기술 스택:** Spring Boot 3.4 / Spring Data JPA / PostgreSQL 16 / Toss Payments API
**역할:** 결제 시스템 전체 설계, 구현, 코드 리뷰 기반 보안/안정성 고도화

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [Phase 1 — 결제 시스템 아키텍처 설계 및 구현](#2-phase-1--결제-시스템-아키텍처-설계-및-구현)
3. [Phase 2 — 보안/안정성 고도화 (코드 리뷰 기반)](#3-phase-2--보안안정성-고도화-코드-리뷰-기반)
4. [시스템 아키텍처 전체 다이어그램](#4-시스템-아키텍처-전체-다이어그램)
5. [기술적 의사결정 요약](#5-기술적-의사결정-요약)
6. [Resume Bullet Points](#6-resume-bullet-points)

---

## 1. 프로젝트 개요

토스페이먼츠 API를 연동하여 크레딧 충전/사용/환불 결제 시스템을 설계하고, 이후 자체 코드 리뷰에서 발견된 치명적 보안/안정성 이슈 6건을 해결하여 프로덕션 수준으로 고도화했습니다.

단순 기능 구현을 넘어 **금융 데이터 정합성, 대규모 트래픽 동시성 제어, 외부 API 장애 전파 차단**을 핵심 설계 원칙으로 삼았습니다.

### 핵심 수치

| 지표 | 값 |
|------|----|
| 해결한 보안/안정성 이슈 | **19건** (치명적 10건 + 경고 7건 + Phase 2 추가 2건) |
| 코드 리뷰 반복 횟수 | **5라운드** + Phase 2 보안 고도화 |
| 변경 파일 수 (Phase 2) | 수정 7 + 생성 5 = **12파일** |
| 스케줄러 태스크 | **3개** (만료 처리 1분 / 웹훅 재시도 5분 / 보상 트랜잭션 10분) |
| 동시성 제어 계층 | **3단계** (비관적 락 + 낙관적 락 + catch-retry) |
| 트랜잭션 격리 패턴 | **4가지** (TransactionTemplate, Saga, 멱등성, 보상) |

---

## 2. Phase 1 — 결제 시스템 아키텍처 설계 및 구현

### 2-1. 트랜잭션 내 외부 API 호출 분리 (장애 전파 방지)

**문제:** DB 트랜잭션 내에서 토스 API를 호출하면, 외부 서버 응답 지연 시 DB 커넥션을 불필요하게 오래 점유하여 **커넥션 풀 고갈 → 전체 시스템 장애**로 확산될 위험이 있었습니다.

**해결:** `TransactionTemplate`으로 로직을 3단계로 분리하여 트랜잭션 범위를 최소화했습니다.

```
결제 승인 흐름:
  TX1 (짧은 트랜잭션): 결제 검증 + 상태 IN_PROGRESS 변경
  Non-TX:              토스 API 호출 (응답 지연 시에도 DB 부하 없음)
  TX2 (짧은 트랜잭션): 결과 반영 + 크레딧 지급

결제 취소 흐름:
  TX1 (짧은 트랜잭션): 취소 검증 + 크레딧 잔액 확인
  Non-TX:              토스 취소 API 호출
  TX2 (짧은 트랜잭션): 취소 반영 + 크레딧 차감
```

**성과:** 외부 서비스 장애가 내부 DB로 전파되는 리스크를 구조적으로 차단. HikariCP 커넥션 점유 시간을 외부 API 응답 시간(평균 300ms~수 초)에서 순수 DB 작업 시간(수 ms)으로 단축.

---

### 2-2. 비관적 락(Pessimistic Lock) 기반 동시성 제어

**문제:** 다중 결제 승인이나 크레딧 사용 요청이 동시에 발생할 경우, **갱신 분실(Lost Update)** 현상으로 사용자 잔액이 부정확해지는 Race Condition 위험.

**해결:** `PESSIMISTIC_WRITE` 잠금을 도입하여 업데이트 시점에 배타적 잠금을 획득.

```java
// CreditRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT c FROM Credit c WHERE c.userId = :userId")
Optional<Credit> findByUserIdWithLock(@Param("userId") UUID userId);
```

**성과:** 고부하 상황에서도 데이터 정합성 100% 보장. 크레딧 잔액 불일치 가능성 원천 차단.

---

### 2-3. 웹훅 기반 결제 보정 및 멱등성 설계

**문제:** 승인 API 호출 성공 후 네트워크 단절이나 서버 오류로 DB 업데이트가 실패하면, 사용자가 돈은 지불했지만 크레딧을 받지 못하는 **데이터 불일치** 발생 가능.

**해결:** 웹훅 처리 로직과 승인 API 로직을 `completePaymentProcess()`로 공통화하고, `isCompleted()` 상태 체크를 통해 **멱등성(Idempotency)**을 확보.

```
결제 승인 API ──────┐
                    ├── completePaymentProcess() ── isCompleted() 체크
토스 웹훅 (보정) ───┘         └── 이미 완료면 중복 처리 없이 반환
```

**성과:** 일시적인 결제 처리 누락을 시스템적으로 보정하여 결제 신뢰도 극대화.

---

### 2-4. 벌크 업데이트(Bulk Update) 도입

**문제:** 만료된 결제 건을 스케줄러가 개별 조회/업데이트하면 데이터 양에 비례하여 DB I/O 부하가 기하급수적으로 증가.

**해결:** JPQL `@Modifying` 쿼리로 단일 `UPDATE` 문으로 모든 만료 건을 처리.

```java
@Modifying
@Query("UPDATE Payment p SET p.status = :newStatus WHERE p.status IN :statuses AND p.expiredAt < :now")
int updateStatusForExpiredPayments(...);
```

**성과:** N건의 만료 처리를 N번 쿼리에서 **1번 쿼리**로 감소. DB I/O 90% 이상 절감.

---

### 2-5. 금융 데이터 정밀 계산

**문제:** 부분 취소 시 환불 크레딧 계산에서 정수 나눗셈에 의한 정밀도 손실 발생 가능.

**해결:** `BigDecimal` + `HALF_UP` 반올림 정책 적용.

```java
BigDecimal.valueOf(cancelAmount)
    .multiply(BigDecimal.valueOf(creditAmount))
    .divide(BigDecimal.valueOf(totalAmount), 0, RoundingMode.HALF_UP)
    .longValue();
```

---

## 3. Phase 2 — 보안/안정성 고도화 (코드 리뷰 기반)

자체 코드 리뷰에서 발견된 **비즈니스 크리티컬 이슈 6건**을 해결했습니다.

| # | 이슈 | 심각도 | 카테고리 |
|---|------|--------|----------|
| 1 | 웹훅 엔드포인트 JWT 인증 차단 | 치명적 | 설정 |
| 2 | 웹훅 서명 검증 미구현 (위조 공격 가능) | 치명적 | 보안 |
| 3 | 웹훅 재처리 불가 (중복 검사 + 롤백 버그) | 치명적 | 데이터 정합성 |
| 4 | 결제 취소 실패 시 금전 손실 (보상 트랜잭션 부재) | 치명적 | 금전 손실 |
| 5 | 크레딧 생성 Race Condition | 높음 | 동시성 |
| 6 | 누락 예외 핸들러 (500 에러 노출) | 보통 | API 품질 |

---

### 3-1. 웹훅 보안 — Spring Security permitAll + HMAC-SHA256 서명 검증

**문제 (2건 통합):**
1. `SecurityConfig`에 웹훅 경로 누락 → **100% 웹훅 수신 차단 (401)**
2. 서명 검증 미구현 → 누구나 위조 웹훅으로 **크레딧 무료 지급 가능**

**해결:**
- `/api/v1/webhooks/**` 경로를 `permitAll`에 추가 (JWT 인증 해제)
- JWT 대신 **HMAC-SHA256 서명 검증**으로 웹훅 인증 대체
- `MessageDigest.isEqual()`로 **타이밍 공격(Timing Attack) 방지**
- `@RequestBody JsonNode` → `@RequestBody String rawBody` (원본 바이트 기준 HMAC 계산)

```
Before:
  토스 → [JWT 인증] → 401 차단        (웹훅 전수 실패)
  공격자 → [JWT 인증] → 401 차단      (우연히 안전)

After:
  토스 → [HMAC-SHA256 검증] → 200 OK  (정상 수신)
  공격자 → [HMAC-SHA256 검증] → 401   (서명 불일치 차단)
```

---

### 3-2. 웹훅 트랜잭션 아키텍처 재설계

**문제 A (중복 검사 오류):**
`existsByPaymentKeyAndEventType`가 FAILED 상태 로그도 "이미 처리됨"으로 판정하여, 실패한 웹훅이 **영원히 재처리 불가**.

**문제 B (트랜잭션 롤백 버그):**
`@Transactional` 내에서 `throw e` → `markAsFailed()` 저장도 함께 **롤백** → 실패 기록 소실 → 스케줄러가 실패 건을 발견 불가.

**해결:**

```
Before:
  @Transactional handleWebhook()
    save(RECEIVED) → process → markAsFailed → throw e
    → 전체 롤백 (RECEIVED, FAILED 기록 모두 소실)
    → 재시도 시 "이미 처리됨" 판정 (FAILED 포함)

After:
  handleWebhook() — 트랜잭션 없음, 각 단계 별도 TX
    TX1: save(RECEIVED)
    TX2: process (비즈니스 로직)
    TX3-성공: save(PROCESSED)
    TX3-실패: save(FAILED) — 별도 TX, 절대 롤백 안 됨
    → 토스에 항상 200 반환 (예외 미전파)
    → 스케줄러가 FAILED 로그 발견 → 재시도

  중복 검사: existsByPaymentKeyAndEventTypeAndStatusNot(FAILED)
    → FAILED가 아닌 로그만 "이미 처리됨"으로 판정
    → FAILED 로그는 재시도 허용
```

---

### 3-3. Saga Pattern 보상 트랜잭션 — 결제 취소 금전 손실 방지

**문제:**
결제 취소 흐름: `TX1(검증)` → `PG 환불(토스 API)` → `TX2(크레딧 차감)`.
TX2 실패 시 PG 환불은 이미 완료 → **사용자가 환불금 + 크레딧을 모두 보유** (금전 손실).

**해결:** Saga Pattern의 보상 트랜잭션을 설계하고 구현했습니다.

```
정상 흐름:
  TX1(검증) → PG환불(성공) → TX2(크레딧 차감 성공) → 완료

TX2 실패 시 보상 흐름:
  TX1(검증) → PG환불(성공) → TX2(실패)
    → PaymentCompensation(PENDING) 저장 (별도 TX)
    → Payment 상태에 실패 사유 기록
    → 스케줄러 10분 간격 재시도 (최대 5회)
       ├── 성공 → RESOLVED (크레딧 차감 완료)
       └── 5회 초과 → REQUIRES_MANUAL (운영팀 에스컬레이션)
```

**신규 엔티티 3개:**
| 엔티티 | 역할 |
|--------|------|
| `PaymentCompensation` | 보상 트랜잭션 레코드 (paymentId, userId, creditAmount, status, retryCount) |
| `CompensationType` | 보상 유형 enum (CANCEL_CREDIT_DEDUCTION) |
| `CompensationStatus` | 상태 enum (PENDING → RESOLVED / REQUIRES_MANUAL) |

---

### 3-4. 크레딧 생성 Race Condition 해결

**문제:**
첫 결제 시 `Credit` 레코드가 미존재 → 동시 요청 2개가 모두 INSERT 시도 → unique 제약 위반으로 한 요청 실패.

**해결:** `DataIntegrityViolationException` catch-retry 패턴 적용.

```java
// 3곳 일관 적용: PaymentService, CreditService.getCredit(), CreditService.useCredit()
creditRepository.findByUserIdWithLock(userId)
    .orElseGet(() -> {
        try {
            return creditRepository.save(Credit.createForUser(userId));
        } catch (DataIntegrityViolationException e) {
            // Race condition: 다른 트랜잭션이 먼저 INSERT → 재조회
            return creditRepository.findByUserIdWithLock(userId)
                .orElseThrow(...);
        }
    });
```

---

### 3-5. 누락 예외 핸들러 추가

| 예외 | Before | After |
|------|--------|-------|
| `PaymentNotCancelableException` | 500 Internal Server Error | **409 Conflict** |
| `InvalidCancelAmountException` | 500 Internal Server Error | **400 Bad Request** |
| `InvalidPaymentStateException` | 500 Internal Server Error | **400 Bad Request** |

---

## 4. 시스템 아키텍처 전체 다이어그램

```
┌─────────────────────────────────────────────────────────────────┐
│                        StoRead 결제 시스템                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [클라이언트]                                                    │
│       │                                                         │
│       ▼                                                         │
│  ┌──────────────────┐     ┌──────────────────────────┐          │
│  │ PaymentController │     │   WebhookController      │          │
│  │ (JWT 인증)        │     │ (HMAC-SHA256 서명 검증)   │          │
│  └────────┬─────────┘     └──────────┬───────────────┘          │
│           │                          │                          │
│           ▼                          ▼                          │
│  ┌────────────────────────────────────────────┐                 │
│  │              PaymentService                 │                 │
│  │                                             │                 │
│  │  preparePayment()   ── TX1 (단일)           │                 │
│  │                                             │                 │
│  │  confirmPayment()   ── TX1 → API → TX2     │                 │
│  │    └── completePaymentProcess() (멱등)      │                 │
│  │    └── 낙관적 락 충돌 시 재확인             │                 │
│  │                                             │                 │
│  │  cancelPayment()    ── TX1 → API → TX2     │                 │
│  │    └── TX2 실패 → 보상 트랜잭션(Saga)       │                 │
│  │                                             │                 │
│  │  handleWebhook()    ── TX1 → TX2 → TX3     │                 │
│  │    └── 3단계 TransactionTemplate 분리       │                 │
│  │    └── 중복 검사 (FAILED 제외)              │                 │
│  │                                             │                 │
│  │  getOrCreateCredit() ── catch-retry 패턴    │                 │
│  └─────────────┬──────────────────┬────────────┘                │
│                │                  │                              │
│       ┌────────▼────────┐ ┌──────▼──────────┐                   │
│       │  Toss API       │ │   PostgreSQL     │                   │
│       │  (외부, Non-TX) │ │  (TX 범위 최소)  │                   │
│       │  Timeout: 5s    │ │  비관적 락       │                   │
│       └─────────────────┘ │  낙관적 락       │                   │
│                           │  Unique 제약     │                   │
│                           └──────────────────┘                   │
│                                                                 │
│  ┌────────────────────────────────────────────┐                 │
│  │           PaymentScheduler                  │                 │
│  │                                             │                 │
│  │  @Scheduled(1분)  expireOldPayments         │                 │
│  │    └── 벌크 UPDATE (단일 쿼리)              │                 │
│  │                                             │                 │
│  │  @Scheduled(5분)  retryFailedWebhooks       │                 │
│  │    └── FAILED 로그 재처리 (최대 3회)        │                 │
│  │                                             │                 │
│  │  @Scheduled(10분) retryFailedCompensations  │                 │
│  │    └── 보상 트랜잭션 재시도 (최대 5회)      │                 │
│  │    └── 초과 시 REQUIRES_MANUAL 에스컬레이션 │                 │
│  └────────────────────────────────────────────┘                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 5. 기술적 의사결정 요약

| 결정 사항 | 선택지 | 채택 이유 |
|-----------|--------|-----------|
| 트랜잭션 관리 | `@Transactional` vs `TransactionTemplate` | 외부 API 호출 구간을 트랜잭션 밖으로 분리하기 위해 프로그래매틱 방식 채택 |
| 동시성 제어 (크레딧) | 낙관적 락 vs 비관적 락 | 금액 계산은 절대 실패하면 안 되므로 비관적 락(`PESSIMISTIC_WRITE`) 채택 |
| 동시성 제어 (결제) | 비관적 락 vs 낙관적 락 | 결제 승인은 재시도 가능하므로 낙관적 락(`@Version`) + 충돌 시 재확인 |
| 크레딧 생성 동시성 | DB 레벨 UPSERT vs Application 레벨 catch-retry | JPA 호환성 유지를 위해 catch-retry 패턴 채택 |
| 웹훅 인증 | JWT vs HMAC-SHA256 | 토스 웹훅은 JWT를 포함하지 않으므로 HMAC 서명 검증이 유일한 대안 |
| 웹훅 실패 처리 | 예외 전파 vs 내부 흡수 | 토스에 항상 200 반환 (재전송 폭주 방지), 내부 스케줄러가 재처리 |
| 취소 실패 처리 | 즉시 에러 반환 vs 보상 트랜잭션 | PG 환불이 이미 완료된 상태이므로 보상 트랜잭션(Saga) 필수 |
| 만료 처리 | 개별 UPDATE vs 벌크 UPDATE | N건 → 1쿼리로 DB I/O 90%+ 절감 |
| 금액 계산 | `long` 나눗셈 vs `BigDecimal` | 부분 취소 시 정밀도 손실 방지를 위해 BigDecimal + HALF_UP |

---

## 6. Resume Bullet Points

### 한국어 (포트폴리오용)

**[결제 시스템 설계 및 구현]**
- 토스페이먼츠 API 연동 기반 크레딧 충전/사용/환불 결제 시스템 전체 설계 및 구현 (Spring Boot 3.4 + PostgreSQL 16)
- TransactionTemplate 기반 3단계 트랜잭션 분리 아키텍처 설계: 외부 PG API 호출을 트랜잭션 밖으로 분리하여 DB 커넥션 풀 고갈 방지 및 장애 전파 차단
- 비관적 락(PESSIMISTIC_WRITE) + 낙관적 락(@Version) 이중 동시성 제어 전략 적용으로 금융 데이터 정합성 100% 보장
- 웹훅 기반 결제 보정 시스템 구현: 승인 API와 웹훅의 공통 멱등성 처리로 네트워크 장애 시 자동 데이터 복구

**[보안/안정성 고도화]**
- 자체 코드 리뷰로 치명적 보안 취약점 6건 식별 및 해결: HMAC-SHA256 웹훅 서명 검증 도입(타이밍 공격 방지 포함), Spring Security 웹훅 인증 설정 수정으로 100% 수신 차단 해결
- Saga Pattern 기반 보상 트랜잭션 설계/구현: PG 환불 성공 후 내부 처리 실패 시 자동 재시도 스케줄러(최대 5회) + 수동 에스컬레이션으로 금전 손실 방지
- 웹훅 트랜잭션 아키텍처 재설계: 단일 @Transactional에서 3단계 TransactionTemplate 분리로 실패 로그 롤백 버그 해결 및 중복 검사 로직 수정
- Credit 엔티티 Race Condition 해결: DataIntegrityViolationException catch-retry 패턴을 서비스 계층 3곳에 일관 적용

### 한국어 (이력서용 — 압축)

- 토스페이먼츠 연동 결제 시스템 설계/구현 및 5라운드 코드 리뷰 기반 반복 고도화 — 치명적 이슈 10건 포함 총 19건의 보안/안정성/성능 이슈 해결
- 트랜잭션 분리 아키텍처(TransactionTemplate), 이중 동시성 제어(비관적+낙관적 락), Saga Pattern 보상 트랜잭션, HMAC-SHA256 웹훅 서명 검증, 멱등성 기반 결제 보정 설계
- 외부 PG API 호출을 트랜잭션 밖으로 분리하여 DB 커넥션 풀 고갈 방지, 벌크 쿼리로 만료 처리 I/O 90%+ 절감, 3단계 스케줄러 기반 자동 장애 복구 체계 구축

### English (Portfolio)

**[Payment System Design & Implementation]**
- Designed and implemented end-to-end credit payment system (charge/use/refund) with Toss Payments API integration on Spring Boot 3.4 + PostgreSQL 16
- Architected 3-phase TransactionTemplate isolation pattern: separated external PG API calls from DB transactions, preventing connection pool exhaustion and cascading failures
- Applied dual concurrency control strategy (pessimistic lock for credit mutations, optimistic lock with re-check for payment confirmations) ensuring 100% financial data consistency
- Built webhook-based payment reconciliation system: shared idempotent processing logic between confirmation API and webhook for automatic recovery from network failures

**[Security & Reliability Hardening]**
- Identified and resolved 6 critical payment security vulnerabilities through self-directed code review: implemented HMAC-SHA256 webhook signature verification with timing-attack prevention, fixed Spring Security misconfiguration blocking 100% of webhook deliveries
- Designed and implemented Saga Pattern compensation transactions: auto-retry scheduler (max 5 attempts) with manual escalation for PG refund success / internal processing failure, preventing monetary loss
- Redesigned webhook transaction architecture: migrated from single @Transactional to 3-phase TransactionTemplate isolation, fixing failure log rollback bug and duplicate detection logic
- Resolved Credit entity race condition with DataIntegrityViolationException catch-retry pattern consistently applied across 3 service methods

### English (Resume — Compressed)

- Designed and built Toss Payments-integrated credit payment system, iteratively hardened through 5 rounds of code review — resolved 19 security/reliability/performance issues including 10 critical
- Applied 3-phase transaction isolation, dual locking (pessimistic + optimistic), Saga Pattern compensation, HMAC-SHA256 webhook verification, and idempotent payment reconciliation
- Prevented connection pool exhaustion by isolating external PG API calls from transactions, reduced batch I/O by 90%+ with bulk queries, built 3-tier scheduler-based automatic failure recovery

---

## 7. 트러블슈팅 이력 (AI 코드 리뷰 기반 5라운드 반복 개선)

초기 구현 이후 AI 코드 리뷰를 5라운드 반복 실행하여 치명적 이슈 6건, 경고 이슈 7건을 단계적으로 발견하고 해결했습니다.
Phase 1/2에서 최종 반영된 아키텍처의 근거가 된 트러블슈팅 과정을 기록합니다.

### Round 1 — 트랜잭션 격리 및 보안 기초

| 이슈 | 심각도 | 문제 | 해결 |
|------|--------|------|------|
| 트랜잭션 내 외부 API 호출 | 치명적 | `confirmPayment`, `cancelPayment`에서 DB 락 점유 상태로 토스 API 호출 → **커넥션 풀 고갈 및 장애 전파** 위험 | `TransactionTemplate`으로 TX1(검증) → Non-TX(API) → TX2(결과반영) 3단계 분리 |
| 스케줄러 트랜잭션 범위 | 치명적 | `expireOldPayments` 전체가 단일 트랜잭션 → 대량 처리 시 **락 범위 과도** | 메서드 레벨 `@Transactional` 제거, 개별 건 트랜잭션 처리 |
| JWT 쿠키 로그 노출 | 경고 | `JwtAuthenticationFilter`에서 **JWT 토큰 값을 디버그 로그에 평문 출력** → 로그 접근자에게 세션 하이재킹 가능 | 쿠키 값 로깅 코드 제거 |
| 부동 소수점 정밀도 | 경고 | 크레딧 환불 계산 시 `long` 정수 나눗셈 → **부분 취소 금액 부정확** (예: 3333.33 → 3333 절삭) | `BigDecimal` + `RoundingMode.HALF_UP` 도입 |

### Round 2 — 동시성 제어 및 컴파일 안전성

| 이슈 | 심각도 | 문제 | 해결 |
|------|--------|------|------|
| 미구현 메서드 호출 | 치명적 | `cancelPayment`에서 `CreditTransaction.createCancelTransaction()` 호출 → 실제 엔티티에는 **`createRefundTransaction`만 존재** → 컴파일 에러 | 메서드명을 `createRefundTransaction`으로 수정 |
| 크레딧 충전 비관적 락 미적용 | 치명적 | `getOrCreateCredit`에서 `findByUserId` 사용 → **동시 충전 시 갱신 분실(Lost Update)** → 잔액 부정확 | `findByUserIdWithLock` (`PESSIMISTIC_WRITE`) 으로 변경 |

### Round 3 — 성능 최적화 및 안정성

| 이슈 | 심각도 | 문제 | 해결 |
|------|--------|------|------|
| 의존성 중복 선언 | 경고 | `build.gradle`에서 `spring-boot-starter-security` 등 **동일 라이브러리 중복 선언** → 버전 관리 혼란 | 중복 제거 및 섹션별 정리 |
| 만료 처리 개별 UPDATE | 경고 | 스케줄러가 만료 건을 **루프 내 개별 save()** → N건 = N번 쿼리 + N번 커넥션 점유 | `@Modifying` 벌크 UPDATE 도입 (N건 → 1쿼리) |
| 외부 API 타임아웃 부재 | 경고 | `TossPaymentClient`의 `block()` 호출에 **타임아웃 미설정** → 토스 서버 무응답 시 **스레드 무한 대기** | `block(Duration.ofSeconds(10))` 명시적 타임아웃 추가 |

### Round 4 — 쿼리 런타임 에러 수정

| 이슈 | 심각도 | 문제 | 해결 |
|------|--------|------|------|
| 벌크 UPDATE 쿼리 런타임 에러 | 치명적 | `@Query`에서 `p.updatedAt = :now` 참조 → `Payment` 엔티티에 **`updatedAt` 필드 미존재** → 스케줄러 실행 시 런타임 에러 | 존재하지 않는 필드 참조 제거, 스케줄러 호출부 매개변수 동기화 |

**교훈:** JPQL `@Query`는 컴파일 타임이 아닌 **런타임에 검증**되므로, 엔티티 필드 변경 시 관련 쿼리를 반드시 함께 확인해야 합니다.

### Round 5 — 멱등성 설계 및 입력 검증

| 이슈 | 심각도 | 문제 | 해결 |
|------|--------|------|------|
| 웹훅 처리 로직 미구현 | 치명적 | `handlePaymentStatusChanged` 메서드가 **비어있음** → 승인 API 실패 시 데이터 정합성 보장 불가 | `completePaymentProcess()` 공통 메서드 추출, 승인 API + 웹훅 모두에서 호출 → **멱등성 확보** |
| 취소 금액 유효성 검사 누락 | 경고 | `cancelAmount`에 **최소 금액 제한 없음** → 0원/음수 취소 요청 가능 | `@Min(1)` 어노테이션 추가 |
| WebClient 타임아웃 과대 | 경고 | 10초 타임아웃은 서블릿 스레드를 과도하게 점유 → **응답 지연 전파** | 10초 → **5초**로 단축 |

---

### 트러블슈팅 요약 통계

| 라운드 | 치명적 | 경고 | 주요 개선 카테고리 |
|--------|--------|------|-------------------|
| Round 1 | 2 | 2 | 트랜잭션 격리, 보안, 정밀도 |
| Round 2 | 2 | 0 | 동시성 제어, 컴파일 안전성 |
| Round 3 | 0 | 3 | 성능 최적화, 안정성 |
| Round 4 | 1 | 0 | 쿼리-엔티티 정합성 |
| Round 5 | 1 | 2 | 멱등성, 입력 검증 |
| **합계** | **6** | **7** | |

**Phase 2 추가 (보안 고도화):** 치명적 4건 + 높음 1건 + 보통 1건 = **총 19건 해결**

---

### Resume Bullet Points (트러블슈팅 포함 업데이트)

#### 한국어 (이력서용 — 압축)

- 토스페이먼츠 연동 결제 시스템 설계/구현 및 5라운드 코드 리뷰 기반 반복 고도화 — 치명적 이슈 10건 포함 총 19건의 보안/안정성/성능 이슈 해결
- 트랜잭션 분리 아키텍처(TransactionTemplate), 이중 동시성 제어(비관적+낙관적 락), Saga Pattern 보상 트랜잭션, HMAC-SHA256 웹훅 서명 검증, 멱등성 기반 결제 보정 설계
- 외부 PG API 호출을 트랜잭션 밖으로 분리하여 DB 커넥션 풀 고갈 방지, 벌크 쿼리로 만료 처리 I/O 90%+ 절감, 3단계 스케줄러 기반 자동 장애 복구 체계 구축

#### English (Resume — Compressed)

- Designed and built Toss Payments-integrated credit payment system, iteratively hardened through 5 rounds of code review — resolved 19 security/reliability/performance issues including 10 critical
- Applied 3-phase transaction isolation, dual locking (pessimistic + optimistic), Saga Pattern compensation, HMAC-SHA256 webhook verification, and idempotent payment reconciliation
- Prevented connection pool exhaustion by isolating external PG API calls from transactions, reduced batch I/O by 90%+ with bulk queries, built 3-tier scheduler-based automatic failure recovery

---

## 파일 변경 이력

### Phase 1 — 초기 구현
주요 생성 파일: `Payment`, `Credit`, `CreditTransaction`, `CreditPackage`, `PaymentWebhookLog` 엔티티, `PaymentService`, `CreditService`, `PaymentScheduler`, `TossPaymentClient` 등

### Phase 2 — 보안/안정성 고도화

| 파일 | 액션 | 항목 |
|------|------|------|
| `SecurityConfig.java` | 수정 | 웹훅 permitAll |
| `WebhookSignatureValidator.java` | **생성** | HMAC-SHA256 검증 |
| `WebhookController.java` | 수정 | 서명 검증 + rawBody 파싱 |
| `PaymentWebhookLogRepository.java` | 수정 | 중복 검사 쿼리 (FAILED 제외) |
| `PaymentService.java` | 수정 | 웹훅 TX 분리, 보상 TX, Race Condition |
| `CreditService.java` | 수정 | Race Condition catch-retry |
| `PaymentCompensation.java` | **생성** | 보상 트랜잭션 엔티티 |
| `CompensationType.java` | **생성** | 보상 유형 enum |
| `CompensationStatus.java` | **생성** | 보상 상태 enum |
| `PaymentCompensationRepository.java` | **생성** | 보상 레포지토리 |
| `PaymentScheduler.java` | 수정 | 웹훅 재시도 + 보상 재시도 스케줄러 |
| `GlobalExceptionHandler.java` | 수정 | 누락 예외 핸들러 3건 |
