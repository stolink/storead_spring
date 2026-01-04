# AI Code Review Feedback Refactoring Part 4

## Issue Description

AI 코드 리뷰에서 'Pass'를 받았으나, 이후 수행된 검증에서 벌크 업데이트 쿼리 내 런타임 오류가 발견되어 수정했습니다.

- 파일: `PaymentRepository.java`, `PaymentScheduler.java`
- 에러 유형: 🔴 치명적 (1건)

### 🔴 치명적 이슈

1. **존재하지 않는 필드 참조 (런타임 에러)**: `PaymentRepository`의 `@Query`에서 `updatedAt` 필드를 업데이트하려 했으나, 실제 `Payment` 엔티티에는 해당 필드가 정의되어 있지 않아 쿼리 실행 시 오류가 발생했습니다.

## Solution Strategy

### 1. 쿼리 수정

- `PaymentRepository.updateStatusForExpiredPayments` 쿼리에서 존재하지 않는 `p.updatedAt = :now` 부분을 삭제했습니다.

### 2. 스케줄러 동기화

- 리포지토리 메서드 시그니처 변경에 맞춰 `PaymentScheduler`의 호출부도 매개변수를 조정하고 코드를 정리했습니다.

## Outcome

- **상태**: ✅ 해결됨
- **빌드 결과**: `./gradlew build` 성공 확인 권장
- **검증 방법**: `PaymentScheduler` 실행 시 쿼리 오류 발생 여부 확인
