# 토스 페이먼츠 크레딧 결제 시스템 API 가이드

## 🚀 구현 완료

StoLink 플랫폼에 토스 페이먼츠 기반 크레딧 결제 시스템이 구현되었습니다.

## 📁 구현된 컴포넌트

### Entity
- `Credit`: 크레딧 잔액 관리
- `Payment`: 결제 정보 관리
- `CreditTransaction`: 크레딧 거래 내역
- `PaymentWebhookLog`: 웹훅 로그
- `PaymentStatus`, `CreditTransactionType`, `WebhookStatus` (Enums)

### Service
- `PaymentService`: 결제 준비/승인/취소/웹훅 처리
- `CreditService`: 크레딧 조회/사용/거래내역
- `CreditPackageService`: 크레딧 패키지 관리
- `TossPaymentClient`: 토스 페이먼츠 API 클라이언트

### Controller
- `PaymentController`: 결제 관련 API
- `CreditController`: 크레딧 관련 API
- `WebhookController`: 웹훅 수신

### Scheduler
- `PaymentScheduler`: 만료 결제 처리, 실패 웹훅 재시도

## 🎯 API 엔드포인트

### 결제 관련 API

#### 1. 크레딧 패키지 목록 조회
```http
GET /api/v1/payments/packages
```

**Response:**
```json
{
  "code": 200,
  "status": "OK",
  "message": "OK",
  "data": [
    {
      "id": 1,
      "name": "크레딧 100점",
      "price": 1000,
      "creditAmount": 100,
      "bonusCredit": 0,
      "isPopular": false
    },
    {
      "id": 2,
      "name": "크레딧 500점 (+50 보너스)",
      "price": 5000,
      "creditAmount": 500,
      "bonusCredit": 50,
      "isPopular": true
    }
  ]
}
```

#### 2. 결제 준비 (주문 생성)
```http
POST /api/v1/payments/prepare
X-User-Id: {userId}
Content-Type: application/json

{
  "packageId": 2
}
```

**Response:**
```json
{
  "code": 200,
  "status": "OK",
  "message": "OK",
  "data": {
    "orderId": "SL-abc123...",
    "orderName": "크레딧 500점 (+50 보너스)",
    "amount": 5000,
    "creditAmount": 550,
    "customerKey": "CUST-1234567890abcdef",
    "successUrl": "/payments/success?orderId=SL-abc123...",
    "failUrl": "/payments/fail?orderId=SL-abc123..."
  }
}
```

#### 3. 결제 승인
```http
POST /api/v1/payments/confirm
X-User-Id: {userId}
Content-Type: application/json

{
  "orderId": "SL-abc123...",
  "paymentKey": "toss_payment_key_...",
  "amount": 5000
}
```

#### 4. 결제 취소
```http
POST /api/v1/payments/{paymentId}/cancel
X-User-Id: {userId}
Content-Type: application/json

{
  "cancelReason": "고객 요청",
  "cancelAmount": null
}
```

#### 5. 결제 내역 조회
```http
GET /api/v1/payments?page=0&size=20
X-User-Id: {userId}
```

#### 6. 결제 상세 조회
```http
GET /api/v1/payments/{paymentId}
X-User-Id: {userId}
```

### 크레딧 관련 API

#### 1. 크레딧 잔액 조회
```http
GET /api/v1/credits
X-User-Id: {userId}
```

**Response:**
```json
{
  "code": 200,
  "status": "OK",
  "message": "OK",
  "data": {
    "id": "credit-uuid",
    "balance": 550,
    "totalCharged": 550,
    "totalUsed": 0,
    "updatedAt": "2026-01-04T12:00:00"
  }
}
```

#### 2. 크레딧 사용
```http
POST /api/v1/credits/use
X-User-Id: {userId}
Content-Type: application/json

{
  "amount": 100,
  "description": "AI 이미지 생성",
  "referenceType": "AI_JOB",
  "referenceId": "job-123"
}
```

#### 3. 크레딧 사용 가능 여부 확인
```http
GET /api/v1/credits/check?amount=100
X-User-Id: {userId}
```

#### 4. 크레딧 거래 내역 조회
```http
GET /api/v1/credits/transactions?page=0&size=20
X-User-Id: {userId}

# 타입별 조회
GET /api/v1/credits/transactions?type=CHARGE&page=0&size=20
X-User-Id: {userId}
```

### 웹훅 API

#### 토스 페이먼츠 웹훅 수신
```http
POST /api/v1/webhooks/toss
TossPayments-Signature: {signature}
Content-Type: application/json

{
  "eventType": "PAYMENT_STATUS_CHANGED",
  "paymentKey": "...",
  "orderId": "...",
  ...
}
```

## 🔧 설정

### 환경 변수
```bash
# Toss Payments API 키 (테스트 환경)
TOSS_SECRET_KEY=test_sk_zXLkKEypNArWmo50nX3lmeaxYG5R
TOSS_CLIENT_KEY=test_ck_D5GePWvyJnrK0W0k6q8gLzN97Eoq
TOSS_WEBHOOK_SECRET=webhook_secret_key
```

