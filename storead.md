# 1. 개요

- **목적**: 작가가 관리하던 복선, 캐릭터, 설정 기반의 원고를 외부로 배포하고, 독자가 이를 소비하며 피드백을 남길 수 있는 소셜 기능 강화.
- **주요 타겟**:
  - **작가**: 체계적으로 관리된 설정을 바탕으로 작품을 연재하고자 하는 창작자.
  - **독자**: 고퀄리티의 설정과 정합성이 검증된 작품을 즐기고 소통하고 싶은 이용자.

---

## 2. 주요 기능 상세 (Functional Requirements)

### 2.1. 작가: 작품 배포 및 관리 (Writer Side)

- **작품 메타데이터 설정**:
  - 신규 작품 등록 시 **표지 이미지(Upload), 장르(분류), 줄거리(Synopsis)** 입력.
  - 기존 에디터에서 작성 중인 프로젝트와 연동.
- **원고 내보내기 (Export to Publish)**:
  - 에디터 내 작성된 글을 선택하여 '공개용 챕터(회차)'로 전환.
  - 출판 시 타겟 폴더(회차)를 지정하여 순차적 연재 지원.

### 2.2. 독자: 탐색 및 열람 (Reader Side)

- **통합 작품 홈 (Discovery Area)**:
  - 서비스 접속 시 나타나는 신규 페이지. 모든 공개 작품이 카드 형태로 노출.
  - **검색 기능**: 제목, 작가명, 키워드 기반의 실시간 검색.
- **작품 상세 페이지 (Index Page)**:
  - 상단: 작가가 설정한 표지, 제목, 줄거리 표시.
  - 하단: 챕터별 리스트(회차명, 등록일자) 제공.
- **콘텐츠 뷰어 (Secure Viewer)**:
  - **보안 강화**: 마우스 우클릭 금지, 드래그 및 복사/붙여넣기 방지 스크립트 적용.
  - **가독성 설정:** 배경색(라이트/다크/세피아), 폰트 크기, 줄 간격 개인화 설정.
  - 글 읽기에 최적화된 심플한 UI.
  - 북마크 및 이어읽기
- **선호 작품 등록 (라이브러리):** 관심 있는 작품을 '내 서재'에 담아두고 새 글 알림을 받는 기능을 강화한다.

### 2.3. 커뮤니티: 소통 (Social Interaction)

- **반응(Engagement)**: 챕터별 '좋아요' 기능 (1회 제한).
- **댓글 시스템**:
  - **계층형 댓글**: 댓글에 대한 답글(대댓글) 작성 가능.
  - **다중 작성**: 한 회차에 여러 개의 의견 남기기 가능.
  - 좋아요 기능

---

## 3. 서비스 프로세스 (User Flow)

1. **작가 연재 흐름**:
   [에디터 작성] → [내보내기 클릭] → [작품 정보/챕터 설정] → [배포 완료]
2. **독자 이용 흐름**:
   [메인 홈 접속] → [작품 검색/선택] → [상세 리스트 확인] → [챕터 열람] → [좋아요/댓글 남기기]

---

## 4. 상세 화면 설계 방향 (UI/UX)

| **화면명**    | **주요 구성 요소**                              | **비고**                        |
| ------------- | ----------------------------------------------- | ------------------------------- |
| **공개 홈**   | 인기/신작 배너, 작품 카드(표지+제목), 검색바    | 네이버 웹툰/카카오페이지 스타일 |
| **작품 상세** | 대형 표지 이미지, 줄거리 요약, 회차 목록 테이블 | 최신순/1화부터 보기 정렬 기능   |
| **뷰어**      | 본문 텍스트, 하단 좋아요/댓글 버튼, 다음화 이동 | 드래그 금지 CSS 적용            |
| **댓글창**    | 댓글 입력창, 작성된 댓글 리스트, 답글 달기 버튼 | 계층형 인덴트(들여쓰기) 적용    |

## 5. 기술적 고려 사항 (Technical Requirement)

- **Security**: `user-select: none`, `copy/cut/paste` 이벤트 핸들러 차단 등으로 텍스트 무단 유출 방지.
- **Performance**: 대량의 댓글 로딩 시 성능 저하 방지를 위한 **무한 스크롤(Intersection Observer)** 또는 페이징 처리.
- **DB Design**: 댓글과 대댓글을 한 테이블에서 관리하는 **Self-referencing 구조** 설계.

---

## 6. 향후 확장 계획

- **AI 정합성 검토 알림**: 작가가 배포하기 전, 기존 설정(복선)과 충돌하는 내용이 있는지 AI가 최종 체크.
- **독자 알림**: 선호하는 작가의 새 챕터가 올라왔을 때 푸시 알림 전송.

---

### ERD

Table 1: Works (작품)

