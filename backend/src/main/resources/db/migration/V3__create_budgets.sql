CREATE TABLE budgets (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category VARCHAR(32) NOT NULL,
    month CHAR(7) NOT NULL,
    amount_paise BIGINT NOT NULL CHECK (amount_paise >= 0),
    CONSTRAINT uq_budgets_user_category_month UNIQUE (user_id, category, month)
);

CREATE INDEX idx_budgets_user_month ON budgets(user_id, month);
