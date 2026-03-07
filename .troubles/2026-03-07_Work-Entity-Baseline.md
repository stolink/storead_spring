# Work 엔티티 스키마 불일치 (Flyway V000)

## Issue Description

Work 엔티티(`Work.java`)에 `isFree` 및 `accessType` 필드가 추가되었지만, Flyway의 초기 스키마 마이그레이션 파일인 `V000__baseline.sql`의 `works` 테이블 정의에는 해당 컬럼들이 누락되어 있었습니다.

- 파일: `src/main/resources/db/migration/V000__baseline.sql`
- 에러 유형: 🔴 치명적

## Solution Strategy

`V000__baseline.sql` 파일의 `works` 테이블 정의에 `is_free` 및 `access_type` 컬럼을 추가했습니다.
_참고사항_: `application-prod.yml`의 `baseline-version: "5"` 경고(⚠️)는 프로덕션 DB가 이미 V5까지의 스키마 변경을 포함하고 있으므로 기존 데이터베이스 충돌을 방지하고 V6부터 마이그레이션을 안전하게 적용하기 위해 의도된 설정이므로 수정하지 않습니다.

### 변경 전

```sql
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    like_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
```

### 변경 후

```sql
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    like_count BIGINT NOT NULL DEFAULT 0,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    access_type VARCHAR(30) NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
```

## Outcome

- **상태**: ✅ 해결됨
- **검증 방법**: 코드 리뷰 지적 사항 반영
