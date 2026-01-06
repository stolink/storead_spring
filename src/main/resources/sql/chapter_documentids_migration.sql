-- Chapter 테이블에 document_ids 컬럼 추가 (병합 배포 지원)
-- 기존 document_id 컬럼은 유지 (하위 호환성 - 시나리오 A, B)
-- 실행 전 반드시 백업 권장

-- 1. 새 컬럼 추가 (이미 존재하면 무시)
ALTER TABLE chapters 
ADD COLUMN IF NOT EXISTS document_ids JSONB;

-- 2. GIN 인덱스 생성 (JSONB 배열 검색 최적화)
CREATE INDEX IF NOT EXISTS idx_chapters_document_ids 
ON chapters USING GIN (document_ids);

-- 3. 기존 단일 document_id 제약조건 삭제 
-- (JSONB 배열과 함께 사용 시 앱 레벨에서 중복 체크)
-- 주의: 해당 제약조건이 존재하는 경우에만 실행
ALTER TABLE chapters DROP CONSTRAINT IF EXISTS uk_chapter_work_document;

-- 4. 메타데이터 주석
COMMENT ON COLUMN chapters.document_id IS '게시된 Stolink 문서 ID (단일/각각 배포 - 시나리오 A, B)';
COMMENT ON COLUMN chapters.document_ids IS '게시된 Stolink 문서 ID 목록 (병합 배포 시 JSONB 배열 - 시나리오 C)';

-- 배포 시나리오 참고:
-- A. 단일 문서 배포: documentId 1개, Chapter 1개
-- B. 다중 문서 각각 배포: documentIds N개, Chapter N개 (각 Chapter별 documentId)
-- C. 다중 문서 병합 배포: documentIds N개 + isMerged=true, Chapter 1개 (documentIds 배열)
