CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    category VARCHAR(32) NOT NULL,
    merchant VARCHAR(120),
    occurred_on DATE NOT NULL,
    note VARCHAR(255),
    source VARCHAR(10) NOT NULL CHECK (source IN ('TEXT', 'MANUAL')),
    raw_input TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user_occurred_on ON transactions(user_id, occurred_on DESC);
