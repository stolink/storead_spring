-- =============================================================================
-- Storead 데모 데이터 삽입 SQL
-- 사용법: docker exec -i stolink-postgres-local psql -U stolink -d stolink < add_demo_works.sql
-- =============================================================================

-- 먼저 기존 사용자 ID 확인
-- SELECT id, email FROM users LIMIT 3;

-- 변수 설정 (실제 사용자 ID로 교체 필요)
-- 아래 SQL은 사용자가 이미 존재한다고 가정

-- =============================================================================
-- 장르별 6번째 작품 추가 (5개 장르 x 1개 = 5개 작품)
-- =============================================================================

-- 먼저 사용자 ID를 가져옴
WITH user_ids AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) as rn FROM users LIMIT 3
)

-- FANTASY 장르 6번째 작품
INSERT INTO works (id, author_id, title, synopsis, cover_image_url, genre, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    (SELECT id FROM user_ids WHERE rn = 3),
    '최강 마법검사',
    '마법과 검술을 동시에 다루는 천재가 세계의 불가사의를 탐험하며 성장하는 이야기. 미지의 던전에서 숨겨진 보물을 찾아나선다.',
    'https://picsum.photos/seed/fantasy6/400/600',
    'FANTASY',
    'ONGOING',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM works WHERE title = '최강 마법검사');

-- ROMANCE 장르 6번째 작품
INSERT INTO works (id, author_id, title, synopsis, cover_image_url, genre, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    (SELECT id FROM users ORDER BY created_at LIMIT 1),
    '달빛 아래 프러포즈',
    '여행 중 우연히 만난 두 사람. 짧은 만남이 평생의 인연이 될 줄은 몰랐다. 운명 같은 재회와 로맨틱한 고백의 순간.',
    'https://picsum.photos/seed/romance6/400/600',
    'ROMANCE',
    'COMPLETED',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM works WHERE title = '달빛 아래 프러포즈');

-- MARTIAL_ARTS 장르 6번째 작품
INSERT INTO works (id, author_id, title, synopsis, cover_image_url, genre, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    (SELECT id FROM users ORDER BY created_at LIMIT 1 OFFSET 1),
    '비검도',
    '비 오는 날에만 사용할 수 있는 전설의 검법. 그 비기를 전수받은 젊은이가 강호에 새로운 전설을 쓴다.',
    'https://picsum.photos/seed/martial6/400/600',
    'MARTIAL_ARTS',
    'ONGOING',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM works WHERE title = '비검도');

-- MODERN_FANTASY 장르 6번째 작품
INSERT INTO works (id, author_id, title, synopsis, cover_image_url, genre, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    (SELECT id FROM users ORDER BY created_at LIMIT 1),
    '소서러 인 서울',
    '서울 한복판에서 마법 전쟁이 일어난다. 도시를 지키기 위해 나선 현대 마법사들의 액션 판타지.',
    'https://picsum.photos/seed/modernfantasy6/400/600',
    'MODERN_FANTASY',
    'COMPLETED',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM works WHERE title = '소서러 인 서울');

-- MYSTERY 장르 6번째 작품
INSERT INTO works (id, author_id, title, synopsis, cover_image_url, genre, status, created_at, updated_at)
SELECT 
    gen_random_uuid(),
    (SELECT id FROM users ORDER BY created_at LIMIT 1 OFFSET 2),
    '그 해 겨울의 진실',
    '10년 전 겨울, 한 마을에서 일어난 대형 실종 사건. 당시 어린이였던 주인공이 성인이 되어 진실을 파헤치기 시작한다.',
    'https://picsum.photos/seed/mystery6/400/600',
    'MYSTERY',
    'ONGOING',
    NOW(),
    NOW()
WHERE NOT EXISTS (SELECT 1 FROM works WHERE title = '그 해 겨울의 진실');

-- 결과 확인
SELECT genre, COUNT(*) as count FROM works GROUP BY genre ORDER BY genre;
