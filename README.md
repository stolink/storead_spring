# StoRead Backend

작가가 관리하던 원고를 외부로 배포하고, 독자가 이를 소비하며 피드백을 남길 수 있는 소셜 기능을 담당하는 Spring Boot 백엔드 서버입니다.

## 기술 스택

- **Spring Boot**: 3.4.1
- **Java**: 21 (AWS Amazon Corretto)
- **Database**: PostgreSQL 16.11
- **Authentication**: Spring Security + JWT + Google OAuth2
- **Payment**: Toss Payments (토스페이먼츠)

## 주요 기능

### 1. 인증 (Auth)

- JWT 토큰 기반 인증 (stolink_spring과 공유)
- Google OAuth2 소셜 로그인
- Access Token / Refresh Token 발급 및 기기 제어 관리
- 회원가입, 로그인, 프로필 관리 및 전체 로그아웃 기능

### 2. 작품 관리 (Works) - 작가용

- 작품 메타데이터 설정 (표지, 장르, 줄거리)
- 작품 CRUD 및 프로젝트 연동
- 연재 상태 관리 (연재중/휴재/완결)
- **[NEW] 통계**: 작품별 조회수, 방문자수(UV), 잔존율, 유입 경로 추적

### 3. 챕터 관리 (Chapters) - 작가용

- 회차별 원고 배포 및 그래프 스냅샷(AI) 저장
- 챕터 순서 관리 (중간 삽입/삭제 시 자동 재정렬)
- 유료 챕터 열람 권한 제어 및 크레딧 소모 체계

### 4. 독자 탐색 (Discovery) - 공개 API

- 공개 작품 목록 조회 및 제목/작가명 검색
- **[NEW] 작품 상세 및 커뮤니티**: 작품 좋아요, 작품별 상세 피드백 제공

### 5. 커뮤니티 및 소셜 (Community & Social)

- 챕터별 별점 부여
- 계층형 댓글 시스템 (대댓글 지원) 및 댓글/챕터 좋아요
- **[NEW] 유저 게이미피케이션**: 독자 레벨/경험치 부여 및 연속 출석 체크 기능

### 6. 커뮤니티 발행 (Community Publish) & 임시저장

- StoLink에서 작성한 원고를 StoRead에 발행
- **[NEW] 임시저장 (Draft)**: 미발행 편집 내용과 문서 연동 식별자 등 보존

### 7. 내 서재 & 읽기 기록 (Library & History)

- 라이브러리: 선호 작품 등록
- 북마크: 챕터별 스크롤 위치 저장
- **[NEW] 열람 기록**: 챕터 단위 상세 열람 로그 및 작품 전체 진행도(%) 추적

### 8. 결제 시스템 (Payment)

- **토스페이먼츠 연동**: 크레딧 충전 및 결제 내역 관리
- **크레딧 무결성**: 잔액 조회, 누적 사용/충전량 추적 및 거래 내역
- **챕터 구매**: 유료 챕터 크레딧 차감 구매 및 본문 접근 권한 검증
- **[NEW] 장애 대응**: 비동기 결제 Webhook 로그 재시도 및 실패 시 보상(Compensation) 트랜잭션 관리

---

## 시작하기

### 사전 요구사항

- Java 21+
- Docker & Docker Compose

### 1. 로컬 개발 (권장)

StoRead는 StoLink와 인프라(PostgreSQL, Neo4j)를 공유합니다.

```bash
# 1. stolink_spring 먼저 실행 (PostgreSQL, Neo4j 제공)
cd ../stolink_spring
docker-compose -f docker-compose.local.yml up -d

# 2. storead_spring 실행
cd ../storead_spring
docker-compose -f docker-compose.local.yml up --build -d
```

다음 서비스가 시작됩니다:

- **StoRead Backend**: `localhost:8081` (로컬 프로필 Override)
- **StoLink Backend**: `localhost:8080` (stolink_spring)
- **PostgreSQL**: `localhost:5432` (stolink_spring 제공)
- **Neo4j**: `localhost:7687` (stolink_spring 제공)