| **Field Name**    | **Type**     | **Constraints**    | **Description**         |
| ----------------- | ------------ | ------------------ | ----------------------- |
| `id`              | BigInt       | PK, Auto Increment | 작품 고유 ID            |
| `author_id`       | BigInt       | FK (Users.id)      | 작성자 ID               |
| `title`           | Varchar(255) | Not Null           | 작품 제목               |
| `synopsis`        | Text         | Not Null           | 작품 줄거리             |
| `cover_image_url` | Varchar(512) | -                  | 표지 이미지 경로        |
| `genre`           | Enum         | Not Null           | 판타지, 로맨스, 무협 등 |
| `created_at`      | DateTime     | Default: Now       | 등록일                  |
| `status`          | Enum         | Not Null           | 연재중, 휴재, 완결      |

Table 2: Chapters (챕터/회차)

| **Field Name**   | **Type**     | **Constraints**    | **Description**           |
| ---------------- | ------------ | ------------------ | ------------------------- |
| `id`             | BigInt       | PK, Auto Increment | 챕터 고유 ID              |
| `work_id`        | BigInt       | FK (Works.id)      | 소속 작품 ID              |
| `title`          | Varchar(255) | Not Null           | 챕터 제목 (예: 1화. 시작) |
| `content`        | LongText     | Not Null           | 회차 본문 내용            |
| `chapter_number` | Int          | Not Null           | 회차 순서 (1, 2, 3...)    |
| `created_at`     | DateTime     | Default: Now       | 생성일                    |

+가능하면 나중에 조회수

Table 3: Comments (댓글 및 답글) : **Self-referencing(자기 참조)** 구조

| **Field Name** | **Type** | **Constraints**                              | **Description**                 |
| -------------- | -------- | -------------------------------------------- | ------------------------------- |
| `id`           | BigInt   | PK, Auto Increment                           | 댓글 고유 ID                    |
| `chapter_id`   | BigInt   | FK (Chapters.id)                             | 해당 회차 ID                    |
| `user_id`      | BigInt   | FK (Users.id)                                | 작성자 ID                       |
| `parent_id`    | BigInt   | FK (Comments.id), Nullable                   | **부모 댓글 ID (답글일 경우)**  |
| `content`      | Text     | Not Null                                     | 댓글 내용                       |
| `created_at`   | DateTime | Default: Now                                 | 작성일                          |
| `like_count`   | Bigint   | NOT NULL, DEFAULT 0, CHECK (like_count >= 0) | 해당 항목이 받은 총 좋아요 합계 |

Table 4: Likes (좋아요) : 중복 좋아요를 방지

| **Field Name** | **Type** | **Constraints**    | **Description** |
| -------------- | -------- | ------------------ | --------------- |
| `id`           | BigInt   | PK, Auto Increment | 좋아요 고유 ID  |
| `chapter_id`   | BigInt   | FK (Chapters.id)   | 해당 회차 ID    |
| `user_id`      | BigInt   | FK (Users.id)      | 유저 ID         |
| `created_at`   | DateTime | Default: Now       | 클릭 시점       |

## 4. 기술적 요구사항 및 보안 전략

### 4.1 콘텐츠 보안 (Content Security)

1. **Client-side Block:**
   - `oncontextmenu="return false;"`
   - `onselectstart="return false;"`
   - `onkeydown` 이벤트 감지 (F12, Ctrl+C, Ctrl+Shift+I 차단).
2. **Server-side Protection:**
   - API 호출 시 유효한 세션 및 해당 회차에 대한 접근 권한(구매 여부 등) 검증.
   - 본문 데이터를 청크(Chunk) 단위로 전달하여 전체 로딩 속도 최적화 및 크롤링 난이도 상승.

### 4.2 성능 및 확장성

- **댓글 로딩:** `Intersection Observer`를 이용한 무한 스크롤 구현.
- **계층형 데이터 조회:**
  - SQL 레벨에서 `WITH RECURSIVE` 쿼리를 사용하여 부모-자식 관계 추출.
  - 성능 최적화를 위해 최상위 댓글을 먼저 로드하고 '답글 보기' 클릭 시 하위 댓글을 Lazy Loading 함.

### 4.3 데이터 무결성 및 순서 관리 (Data Integrity)

- **회차 재정렬 로직:** 특정 회차가 삭제될 경우, 해당 작품(`work_id`)의 더 큰 `chapter_number`를 가진 모든 회차의 번호를 `1` 처리하여 순차적 무결성을 유지한다.
- **중간 삽입 처리:** 작가가 중간에 회차를 추가할 경우, 삽입 지점 이후의 모든 회차 번호를 `+1` 처리하는 로직을 서버사이드 트랜잭션으로 보장한다.

## 5. UI/UX 상세 설계 방향

- **홈 화면:** 카드 레이아웃 위주로 구성하며, 마우스 오버 시 작품의 메타데이터를 동적으로 노출하여 클릭률 유도.
- **상세 페이지:** 대형 배너 이미지를 배경으로 사용하되, 가독성을 위해 상단 영역에 블러(Blur) 처리나 그라데이션 적용.
- **뷰어 화면:** 집중도를 위해 상단 내비게이션은 스크롤 시 숨김 처리하고, 하단에 플로팅 바를 배치하여 '좋아요'와 '다음 화' 버튼에 접근하기 쉽게 설계.