### 데이터베이스
JPA의 `ddl-auto: update` 설정으로 자동으로 테이블이 생성됩니다.
필요시 `/src/main/resources/db/migration/V001__create_payment_tables.sql` 참조

## 🛡️ 보안 및 동시성 제어

### 1. 낙관적 락 (Optimistic Locking)
- `Credit`, `Payment` 엔티티에 `@Version` 필드 적용
- 동시 수정 시 `OptimisticLockingFailureException` 발생

### 2. 비관적 락 (Pessimistic Locking)
- 크레딧 사용/충전 시 `findByUserIdWithLock()` 사용
- 결제 승인/취소 시 `findByOrderIdWithLock()` 사용

### 3. 멱등성 보장
- 주문 생성 시 `idempotencyKey`로 중복 방지
- 웹훅 처리 시 `paymentKey + eventType`으로 중복 방지
- 결제 승인 시 상태 체크로 중복 승인 방지

### 4. 트랜잭션 관리
- 모든 쓰기 작업은 `@Transactional` 적용
- 읽기 작업은 `@Transactional(readOnly = true)` 적용

## 📊 데이터 흐름

### 결제 프로세스
1. 사용자가 크레딧 패키지 선택
2. `/payments/prepare` 호출 → `Payment` 엔티티 생성 (PENDING)
3. 프론트엔드에서 토스 결제창 호출
4. 사용자가 결제 완료 후 `/payments/confirm` 호출
5. 토스 페이먼츠 API 승인 요청
6. 승인 성공 시:
   - `Payment` 상태 → DONE
   - `Credit` 충전
   - `CreditTransaction` 기록
7. 웹훅 수신 → 로그 저장

### 크레딧 사용 프로세스
1. AI 서비스에서 `/credits/check` 호출 (잔액 확인)
2. `/credits/use` 호출
3. 비관적 락으로 `Credit` 조회
4. 잔액 차감 및 `CreditTransaction` 기록

## 🔍 주요 특징

### 1. 견고한 에러 처리
- 모든 결제 예외는 `GlobalExceptionHandler`에서 처리
- 토스 API 에러는 `TossPaymentException`으로 변환
- HTTP 상태 코드에 맞는 적절한 응답

### 2. 결제 상태 관리
- PENDING → READY → IN_PROGRESS → DONE
- DONE → PARTIAL_CANCELED → CANCELED
- 각 상태 전이는 `Payment` 엔티티 내부 검증

### 3. 부분 취소 지원
- 전액 취소 또는 부분 취소 가능
- 취소 금액에 비례하여 크레딧 차감

### 4. 자동 만료 처리
- 스케줄러가 1분마다 만료된 결제 처리
- 만료 시 상태 → EXPIRED

### 5. 웹훅 재시도
- 실패한 웹훅은 5분마다 최대 3회 재시도
- 재시도 간격은 5분 × 재시도 횟수

## 🧪 테스트 방법

### 1. 의존성 다운로드
```bash
./gradlew build
```

### 2. 애플리케이션 실행
```bash
./gradlew bootRun
```

### 3. API 테스트 (예시: curl)
```bash
# 크레딧 패키지 조회
curl -X GET http://localhost:8080/api/v1/payments/packages

# 결제 준비
curl -X POST http://localhost:8080/api/v1/payments/prepare \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{"packageId": 2}'

# 크레딧 잔액 조회
curl -X GET http://localhost:8080/api/v1/credits \
  -H "X-User-Id: 550e8400-e29b-41d4-a716-446655440000"
```

## ⚠️ 주의사항

1. **인증**: 현재는 `X-User-Id` 헤더로 사용자 식별. 프로덕션에서는 JWT/OAuth 필수
2. **토스 API 키**: 테스트 키가 하드코딩되어 있음. 프로덕션에서는 환경 변수 사용
3. **웹훅 서명 검증**: 현재 비활성화됨. 프로덕션에서는 반드시 활성화
4. **HTTPS**: 프로덕션 환경에서는 HTTPS 필수
5. **Rate Limiting**: 현재 미구현. 필요 시 추가

## 📝 다음 단계

- [ ] 인증 시스템 통합 (Spring Security)
- [ ] 웹훅 서명 검증 활성화
- [ ] 단위 테스트 작성
- [ ] 통합 테스트 작성
- [ ] API 문서화 (Swagger/OpenAPI)
- [ ] 프론트엔드 통합 가이드
- [ ] 로깅 및 모니터링 강화
- [ ] Rate Limiting 구현

## 📚 참고 자료

- [토스 페이먼츠 개발 가이드](https://docs.tosspayments.com/)
- [토스 페이먼츠 테스트 카드](https://docs.tosspayments.com/reference/test-card)
- [Spring Data JPA 문서](https://spring.io/projects/spring-data-jpa)