### 2. IDE에서 직접 실행

```bash
# 데이터베이스 컨테이너 실행 (stolink_spring)
cd ../stolink_spring
docker-compose -f docker-compose.local.yml up -d postgres neo4j

# 백엔드 실행
gradlew.bat bootRun  # Windows
./gradlew bootRun    # Linux/Mac
```

### 3. 프로덕션/스테이징 배포

```bash
docker-compose up -d
```

---

## API 명세

### 인증 및 게이미피케이션

| Method | Endpoint                     | 설명                         |
| ------ | ---------------------------- | ---------------------------- |
| POST   | `/api/auth/register`         | 회원가입                     |
| POST   | `/api/auth/login`            | 로그인                       |
| POST   | `/api/auth/refresh`          | Access Token 갱신            |
| POST   | `/api/auth/logout`           | 로그아웃                     |
| POST   | `/api/auth/logout-all`       | 전 기기 로그아웃             |
| GET    | `/api/auth/me`               | 내 정보 조회                 |
| PATCH  | `/api/auth/me`               | 프로필 수정                  |
| GET    | `/api/users/me/gamification` | 내 게이미피케이션(경험치 등) |
| POST   | `/api/users/me/attendance`   | 출석 체크                    |

### 작품 관리 및 통계 (작가용)

| Method | Endpoint                               | 설명                               |
| ------ | -------------------------------------- | ---------------------------------- |
| GET    | `/api/works`                           | 내 작품 목록                       |
| POST   | `/api/works`                           | 작품 생성                          |
| PATCH  | `/api/works/{id}`                      | 작품 수정                          |
| DELETE | `/api/works/{id}`                      | 작품 삭제                          |
| GET    | `/api/author/works/{workId}/stats`     | 작품 종합 통계 (조회/별점/댓글 등) |
| GET    | `/api/author/works/{workId}/retention` | 독자 잔존율 (계단식 열람률)        |

### 챕터 (작가용)

| Method | Endpoint                       | 설명      |
| ------ | ------------------------------ | --------- |
| GET    | `/api/works/{workId}/chapters` | 챕터 목록 |
| POST   | `/api/works/{workId}/chapters` | 챕터 생성 |
| GET    | `/api/chapters/{id}`           | 챕터 상세 |
| PATCH  | `/api/chapters/{id}`           | 챕터 수정 |
| DELETE | `/api/chapters/{id}`           | 챕터 삭제 |

### 독자 탐색 & 피드백 (Public)

| Method | Endpoint                                 | 설명                  |
| ------ | ---------------------------------------- | --------------------- |
| GET    | `/api/discovery`                         | 공개 작품 목록        |
| GET    | `/api/discovery/search`                  | 작품 검색             |
| GET    | `/api/discovery/works/{id}`              | 작품 상세 (공개용)    |
| GET    | `/api/discovery/chapters/{id}`           | 챕터 열람 (공개용)    |
| POST   | `/api/works/{id}/like`                   | 작품 좋아요 토글      |
| GET    | `/api/works/{id}/like`                   | 작품 좋아요 여부      |
| GET    | `/api/discovery/works/{workId}/feedback` | 작품 상세 피드백 목록 |
| POST   | `/api/discovery/works/{workId}/feedback` | 작품 상세 피드백 작성 |

### 커뮤니티 (별점/댓글/라이브러리/북마크)

| Method | Endpoint                               | 설명                    |
| ------ | -------------------------------------- | ----------------------- |
| POST   | `/api/chapters/{id}/rating`            | 챕터 별점 등록/수정     |
| POST   | `/api/chapters/{chapterId}/comments`   | 댓글 작성               |
| POST   | `/api/comments/{id}/replies`           | 대댓글 작성             |
| POST   | `/api/comments/{id}/like`              | 댓글 좋아요 토글        |
| POST   | `/api/library/{workId}`                | 내 서재 작품 추가       |
| GET    | `/api/works/{workId}/reading-progress` | 작품별 읽기 진행도 조회 |
| POST   | `/api/bookmarks/{chapterId}`           | 스크롤 위치 북마크 저장 |

