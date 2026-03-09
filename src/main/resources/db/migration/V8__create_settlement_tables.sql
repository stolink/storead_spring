-- V8: 정산(Settlement) 도메인 테이블 생성

-- 작가별 수익 잔액
CREATE TABLE IF NOT EXISTS author_revenues (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL UNIQUE,
    total_earned BIGINT NOT NULL DEFAULT 0,
    total_settled BIGINT NOT NULL DEFAULT 0,
    pending_balance BIGINT NOT NULL DEFAULT 0,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_author_revenue_author ON author_revenues(author_id);

-- 개별 수익 거래 기록
CREATE TABLE IF NOT EXISTS revenue_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL,
    work_id UUID NOT NULL,
    chapter_id UUID NOT NULL,
    buyer_user_id UUID NOT NULL,
    purchase_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    credit_amount INTEGER NOT NULL,
    platform_fee_rate DOUBLE PRECISION NOT NULL,
    platform_fee INTEGER NOT NULL,
    author_share INTEGER NOT NULL,
    settlement_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_revenue_tx_author ON revenue_transactions(author_id);
CREATE INDEX IF NOT EXISTS idx_revenue_tx_author_period ON revenue_transactions(author_id, created_at);
CREATE INDEX IF NOT EXISTS idx_revenue_tx_work ON revenue_transactions(work_id);
CREATE INDEX IF NOT EXISTS idx_revenue_tx_settlement ON revenue_transactions(settlement_id);
ALTER TABLE revenue_transactions ADD CONSTRAINT uk_revenue_tx_purchase UNIQUE (purchase_id, type);

-- 정산 배치
CREATE TABLE IF NOT EXISTS settlements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id UUID NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    gross_amount BIGINT NOT NULL,
    platform_fee_total BIGINT NOT NULL,
    net_amount BIGINT NOT NULL,
    transaction_count INTEGER NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    confirmed_at TIMESTAMP,
    completed_at TIMESTAMP,
    version BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_settlement_author ON settlements(author_id);
CREATE INDEX IF NOT EXISTS idx_settlement_status ON settlements(status);
CREATE INDEX IF NOT EXISTS idx_settlement_period ON settlements(author_id, period_start, period_end);
ALTER TABLE settlements ADD CONSTRAINT uk_settlement_author_period UNIQUE (author_id, period_start, period_end);
