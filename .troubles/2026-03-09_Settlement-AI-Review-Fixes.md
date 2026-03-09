# 정산 시스템 AI 코드 리뷰 수정

## Issue Description

AI 코드 리뷰에서 발견된 🔴 치명적 1건, ⚠️ 경고 1건 수정.

### 🔴 치명적: 수익 기록 실패 시 데이터 불일치

- 파일: `ChapterPurchaseService.java:108-114`
- 에러 유형: 🔴 치명적
- `revenueService.recordChapterSaleRevenue()`를 `try-catch`로 감싸서 예외를 삼키고 있었음. 구매는 성공하지만 수익 기록이 누락되어 데이터 불일치 발생 가능.

### ⚠️ 경고: RevenueTransaction 금액 필드 Integer 타입

- 파일: `RevenueTransaction.java:48-58`
- 에러 유형: ⚠️ 경고
- `creditAmount`, `platformFee`, `authorShare`가 `Integer`로 선언. `AuthorRevenue`는 이미 `Long` 사용 중으로 일관성 부족 및 오버플로우 위험.

## Solution Strategy

### 🔴 수익 기록 try-catch 제거

#### 변경 전
```java
try {
    revenueService.recordChapterSaleRevenue(purchase);
} catch (Exception e) {
    log.warn("수익 기록 실패 (구매는 정상 처리됨): purchaseId={}, error={}",
            purchase.getId(), e.getMessage());
}
```

#### 변경 후
```java
revenueService.recordChapterSaleRevenue(purchase);
```

### ⚠️ Integer → Long 타입 변경

#### 변경 전
```java
private Integer creditAmount;
private Integer platformFee;
private Integer authorShare;
```

#### 변경 후
```java
private Long creditAmount;
private Long platformFee;
private Long authorShare;
```

영향 범위:
- `RevenueTransaction.java` — 필드 및 팩토리 메서드
- `RevenueTransactionResponse.java` — DTO 필드
- `RevenueService.java` — `(long)` 캐스트 제거
- `V8__create_settlement_tables.sql` — `INTEGER` → `BIGINT`
- `SettlementServiceTest.java` — 빌더 리터럴 `100` → `100L`

## Outcome

- **상태**: ✅ 해결됨
- **빌드 결과**: `./gradlew compileJava` 성공
- **테스트 결과**: 15개 단위 테스트 전부 통과
- **검증 방법**: `./gradlew test --tests "com.stolink.backend.domain.settlement.*"`