### 결제 및 크레딧

| Method | Endpoint                            | 설명                          |
| ------ | ----------------------------------- | ----------------------------- |
| GET    | `/api/chapters/{id}/purchase/check` | 챕터 권한 검증 (구매 여부 등) |
| POST   | `/api/chapters/{id}/purchase`       | 유료 챕터 크레딧 구매         |
| GET    | `/api/chapters/{id}/access`         | 챕터 뷰 접근/승인             |
| POST   | `/api/payments/prepare`             | 결제 생성 전                  |
| POST   | `/api/payments/confirm`             | 결제 승인 확인                |
| GET    | `/api/credits`                      | 사용자 전체 크레딧 조회       |
| GET    | `/api/credits/transactions`         | 크레딧 입출 내역 요약         |

### 공통/기타 유틸리티

| Method   | Endpoint                 | 설명                                 |
| -------- | ------------------------ | ------------------------------------ |
| POST     | `/api/upload`            | 파일(표지/이미지 등) 클라우드 업로드 |
| POST     | `/api/community/publish` | Draft 기반 원고 발행                 |
| GET/POST | `/api/drafts`            | 임시저장 내용 목록/생성              |

---

## 환경 변수

| 분류       | 변수명                           | 설명            | 비고                         |
| ---------- | -------------------------------- | --------------- | ---------------------------- |
| **DB**     | `POSTGRESQL_URL`                 | DB 호스트       | `jdbc:postgresql://...`      |
| **DB**     | `POSTGRESQL_PORT`                | 포트 번호       | 기본 5432                    |
| **DB**     | `POSTGRESQL_USERNAME / PASSWORD` | 인증 정보       |                              |
| **인증**   | `JWT_SECRET`                     | 시크릿 키       | stolink_spring과 동일해야 함 |
| **인증**   | `JWT_COOKIE_DOMAIN`              | 쿠키 도메인     | 기본 localhost               |
| **OAuth2** | `GOOGLE_CLIENT_ID / SECRET`      | 구글 앱 키      |                              |
| **OAuth2** | `OAUTH2_REDIRECT_URI`            | 인증 콜백 주소  |                              |
| **결제**   | `TOSS_CLIENT_KEY / SECRET_KEY`   | 토스페이먼츠 키 | 테스트는 `test_` 접두사      |
| **결제**   | `TOSS_WEBHOOK_SECRET`            | 웹훅 검증용 키  |                              |
| **CORS**   | `CORS_ALLOWED_ORIGINS`           | 통신 허용 주소  |                              |

---

## 데이터베이스 스키마

### 최신 ERD

