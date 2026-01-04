# AI Code Review Feedback Refactoring Part 3

## Issue Description

AI 코드 리뷰에서 'Pass'를 받았으나, 남아있던 경고(⚠️) 사항들을 시스템 안정성과 성능 향상을 위해 개선했습니다.

- 파일: `build.gradle`, `PaymentRepository.java`, `PaymentScheduler.java`, `TossPaymentClient.java`
- 에러 유형: ⚠️ 경고 (3건)

### ⚠️ 경고 이슈

1. **의존성 중복 선언**: `build.gradle`에서 `spring-boot-starter-security` 및 기타 라이브러리들이 중복 선언되어 관리가 비효율적이었습니다.
2. **만료 결제 처리 성능**: `PaymentScheduler`에서 만료된 결제를 하나씩 수정하여 저장하고 있어, 데이터 대량 발생 시 성능 저하 및 커넥션 점유 문제가 우려되었습니다.
3. **외부 API 호출 타임아웃 부재**: `TossPaymentClient`에서 `block()`을 사용한 동기 호출 시 타임아웃이 설정되어 있지 않아, 외부 서버 응답 지연 시 스레드 무한 대기 위험이 있었습니다.

## Solution Strategy

### 1. 빌드 설정 최적화

- `build.gradle`의 중복 선언된 의존성들을 제거하고 섹션별로 정리했습니다.

### 2. 벌크 업데이트 도입 (`PaymentRepository` & `PaymentScheduler`)

- `PaymentRepository`에 `@Modifying`과 `@Query`를 이용한 `updateStatusForExpiredPayments` 메서드를 추가했습니다.
- 스케줄러에서 루프를 도는 대신 단일 쿼리로 모든 만료 결제를 한 번에 `EXPIRED` 상태로 변경하도록 개선했습니다.

### 3. API 호출 안정성 확보

- 모든 `webClient...block()` 호출에 `Duration.ofSeconds(10)` 타임아웃을 명시적으로 추가했습니다.

## Outcome

- **상태**: ✅ 해결됨
- **빌드 결과**: `./gradlew build` 성공 확인 권장
- **검증 방법**: 벌크 업데이트 로그 확인 및 타임아웃 테스트 (Mockito 등 활용 가능)
