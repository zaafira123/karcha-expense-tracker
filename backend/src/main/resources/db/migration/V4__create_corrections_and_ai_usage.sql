-- V4__create_corrections_and_ai_usage.sql
-- Kharcha: Learned merchant categories and AI usage tracking

CREATE TABLE user_merchant_categories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    merchant_key VARCHAR(120) NOT NULL,
    category VARCHAR(32) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_merchant UNIQUE (user_id, merchant_key)
);

CREATE INDEX idx_user_merchant_categories ON user_merchant_categories (user_id, merchant_key);

CREATE TABLE ai_usage (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    usage_date DATE NOT NULL,
    calls INT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_ai_usage_date UNIQUE (user_id, usage_date)
);

CREATE INDEX idx_ai_usage_user_date ON ai_usage (user_id, usage_date);
