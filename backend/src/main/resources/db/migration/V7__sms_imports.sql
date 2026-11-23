ALTER TABLE transactions DROP CONSTRAINT transactions_source_check;
ALTER TABLE transactions ADD CONSTRAINT transactions_source_check CHECK (source IN ('TEXT', 'MANUAL', 'SMS'));

CREATE TABLE sms_imports (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    message_hash VARCHAR(64) NOT NULL,
    transaction_id UUID REFERENCES transactions(id) ON DELETE SET NULL,
    type VARCHAR(10) NOT NULL CHECK (type IN ('DEBIT', 'CREDIT', 'UNKNOWN')),
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    occurred_on DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, message_hash)
);

CREATE INDEX idx_sms_imports_user_created_at ON sms_imports(user_id, created_at DESC);
