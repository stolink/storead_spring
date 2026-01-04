# AI Code Review Feedback Refactoring

## Issue Description

AI 코드 리뷰를 통해 발견된 서비스 안정성 및 보안 관련 결함들을 수정했습니다.

- 파일: `PaymentService.java`, `PaymentScheduler.java`, `JwtAuthenticationFilter.java`
- 에러 유형: 🔴 치명적 (2건), ⚠️ 경고 (2건)

### 🔴 치명적 이슈

1. **트랜잭션 내 외부 API 호출**: `confirmPayment`와 `cancelPayment`에서 DB 락을 점유한 상태로 토스 API를 호출하여 커넥션 풀 고갈 및 장애 전파 위험이 있었습니다.
2. **스케줄러 트랜잭션 범위**: `expireOldPayments` 전체가 하나의 트랜잭션으로 묶여 대량 처리 시 락 범위가 과도하게 넓어지는 문제가 있었습니다.

### ⚠️ 경고 이슈

1. **로그 민감 정보 노출**: `JwtAuthenticationFilter`에서 디버깅 목적으로 JWT 쿠키 값을 출력하고 있었습니다.
2. **부동 소수점 정밀도**: 크레딧 환불 계산 시 정수 나눗셈으로 인한 데이터 부정합 위험이 있었습니다.

## Solution Strategy

### 1. 트랜잭션 분리 (PaymentService)

`TransactionTemplate`을 사용하여 로직을 쪼갰습니다.

- **TX1**: 상태 확인 및 `IN_PROGRESS` 변경 (락 짧게 유지)
- **Non-TX**: 토스 API 호출 (응답 지연 시에도 DB 커넥션 미점유)
- **TX2**: 결과 반영 및 크레딧 지급/차감

### 2. 스케줄러 최적화

- 메서드 레벨의 `@Transactional`을 제거하여 개별 건마다 트랜잭션이 처리되도록 수정했습니다.

### 3. 보안 및 정밀도 개선

- `JwtAuthenticationFilter`의 쿠키 로그 블록을 제거했습니다.
- `BigDecimal`을 도입하고 `RoundingMode.HALF_UP`을 적용하여 환불 크레딧 계산의 정확도를 높였습니다.

## Outcome

- **상태**: ✅ 해결됨
- **빌드 결과**: `./gradlew build` 성공 확인 권장
- **검증 방법**: 토스 테스트 환경 결제/취소 시나리오 재테스트
