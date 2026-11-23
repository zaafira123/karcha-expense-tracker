-- V5__create_agent_tables.sql
-- Kharcha: Budget agent proposals and audit logging

CREATE TABLE agent_proposals (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    month CHAR(7) NOT NULL,
    from_category VARCHAR(32) NOT NULL,
    to_category VARCHAR(32) NOT NULL,
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    shortfall_paise BIGINT NOT NULL,
    donor_spare_paise BIGINT NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'APPLIED', 'UNDONE', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    decided_at TIMESTAMPTZ NULL,
    applied_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_agent_proposals_user_status ON agent_proposals (user_id, status);
CREATE INDEX idx_agent_proposals_user_month ON agent_proposals (user_id, month);

CREATE TABLE agent_audit (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    proposal_id UUID NULL REFERENCES agent_proposals(id) ON DELETE SET NULL,
    event VARCHAR(20) NOT NULL CHECK (event IN ('PROPOSED', 'APPROVED', 'REJECTED', 'APPLIED', 'UNDONE', 'EXPIRED')),
    detail TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_audit_user_created ON agent_audit (user_id, created_at DESC);