```mermaid
erDiagram
    users ||--o{ works : "작성"
    users ||--o{ comments : "작성"
    users ||--o{ chapter_ratings : "평가"
    users ||--o{ comment_likes : "좋아요"
    users ||--o{ chapter_likes : "좋아요"
    users ||--o{ work_likes : "좋아요"
    users ||--o{ work_feedbacks : "피드백"
    users ||--o{ libraries : "담기"
    users ||--o{ bookmarks : "저장"
    users ||--o| credits : "보유"
    users ||--o{ credit_transactions : "거래"
    users ||--o{ payments : "결제"
    users ||--o{ payment_compensations : "보상"
    users ||--o{ chapter_purchases : "구매"
    users ||--o{ reading_histories : "열람"
    users ||--o{ chapter_reading_logs : "열람 로그"
    users ||--o| user_gamifications : "레벨/경험치"
    users ||--o{ refresh_tokens : "발급"
    users ||--o{ visit_logs : "방문"
    users ||--o{ drafts : "작성"

    works ||--o{ chapters : "포함"
    works ||--o{ libraries : "담김"
    works ||--o{ work_likes : "좋아요됨"
    works ||--o{ work_feedbacks : "피드백됨"
    works ||--o{ reading_histories : "열람됨"
    works ||--o{ chapter_reading_logs : "열람됨"
    works ||--o{ visit_logs : "방문됨"

    chapters ||--o{ comments : "포함"
    chapters ||--o{ chapter_ratings : "평가됨"
    chapters ||--o{ chapter_likes : "좋아요됨"
    chapters ||--o{ bookmarks : "저장됨"
    chapters ||--o{ chapter_purchases : "구매됨"
    chapters ||--o{ chapter_reading_logs : "열람됨"

    comments ||--o{ comments : "대댓글"
    comments ||--o{ comment_likes : "좋아요됨"

    payments ||--o{ credit_transactions : "충전 대상"
    payments ||--o{ payment_compensations : "보상 대상"

    credits ||--o{ credit_transactions : "거래"

    users {
        uuid id PK
        string email UK
        string password
        string nickname
        string avatar_url
        enum provider "OAuth2"
        string provider_id
        boolean goal_notification
        boolean foreshadowing_notification
        boolean ai_suggestion_notification
        timestamp created_at
        timestamp updated_at
    }

    user_gamifications {
        uuid id PK
        uuid user_id FK
        int level
        int exp
        string title
        int attendance_streak
        date last_attendance_date
        timestamp created_at
    }

    refresh_tokens {
        uuid id PK
        string token UK
        uuid user_id FK
        timestamp expires_at
        string device_info
        string ip_address
    }

    works {
        uuid id PK
        uuid author_id FK
        string title
        text synopsis
        text cover_image_url
        enum genre
        enum status
        string project_id UK
        bigint rating_sum
        bigint rating_count
        double average_rating
        bigint like_count
        timestamp created_at
        timestamp updated_at
    }

    work_likes {
        uuid id PK
        uuid user_id FK
        uuid work_id FK
    }

    work_feedbacks {
        uuid id PK
        uuid work_id FK
        uuid user_id FK
        enum feedback_type
    }

    chapters {
        uuid id PK
        uuid work_id FK
        string title
        text content
        int chapter_number
        string document_id
        json document_ids
        json graph_snapshot
        bigint view_count
        bigint rating_sum
        bigint rating_count
        boolean is_free
        int price
        enum access_type
        timestamp created_at
        timestamp updated_at
    }

    chapter_likes {
        uuid id PK
        uuid chapter_id FK
        uuid user_id FK
    }

    chapter_purchases {
        uuid id PK
        uuid user_id FK
        uuid chapter_id FK
        int price_paid
        timestamp purchased_at
    }

    comments {
        uuid id PK
        uuid chapter_id FK
        uuid user_id FK
        uuid parent_id FK
        text content
        string relation_id
        bigint like_count
        timestamp created_at
        timestamp updated_at
    }

    comment_likes {
        uuid id PK
        uuid comment_id FK
        uuid user_id FK
    }

    chapter_ratings {
        uuid id PK
        uuid chapter_id FK
        uuid user_id FK
        int score
    }

    libraries {
        uuid id PK
        uuid user_id FK
        uuid work_id FK
    }

    bookmarks {
        uuid id PK
        uuid user_id FK
        uuid chapter_id FK
        int scroll_position "기존 position"
    }

    reading_histories {
        uuid id PK
        uuid user_id FK
        uuid work_id FK
        uuid last_chapter_id
        int last_chapter_number
        int progress
        timestamp last_read_at
    }

    chapter_reading_logs {
        uuid id PK
        uuid work_id FK
        uuid chapter_id FK
        int chapter_number
        uuid user_id FK
    }

    visit_logs {
        uuid id PK
        uuid work_id FK
        uuid user_id FK
        string source
    }

    drafts {
        uuid id PK
        uuid user_id FK
        string document_id
        boolean is_merged
        string project_id
        string title
        text content
        timestamp created_at
        timestamp expires_at
        string work_title
        enum publish_status
    }

    credits {
        uuid id PK
        uuid user_id FK UK
        bigint balance
        bigint total_charged
        bigint total_used
    }

    credit_transactions {
        uuid id PK
        uuid user_id FK
        uuid credit_id FK
        uuid payment_id FK
        enum type
        bigint amount
        bigint balance_before
        bigint balance_after
        string description
        string reference_type
        string reference_id
    }

    payments {
        uuid id PK
        uuid user_id FK
        string order_id UK
        string order_name
        bigint amount
        bigint credit_amount
        string payment_key
        string payment_method
        enum status
        bigint canceled_amount
        string cancel_reason
        string failure_code
        string failure_message
        string idempotency_key UK
        timestamp requested_at
        timestamp approved_at
        timestamp canceled_at
        timestamp expired_at
        json metadata
    }

    payment_compensations {
        uuid id PK
        uuid payment_id FK
        uuid user_id FK
        enum type
        enum status
        bigint credit_amount
        string error_message
        int retry_count
    }

    payment_webhook_logs {
        uuid id PK
        string event_type
        string payment_key
        string order_id
        enum status
        json request_body
        json response_body
        string error_message
        int retry_count
        timestamp next_retry_at
        timestamp received_at
        timestamp processed_at
    }
```

