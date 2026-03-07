CREATE TABLE IF NOT EXISTS payment_compensations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments (id),
    user_id UUID NOT NULL REFERENCES users (id),
    type VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    credit_amount BIGINT NOT NULL,
    error_message VARCHAR(500),
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_payment_compensations_payment_id ON payment_compensations (payment_id);

CREATE INDEX IF NOT EXISTS idx_payment_compensations_user_id ON payment_compensations (user_id);
