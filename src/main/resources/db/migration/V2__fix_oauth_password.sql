-- OAuth 로그인을 위한 users 테이블 password 컬럼 nullable 설정
-- 실행 방법: PostgreSQL에서 직접 실행하거나, Docker 컨테이너 재생성

-- 방법 1: 기존 테이블 컬럼 수정 (권장)
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;

-- 방법 2: 개발 환경에서 테이블 재생성이 필요한 경우
-- Docker 컨테이너 볼륨을 삭제하고 재시작하면 JPA ddl-auto: update가 올바른 스키마 생성
-- docker compose -f docker-compose.standalone.yml down -v
-- docker compose -f docker-compose.standalone.yml up -d
