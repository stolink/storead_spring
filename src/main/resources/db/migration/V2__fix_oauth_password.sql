-- OAuth 로그인을 위한 users 테이블 password 컬럼 nullable 설정
ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
