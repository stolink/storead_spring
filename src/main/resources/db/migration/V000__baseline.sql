-- Core Tables Baseline

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(100) NOT NULL,
    avatar_url VARCHAR(500),
    provider VARCHAR(50) NOT NULL DEFAULT 'LOCAL',
    provider_id VARCHAR(255),
    goal_notification BOOLEAN NOT NULL DEFAULT TRUE,
    foreshadowing_notification BOOLEAN NOT NULL DEFAULT FALSE,
    ai_suggestion_notification BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS works (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    author_id UUID NOT NULL REFERENCES users (id),
    title VARCHAR(255) NOT NULL,
    synopsis TEXT NOT NULL,
    cover_image_url TEXT,
    genre VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ONGOING',
    project_id VARCHAR(255) UNIQUE,
    average_rating DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    like_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Note: V3 adds rating_sum/rating_count to chapters, so we omit them here if we want V3 to run.
-- OR we include them and make V3 use IF NOT EXISTS?
-- Actually, the best way for a baseline is to include everything up to V0.
CREATE TABLE IF NOT EXISTS chapters (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    work_id UUID NOT NULL REFERENCES works (id),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    chapter_number INTEGER NOT NULL,
    document_id VARCHAR(255),
    document_ids JSONB,
    graph_snapshot JSONB,
    view_count BIGINT NOT NULL DEFAULT 0,
    is_free BOOLEAN NOT NULL DEFAULT TRUE,
    price INTEGER NOT NULL DEFAULT 0,
    access_type VARCHAR(30) NOT NULL DEFAULT 'FREE',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_chapter_work_number UNIQUE (work_id, chapter_number)
);
