-- V6: Add budget snapshot columns to agent_proposals
-- These are captured at approve time so undo can detect changes.
ALTER TABLE agent_proposals
    ADD COLUMN donor_budget_before_paise   BIGINT,
    ADD COLUMN receiver_budget_before_paise BIGINT;
