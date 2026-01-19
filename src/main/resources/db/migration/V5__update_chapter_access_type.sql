-- V5: 기존 챕터 데이터에 accessType 업데이트
-- isFree 값을 기반으로 accessType을 설정합니다.

-- 무료 챕터 (isFree = true)는 accessType을 'FREE'로 설정
UPDATE chapters
SET access_type = 'FREE'
WHERE is_free = true;

-- 유료 챕터 (isFree = false)는 accessType을 'PAID'로 설정
UPDATE chapters
SET access_type = 'PAID'
WHERE is_free = false;

-- accessType이 NULL인 경우 기본값 'FREE'로 설정 (혹시 모를 누락 방지)
UPDATE chapters
SET access_type = 'FREE'
WHERE access_type IS NULL;
