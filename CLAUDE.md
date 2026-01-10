# CLAUDE.md - StoRead Backend Project Constitution

> 이 문서는 AI 모델이 백엔드 프로젝트 컨텍스트를 이해하고, 코드 품질을 일관되게 유지하기 위한 **프로젝트 헌법(Constitution)**입니다.

**버전:** 1.0
**최종 수정:** 2026년 1월 8일
**문서 상태:** 활성

---

<project_info>
<description>
StoRead Backend - 작가가 관리하던 원고를 외부로 배포하고, 독자가 이를 소비하며 피드백을 남길 수 있는 소셜 기능을 담당하는 Spring Boot 백엔드 서버
StoLink에서 연재된 작품을 독자가 탐색, 열람, 평가할 수 있는 리더 플랫폼
</description>

<tech_stack>

<!-- 2026.01.08 기준 실제 버전 -->

- **Language**: Java 21 (AWS Amazon Corretto)
- **Framework**: Spring Boot 3.4.1, Spring Data JPA, Spring Security
- **ORM**: Hibernate 6.x
- **Database**: PostgreSQL 16 (StoLink와 공유, JSONB 활용)
- **Authentication**: JWT + Google OAuth2 (StoLink와 JWT_SECRET 공유)
- **JSON**: Jackson Databind, Hypersistence Utils (JSONB 타입 지원)
- **HTTP Client**: Spring WebFlux WebClient (StoLink 연동)
- **Validation**: Jakarta Validation (Bean Validation 3.0)
- **Utility**: Lombok 1.18.36
- **Build**: Gradle 8.x
- **Container**: Docker, Docker Compose

</tech_stack>

<core_entities>

<!-- com.stolink.backend.domain 기준 -->

- **User**: 사용자 (email, nickname, profileImageUrl) - StoLink와 공유
- **Work**: 작품 (title, synopsis, coverImageUrl, genre, status, ratingSum/Count)
- **Chapter**: 챕터/회차 (title, content, chapterNumber, viewCount, ratingSum/Count, documentId) ⭐ 핵심
- **Comment**: 댓글 (Self-referencing 구조, parentId로 대댓글 관리, likeCount)
- **ChapterRating**: 챕터 별점 (1~10점, 사용자당 1회)
- **CommentLike**: 댓글 좋아요 (중복 방지)
- **Library**: 선호 작품 (내 서재)
- **Bookmark**: 읽은 위치 저장 (이어읽기)
- **Draft**: 임시저장 원고 (StoLink → StoRead 배포 중간 단계)

</core_entities>

<stolink_integration>

<!-- StoLink 연동 -->

- **공유 인프라**: PostgreSQL (stolink_spring 제공)
- **JWT 공유**: 동일한 JWT_SECRET으로 SSO 구현
- **배포 흐름**: StoLink 에디터 → Draft → Work/Chapter 생성
- **Document 연동**: Chapter.documentId로 StoLink 문서와 연결
- **isPublished 동기화**: 배포 시 StoLink 문서의 isPublished 상태 업데이트

</stolink_integration>
</project_info>

---

<coding_rules>
<java>

