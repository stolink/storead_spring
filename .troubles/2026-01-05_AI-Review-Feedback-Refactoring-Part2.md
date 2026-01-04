# AI Code Review Feedback Refactoring Part 2

## Issue Description

이전 리팩토링 이후 추가된 AI 코드 리뷰 피드백에서 발견된 치명적 결함들을 수정했습니다.

- 파일: `PaymentService.java`
- 에러 유형: 🔴 치명적 (2건)

### 🔴 치명적 이슈

1. **미구현 메서드 호출 (컴파일 에러)**: `cancelPayment`에서 `CreditTransaction.createCancelTransaction()`을 호출하고 있었으나, 실제 엔티티에는 `createRefundTransaction`만 정의되어 있어 컴파일 에러가 발생하는 상태였습니다.
2. **결제 승인 시 동시성 이슈**: `confirmPayment` 과정에서 사용자의 크레딧을 조회/성공 처리할 때 비관적 락(Pessimistic Lock)을 사용하지 않아, 다중 결제 승인이 동시에 일어날 경우 크레딧 합산에 정합성 오류가 발생할 위험이 있었습니다.

## Solution Strategy

### 1. 메서드명 일치화

- `PaymentService`에서 호출하는 메서드명을 엔티티에 정의된 `createRefundTransaction`으로 수정했습니다.

### 2. 동시성 제어 강화 (Pessimistic Lock 적용)

- `getOrCreateCredit` 메서드 내에서 `findByUserId` 대신 `findByUserIdWithLock`을 호출하도록 변경하여, 크레딧 충전 시 해당 사용자 레코드에 대한 쓰기 잠금을 획득하도록 보장했습니다.

## Outcome

- **상태**: ✅ 해결됨
- **빌드 결과**: `./gradlew build` 성공 확인 권장
- **검증 방법**: `confirmPayment`와 `cancelPayment` 로직의 정상 동작 및 DB 락 확인
