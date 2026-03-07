# Fix Missing Foreign Keys and Indexes in V6 Migration

## Issue Description

The initial `V6__create_payment_compensations_table.sql` was missing critical data integrity and performance features.

- 파일: `src/main/resources/db/migration/V6__create_payment_compensations_table.sql`
- 에러 유형: 🔴 치명적 (외래 키 누락), ⚠️ 경고 (인덱스 누락)

## Solution Strategy

Added `REFERENCES` constraints for `payment_id` and `user_id` to ensure referential integrity. Added indexes for these columns to optimize lookup performance.

### 변경 전

```sql
CREATE TABLE IF NOT EXISTS payment_compensations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    credit_amount BIGINT NOT NULL,
    error_message VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
```

### 변경 후

```sql
CREATE TABLE IF NOT EXISTS payment_compensations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments(id),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    credit_amount BIGINT NOT NULL,
    error_message VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_compensations_payment_id ON payment_compensations(payment_id);
CREATE INDEX IF NOT EXISTS idx_payment_compensations_user_id ON payment_compensations(user_id);
```

## Outcome

- **상태**: ✅ 해결됨
- **검증 방법**: `V6__create_payment_compensations_table.sql` 파일 내용 확인 및 빌드 확인
