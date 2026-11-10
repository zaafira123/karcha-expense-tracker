CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    agent_cap_paise BIGINT NOT NULL DEFAULT 200000
);

CREATE INDEX idx_users_email ON users(email);