- MUST: Java 21 기능 활용 (Record, Pattern Matching, Virtual Thread 고려)
- MUST: Lombok 사용 시 `@Builder`, `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 패턴 준수
- MUST: 불변 객체 지향 - DTO는 Record 또는 `@Value` 사용 권장
- MUST: 메서드/클래스당 100줄 이내 유지
- SHOULD: Optional 반환 시 `orElseThrow()` 보다 명시적 예외 처리
- MUST NOT: Primitive type의 null 반환 (`int` 대신 `Integer` 사용 시 null 체크)
- MUST NOT: `System.out.println` 사용 금지 - SLF4J Logger 사용
- MUST NOT: Magic Number/String 직접 사용 - 상수 또는 Enum 정의
  </java>

<spring>
<!-- Spring Boot 3.4.x 특성 반영 -->
- MUST: Controller → Service → Repository 레이어 분리 엄수
- MUST: DTO와 Entity 분리 (Entity 직접 노출 금지)
- MUST: `@Transactional` 범위 최소화 (읽기 전용은 `readOnly = true`)
- MUST: 예외는 `@RestControllerAdvice`에서 전역 처리
- SHOULD: 응답 형식 통일 - `ApiResponse<T>` 래퍼 사용
- MUST NOT: Controller에서 비즈니스 로직 구현
- MUST NOT: Repository에서 직접 HTTP 응답 반환
- MUST NOT: 엔티티에서 양방향 연관관계 무분별 사용 (N+1 주의)
</spring>

<jpa_hibernate>

<!-- Spring Data JPA + Hibernate 6.x -->

- MUST: Lazy Loading 기본 전략 (@ManyToOne, @OneToMany 등)
- MUST: N+1 문제 방지 - `@EntityGraph` 또는 `fetch join` 사용
- MUST: Batch Insert/Update 활용 (`hibernate.jdbc.batch_size` 설정)
- SHOULD: DTO Projection 활용으로 필요한 컬럼만 조회
- SHOULD: `@Version` 낙관적 락 적용 (동시성 이슈 방지)
- MUST NOT: Open Session In View 안티패턴 (비활성화 확인)
- MUST NOT: Entity에서 equals/hashCode 잘못 구현 (ID 기반 필수)
- MUST NOT: `CascadeType.ALL` 무분별 사용
  </jpa_hibernate>

<postgresql>
<!-- PostgreSQL 16 + Hypersistence Utils -->
- MUST: JSONB 타입은 `@JdbcTypeCode(SqlTypes.JSON)` 적용 (Hibernate 6.x)
- MUST: 인덱스 전략 수립 (자주 조회되는 컬럼)
- SHOULD: 대용량 데이터는 페이지네이션 필수
- SHOULD: EXPLAIN ANALYZE로 쿼리 플랜 검증
- MUST NOT: SELECT * 사용 (필요한 컬럼만 조회)
- MUST NOT: OFFSET 기반 대용량 페이지네이션 (Keyset 사용 권장)
</postgresql>

<security>

<!-- Spring Security + JWT + OAuth2 -->

- MUST: JWT 토큰은 쿠키 기반으로 관리 (HttpOnly, Secure)
- MUST: Access Token / Refresh Token 분리
- MUST: 민감 정보 (JWT_SECRET) 환경변수로 관리 - 코드에 하드코딩 금지
- MUST: StoLink와 동일한 JWT_SECRET 사용 (SSO)
- SHOULD: API 권한 검증 시 @PreAuthorize 또는 커스텀 어노테이션 활용
- MUST NOT: 인증 없이 작가 전용 API (works, chapters 등) 접근 허용
- MUST NOT: 다른 사용자의 작품/댓글 수정/삭제 허용
  </security>

<stolink_api>

<!-- StoLink API 연동 -->

- MUST: WebFlux WebClient 사용 (RestTemplate 지양)
- MUST: 타임아웃 설정 (connection, read, write)
- SHOULD: 배포 시 StoLink 문서 isPublished 상태 동기화
- MUST NOT: 동기 블로킹 호출로 스레드 고갈
- MUST NOT: StoLink 서비스 장애가 전체 시스템 장애로 전파
  </stolink_api>

<chapter_management>

<!-- 챕터 순서 관리 -->

- MUST: 삭제 시 해당 작품의 더 큰 chapter_number를 가진 회차 번호 -1
- MUST: 중간 삽입 시 삽입 지점 이후 회차 번호 +1
- MUST: 순서 변경은 트랜잭션 내에서 원자적으로 처리
- MUST NOT: 동일 작품 내 chapter_number 중복 허용
  </chapter_management>

<performance>
<!-- 데이터 처리 성능 최적화 -->
- MUST: Connection Pool 적정 크기 설정 (HikariCP)
- MUST: 벌크 연산 시 `@Modifying` + `clearAutomatically = true`
- SHOULD: 응답 압축 (GZIP) 활성화
- SHOULD: 페이지네이션 기본 적용 (limit 20)
- SHOULD: 조회 API는 DTO Projection으로 최적화
- SHOULD: 댓글 로딩은 무한 스크롤 (Intersection Observer) + 페이지네이션
- MUST NOT: 루프 안에서 DB 쿼리 실행 (Batch 쿼리로 변환)
- MUST NOT: 대용량 데이터 메모리 로딩 (Stream/Cursor 사용)
</performance>

<naming>
- 패키지: 소문자 (예: `com.stolink.backend.domain.chapter`)
- 클래스: PascalCase (예: `ChapterService`)
- 메서드: camelCase, 동사로 시작 (예: `findByWorkId`, `createChapter`)
- 인터페이스: I 접두사 지양 (예: `ChapterRepository`, not `IChapterRepository`)
- DTO: 용도 명시 (예: `CreateChapterRequest`, `ChapterResponse`)
- Entity: 테이블명과 일치, 단수형 (예: `Chapter`, `Work`)
- 상수: UPPER_SNAKE_CASE (예: `DEFAULT_PAGE_SIZE`)
- Enum: PascalCase, 값은 UPPER_SNAKE_CASE
</naming>

<testing>
- MUST: Service 레이어 단위 테스트 필수
- MUST: `@DataJpaTest`로 Repository 테스트
- SHOULD: `@WebMvcTest`로 Controller 테스트
- SHOULD: Testcontainers로 통합 테스트 (PostgreSQL)
- MUST NOT: 프로덕션 DB에 테스트 수행
- MUST NOT: 테스트 간 상태 공유 (격리 필수)
</testing>
</coding_rules>

---

<restrictions>
<!-- 이것만은 절대 하지 마 (Negative Constraints) -->

🔴 **MUST NOT (절대 금지)**:

- Entity 직접 API 응답으로 반환 (DTO 변환 필수)
- `System.out.println` 로깅 (SLF4J Logger 사용)
- Controller에서 비즈니스 로직 구현
- N+1 쿼리 발생 (fetch join 또는 @EntityGraph)
- SELECT \* 쿼리 사용
- 루프 내 DB 쿼리 실행
- 트랜잭션 범위 내 외부 API 호출
- main/develop 브랜치에 직접 push
- PR 없이 main에 머지
- 프로덕션 DB 직접 조작
- 하드코딩된 credential (JWT_SECRET 등)
- 다른 사용자의 작품/댓글 수정/삭제 허용
- chapter_number 중복 허용

⚠️ **SHOULD NOT (지양)**:

- 500줄 이상의 단일 파일
- Native Query 사용 (JPQL 우선)
- CascadeType.ALL 무분별 사용
- 양방향 연관관계 과도한 사용
- OFFSET 기반 대용량 페이지네이션
- 동기 블로킹 외부 API 호출
- 테스트 없는 코드 커밋
  </restrictions>

---

<workflow_protocol>

<!-- AI 모델이 따라야 할 단계별 프로토콜 -->

1. **Analyze (분석)**

   - 사용자 요청을 파악하고 관련 파일 경로 확인
   - 기존 코드베이스에서 유사한 패턴 검색
   - domain/{도메인}/service, repository 에서 기존 로직 확인
   - StoLink 연동 여부 확인 (Draft, isPublished 등)

2. **Plan (계획 수립)**

   - 변경 계획을 단계별로 수립
   - 영향 받는 Entity, DTO, Service, Repository, Controller 나열
   - 데이터베이스 스키마 변경 필요 여부 확인
   - N+1 문제, 트랜잭션 범위, 동시성 이슈 사전 검토

3. **Implement (구현)**

   - 계획에 따라 코드 작성
   - Entity → DTO → Repository → Service → Controller 순서
   - 예외 처리 GlobalExceptionHandler에 추가
   - 기존 API 응답 형식(ApiResponse) 준수

4. **Verify (검증)**
   - 컴파일 에러 확인 (`./gradlew compileJava`)
   - 테스트 실행 (`./gradlew test`)
   - API 동작 확인 (`./gradlew bootRun` + curl/Postman 테스트)
   - 쿼리 로그 확인 (N+1 체크)
     </workflow_protocol>

---

<branch_strategy>

<!-- 3-Layer 브랜치 전략 -->

| 브랜치      | 용도      | 직접 Push | PR 대상          |
| ----------- | --------- | --------- | ---------------- |
| `main`      | 프로덕션  | ❌ 금지   | hotfix/\*        |
| `develop`   | 개발 통합 | ❌ 금지   | feature/_, fix/_ |
| `feature/*` | 기능 개발 | ✅ 허용   | → develop        |
| `fix/*`     | 버그 수정 | ✅ 허용   | → develop        |
| `hotfix/*`  | 긴급 수정 | ✅ 허용   | → main           |

</branch_strategy>

---

<commit_convention>

<!-- Conventional Commits -->

```
feat: 새 기능 추가
fix: 버그 수정
docs: 문서 변경
style: 코드 포맷팅 (동작 변화 X)
refactor: 리팩토링 (동작 변화 X)
perf: 성능 개선
test: 테스트 추가
chore: 빌드, 설정, 의존성 변경
ci: CI/CD 설정 변경
hotfix: 긴급 수정
db: 데이터베이스 스키마/마이그레이션 변경
```

**예시**:

- `feat(chapter): 챕터 배포 API 추가`
- `fix(comment): 대댓글 삭제 시 NPE 수정`
- `perf(discovery): 작품 목록 조회 N+1 해결`
- `db(rating): chapter_ratings 테이블 인덱스 추가`
  </commit_convention>

---

<ai_code_review>

<!--
  이 섹션은 GitHub Actions의 ai-review.yml 워크플로우에서 사용됩니다.
  수정 시 워크플로우에도 영향을 미칩니다.
-->

## AI 코드 리뷰어 페르소나

당신은 **StoRead 백엔드 프로젝트의 시니어 백엔드 개발자이자 DBA 전문가**입니다.

### 프로젝트 컨텍스트

- Spring Boot 3.4.1 기반 REST API 서버
- PostgreSQL 16 (StoLink와 공유)
- JWT + OAuth2 인증 (StoLink와 SSO)
- 기술 스택: Java 21, Spring Boot 3.4.1, Spring Data JPA, Hibernate 6.x
- 핵심 엔티티: User, Work, Chapter, Comment, ChapterRating, Library, Bookmark

### 리뷰 우선순위

1. **치명적** (🔴): 런타임 에러, SQL Injection, N+1 쿼리, 데이터 정합성 이슈
2. **경고** (⚠️): 성능 이슈, 트랜잭션 범위 문제, 안티패턴
3. **제안** (💡): 코드 스타일, 리팩토링 (선택사항)

## 🔴 치명적 (즉시 수정)

- N+1 쿼리 발생 (EXPLAIN ANALYZE 확인)
- SQL Injection 취약점
- 트랜잭션 내 외부 API 호출
- Entity 직접 API 응답 노출 (민감 정보 유출)
- 데드락 가능성 있는 코드
- 동시성 이슈 (낙관적/비관적 락 미적용)
- 메모리 누수 (Stream close 누락, 대용량 데이터 로딩)
- chapter_number 중복/불연속 발생 가능성
- 인증/인가 우회 가능성
- 다른 사용자의 작품/댓글/별점 조작 가능성

## ⚠️ 경고 (권장 수정)

- SELECT \* 사용 (필요한 컬럼만 조회)
- OFFSET 기반 대용량 페이지네이션
- 트랜잭션 범위 과도하게 넓음
- 루프 내 DB 쿼리 실행
- 불필요한 Lazy Loading (DTO Projection 권장)
- 인덱스 누락 가능성 (WHERE 조건 분석)
- 예외 처리 누락
- 500줄 이상의 단일 파일

## 💡 제안 (선택)

- 코드 스타일 개선
- 리팩토링 기회
- 더 나은 패턴 제안
- 캐시 적용 가능 지점

## 출력 규칙

1. 🔴 치명적, ⚠️ 경고가 하나라도 있으면 해당 섹션 출력
2. 🔴, ⚠️가 없으면 '✅ 코드 리뷰 통과 - 수정 필요 사항 없음' 출력
3. 💡 제안은 선택사항이므로 '수정 필요'로 취급하지 않음

## 출력 형식

```
### 🔴 치명적 (N건)
**파일:라인** - 이슈 제목
- 문제: 설명
- 개선: 코드 예시

### ⚠️ 경고 (N건)
**파일:라인** - 이슈 제목
> 설명

---
💡 **참고 제안** (선택사항)
- 제안 내용
```

</ai_code_review>

---

<file_structure>

<!-- 2026.01.08 기준 실제 구조 -->

```
storead_spring/
├── src/main/java/com/stolink/backend/
│   ├── BackendApplication.java              # Spring Boot 메인 클래스
│   ├── domain/                              # 도메인별 모듈
│   │   ├── user/                            # 사용자/인증 도메인
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── work/                            # 작품 도메인 (작가용)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── chapter/                         # 챕터 도메인 ⭐ 핵심
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── comment/                         # 댓글 도메인 (계층형)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── like/                            # 댓글 좋아요 도메인
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   └── dto/
│   │   ├── rating/                          # 챕터 별점 도메인
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/
│   │   ├── library/                         # 선호 작품 도메인 (내 서재)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/
│   │   ├── bookmark/                        # 북마크 도메인 (이어읽기)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/
│   │   ├── discovery/                       # 탐색 도메인 (Public API)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   └── dto/
│   │   ├── draft/                           # 임시저장 도메인 (StoLink 연동)
│   │   │   ├── controller/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   └── entity/
│   │   ├── community/                       # 커뮤니티 서비스 (StoLink 연동)
│   │   │   └── service/
│   │   └── payment/                         # 결제 도메인 (예정)
│   │       └── ...
│   └── global/                              # 전역 설정
│       ├── common/                          # 공통 응답, 예외, 엔티티
│       │   ├── ApiResponse.java
│       │   ├── GlobalExceptionHandler.java
│       │   ├── ErrorCode.java
│       │   └── BaseEntity.java
│       ├── config/                          # 설정 클래스
│       │   ├── WebConfig.java               # CORS 설정
│       │   ├── JpaConfig.java
│       │   └── WebClientConfig.java
│       ├── security/                        # JWT, Spring Security
│       │   ├── JwtTokenProvider.java
│       │   ├── JwtAuthenticationFilter.java
│       │   └── SecurityConfig.java
│       └── util/                            # 유틸리티
│           └── SecurityUtils.java
├── src/main/resources/
│   └── application.yml                      # 설정 파일
├── build.gradle                             # Gradle 빌드 설정
├── docker-compose.yml                       # 프로덕션 배포
├── docker-compose.local.yml                 # 로컬 개발 (StoLink 인프라 사용)
├── docker-compose.standalone.yml            # 독립 실행 (자체 DB)
├── Dockerfile                               # 프로덕션 컨테이너
└── *.md                                     # 문서
```

</file_structure>

---

<commands>
<!-- 자주 사용하는 명령어 -->

| 명령어                   | 설명                                      |
| ------------------------ | ----------------------------------------- |
| `./gradlew bootRun`      | 개발 서버 시작 (localhost:8081)           |
| `./gradlew build`        | 프로덕션 빌드                             |
| `./gradlew compileJava`  | 컴파일만 수행                             |
| `./gradlew test`         | 테스트 실행                               |
| `./gradlew clean build`  | 클린 빌드                                 |
| `docker-compose up -d`   | 프로덕션 컨테이너 시작                    |
| `docker-compose -f docker-compose.local.yml up -d` | 로컬 개발 (StoLink 인프라 사용) |

</commands>

---

<database_config>

<!-- 개발 환경 데이터베이스 설정 -->

### PostgreSQL (StoLink 공유)

- **Host**: localhost:5432
- **Database**: stolink (공유)
- **User**: stolink
- **Password**: stolink123

### 포트 설정

| 환경      | StoRead Backend      | StoLink Backend | PostgreSQL |
| --------- | -------------------- | --------------- | ---------- |
| 로컬 개발 | 8081                 | 8080            | 5432       |
| EC2 배포  | 8080 (별도 인스턴스) | 8080            | RDS        |

</database_config>

---

<api_quick_reference>

<!-- 핵심 API 엔드포인트 요약 -->

| 도메인    | GET                                   | POST                                | PATCH                 | DELETE                |
| --------- | ------------------------------------- | ----------------------------------- | --------------------- | --------------------- |
| Auth      | `/api/auth/me`                        | `/api/auth/login`, `/register`      | `/api/auth/me`        | -                     |
| Works     | `/api/works`                          | `/api/works`                        | `/api/works/:id`      | `/api/works/:id`      |
| Chapters  | `/api/works/:wid/chapters`            | `/api/works/:wid/chapters`          | `/api/chapters/:id`   | `/api/chapters/:id`   |
| Discovery | `/api/discovery`, `/discovery/search` | -                                   | -                     | -                     |
| Rating    | `/api/chapters/:id/rating`            | `/api/chapters/:id/rating`          | -                     | `/api/chapters/:id/rating` |
| Comments  | `/api/chapters/:cid/comments`         | `/api/chapters/:cid/comments`       | -                     | `/api/comments/:id`   |
| Replies   | `/api/comments/:id/replies`           | `/api/comments/:id/replies`         | -                     | -                     |
| Like      | -                                     | `/api/comments/:id/like` (토글)     | -                     | -                     |
| Library   | `/api/library`                        | `/api/library/:workId`              | -                     | `/api/library/:workId`|
| Bookmark  | `/api/bookmarks/:chapterId`           | `/api/bookmarks/:chapterId`         | -                     | -                     |
| Draft     | -                                     | `/api/works/:wid/drafts`, `/drafts/bulk` | -                 | -                     |

</api_quick_reference>

---

<env_variables>

<!-- 환경 변수 -->

### 필수

| 변수명                 | 설명                                       |
| ---------------------- | ------------------------------------------ |
| `JWT_SECRET`           | JWT 서명 키 (stolink_spring과 동일해야 함) |
| `POSTGRESQL_URL`       | PostgreSQL 호스트                          |
| `POSTGRESQL_PORT`      | PostgreSQL 포트 (기본: 5432)               |
| `POSTGRESQL_USERNAME`  | PostgreSQL 사용자명                        |
| `POSTGRESQL_PASSWORD`  | PostgreSQL 비밀번호                        |
| `GOOGLE_CLIENT_ID`     | Google OAuth2 클라이언트 ID                |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 클라이언트 시크릿            |

### 선택

| 변수명                 | 설명                  | 기본값                                |
| ---------------------- | --------------------- | ------------------------------------- |
| `JWT_COOKIE_DOMAIN`    | JWT 쿠키 도메인       | localhost                             |
| `CORS_ALLOWED_ORIGINS` | CORS 허용 origins     | http://localhost:3000,5173,5174       |
| `OAUTH2_REDIRECT_URI`  | OAuth2 리다이렉트 URI | http://localhost:5174/oauth2/callback |
| `STOLINK_API_URL`      | StoLink API 주소      | http://localhost:8080                 |

</env_variables>

---

<reference_docs>

<!-- 참고 문서 (SSOT) -->

| 문서                             | 내용                          |
| -------------------------------- | ----------------------------- |
| `README.md`                      | 프로젝트 개요 및 시작 가이드  |
| `storead.md`                     | 기능 명세 및 ERD              |
| `FRONTEND_INTEGRATION_GUIDE.md` | 프론트엔드 연동 가이드        |
| `docs/`                          | 추가 문서들                   |

</reference_docs>

---

<request_guidelines>

<!-- 요청 시 주의사항 -->

1. 새 API는 기존 응답 형식(`ApiResponse<T>`)과 호환성 확인
2. Entity 변경 시 기존 데이터 마이그레이션 계획 수립
3. 쿼리 추가 시 N+1 문제 사전 검토 (`@EntityGraph` 또는 fetch join)
4. StoLink 연동 로직 확인 (Draft, isPublished 동기화)
5. 챕터 순서 변경 시 트랜잭션 내 원자적 처리
6. 성능 민감한 API는 페이지네이션 필수 적용
7. 댓글은 계층형 구조 (Self-referencing) 유지
8. 응답은 한국어로 작성
   </request_guidelines>

---

## 버전 이력

| 버전 | 날짜       | 변경 내용                                                                                |
| ---- | ---------- | ---------------------------------------------------------------------------------------- |
| 1.0  | 2026.01.08 | **최초 작성** - StoRead 백엔드 전용, StoLink 연동, JWT SSO, 소셜 기능 도메인 정의        |
