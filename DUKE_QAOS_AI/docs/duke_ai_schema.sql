-- duke_ai_schema.sql
CREATE DATABASE duke_ai_production;

\c duke_ai_production;

CREATE TABLE system_logs (
    id BIGSERIAL PRIMARY KEY,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    level VARCHAR(10) NOT NULL,
    component VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB
);

CREATE TABLE user_identities (
    user_id VARCHAR(255) PRIMARY KEY,
    public_key_hash VARCHAR(255) NOT NULL,
    mfa_enabled BOOLEAN DEFAULT true,
    last_verified TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_logs_timestamp ON system_logs(timestamp);
CREATE INDEX idx_logs_level ON system_logs(level);

-- Security: Enable row-level security
ALTER TABLE system_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_identities ENABLE ROW LEVEL SECURITY;