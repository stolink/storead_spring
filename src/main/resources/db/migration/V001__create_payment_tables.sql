-- 크레딧 잔액 테이블
CREATE TABLE IF NOT EXISTS credits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    total_charged BIGINT NOT NULL DEFAULT 0,
    total_used BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (user_id)
);

CREATE INDEX IF NOT EXISTS idx_credits_user_id ON credits(user_id);

-- 결제 테이블
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),

    -- 주문 정보
    order_id VARCHAR(64) NOT NULL UNIQUE,
    order_name VARCHAR(100) NOT NULL,

    -- 결제 금액
    amount BIGINT NOT NULL CHECK (amount > 0),
    credit_amount BIGINT NOT NULL,

    -- 토스 페이먼츠 정보
    payment_key VARCHAR(200),
    payment_method VARCHAR(50),

    -- 상태 관리
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    -- 취소 정보
    canceled_amount BIGINT DEFAULT 0,
    cancel_reason VARCHAR(200),

    -- 실패 정보
    failure_code VARCHAR(100),
    failure_message VARCHAR(500),

    -- 멱등성 키
    idempotency_key VARCHAR(64) NOT NULL UNIQUE,

    -- 타임스탬프
    requested_at TIMESTAMP NOT NULL DEFAULT NOW(),
    approved_at TIMESTAMP,
    canceled_at TIMESTAMP,
    expired_at TIMESTAMP,

    -- 메타데이터
    metadata JSONB,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT chk_canceled_amount CHECK (canceled_amount <= amount)
);

CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);
CREATE INDEX IF NOT EXISTS idx_payments_order_id ON payments(order_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_key ON payments(payment_key);
CREATE INDEX IF NOT EXISTS idx_payments_requested_at ON payments(requested_at DESC);

-- 크레딧 거래 내역 테이블
CREATE TABLE IF NOT EXISTS credit_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    credit_id UUID NOT NULL REFERENCES credits(id),
    payment_id UUID REFERENCES payments(id),

    -- 거래 정보
    type VARCHAR(30) NOT NULL,
    amount BIGINT NOT NULL,
    balance_before BIGINT NOT NULL,
    balance_after BIGINT NOT NULL,

    -- 상세 정보
    description VARCHAR(200),
    reference_type VARCHAR(50),
    reference_id VARCHAR(100),

    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_balance_consistency CHECK (balance_after = balance_before + amount)
);

CREATE INDEX IF NOT EXISTS idx_credit_transactions_user_id ON credit_transactions(user_id);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_credit_id ON credit_transactions(credit_id);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_type ON credit_transactions(type);
CREATE INDEX IF NOT EXISTS idx_credit_transactions_created_at ON credit_transactions(created_at DESC);

-- 웹훅 로그 테이블
CREATE TABLE IF NOT EXISTS payment_webhook_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- 웹훅 정보
    event_type VARCHAR(50) NOT NULL,
    payment_key VARCHAR(200),
    order_id VARCHAR(64),

    -- 처리 정보
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',

    -- 요청/응답 데이터
    request_body JSONB NOT NULL,
    response_body JSONB,
    error_message VARCHAR(500),

    -- 재시도 정보
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP,

    received_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_webhook_logs_payment_key ON payment_webhook_logs(payment_key);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_status ON payment_webhook_logs(status);
CREATE INDEX IF NOT EXISTS idx_webhook_logs_event_type ON payment_webhook_logs(event_type);