### 테이블 상세 (업데이트됨)

| 테이블                    | 설명                         | 변경/추가 요소                                                |
| ------------------------- | ---------------------------- | ------------------------------------------------------------- |
| `users`                   | 사용자 정보                  | OAuth2 대응 및 세분화된 알림 설정 지원                        |
| `user_gamifications`      | **[NEW]** 레벨 / 경험치      | 연속 출석, 칭호 등 열람 보상 연계                             |
| `refresh_tokens`          | **[NEW]** 리프레시 토큰      | 접속 IP, 기기 정보(`device_info`) 관리                        |
| `works`                   | 작품 기본 정보               | 프로젝트 식별자 연동, 조회 통계 최적화 필드 추가              |
| `work_likes / feedbacks`  | **[NEW]** 인기 및 피드백     | 작품별 좋아요 중복 제어 및 리뷰 시스템 강화                   |
| `chapters`                | 회차 내역                    | AI 기반 분석(`graph_snapshot`), 유료 제한 권한 제어 필드 추가 |
| `chapter_likes`           | **[NEW]** 회차별 좋아요      | 챕터 단위 리뷰 강화                                           |
| `comments` / `likes`      | 댓글 시스템                  | 대댓글 지원 기능 및 외부 참조 ID 추가 구조 확립               |
| `libraries` / `bookmarks` | 서재보관/스크롤 위치         | 기존 `position` → `scroll_position` 명확화                    |
| `reading_histories`       | **[NEW]** 작품 읽기 진척도   | 진행렬(%) 기반의 마지막 읽기 추적 (이어보기 용)               |
| `chapter_reading_logs`    | **[NEW]** 회차 조회 로그     | 세부 이탈률 및 전환율 통계 수집용 시계열성 테이블             |
| `visit_logs`              | **[NEW]** 사이트 유입 소스   | 방문 경로, 유입 트래픽 관리                                   |
| `drafts`                  | **[NEW]** 자동 문서 임시저장 | 배포 전 문서 형상 유지(자동 소멸 지원)                        |
| `credits / transactions`  | 재화 충전 및 트랜잭션        | 충전 무결성 누적 모니터링 체계 도입 (total_charged 등)        |
| `payments / webhooks`     | 결제 처리 본체 및 로그       | 결제 승인/취소와 토스페이먼츠 웹훅 응답 저장소                |
| `compensations`           | **[NEW]** 결제 실패 보상     | 멱등성 기반(네트워크 에러 시) 지급 안정성 처리                |

## 챕터 순서 관리

- 삭제 시: 해당 작품의 더 큰 `chapter_number`를 가진 회차 번호 **-1**
- 중간 삽입 시: 삽입 지점 이후 회차 번호 **+1**

## 관련 프로젝트

- **[StoLink Backend](../stolink_spring)**: 작가용 스토리 관리 플랫폼 (에디터, 인프라 제공)
- **[StoRead Frontend](../storead_frontend)**: React + TypeScript 독자용 프론트엔드
- **[StoLink Frontend](../stolink_frontend)**: 작가용 웹 관리 애플리케이션
