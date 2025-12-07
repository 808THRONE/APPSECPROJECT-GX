-- PostgreSQL Initialization Script
-- SecureGate IAM Portal Database Setup

-- Enable pgcrypto extension
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create users table
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255),
    department VARCHAR(100),
    role VARCHAR(50),
    clearance_level INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create ABAC policies table (JSONB for flexibility)
CREATE TABLE IF NOT EXISTS abac_policies (
    policy_id VARCHAR(50) PRIMARY KEY,
    effect VARCHAR(10) NOT NULL CHECK (effect IN ('permit', 'deny')),
    subject JSONB NOT NULL,
    resource JSONB NOT NULL,
    environment JSONB,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Create audit logs table
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(50) NOT NULL,
    user_id UUID REFERENCES users(id),
    user_email VARCHAR(255),
    ip_address INET,
    device_fingerprint VARCHAR(255),
    resource VARCHAR(255),
    action VARCHAR(100),
    result VARCHAR(20),
    metadata JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create 2FA secrets table
CREATE TABLE IF NOT EXISTS totp_secrets (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    secret_encrypted TEXT NOT NULL,
    backup_codes JSONB,
    enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Create indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_policies_effect ON abac_policies(effect);

-- Row-Level Security (RLS) setup
-- TODO: Configure RLS policies for multi-tenancy

-- Insert demo user (password: 'SecurePassword123!')
-- Bcrypt hash with cost factor 12
INSERT INTO users (email, name, department, role, clearance_level, password_hash)
VALUES (
    'admin@securegate.com',
    'Administrator',
    'Security Operations',
    'Security Administrator',
    5,
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY8MCEjPcNiWqGy'
) ON CONFLICT (email) DO NOTHING;

-- Insert demo ABAC policy
INSERT INTO abac_policies (policy_id, effect, subject, resource, environment)
VALUES (
    'p001',
    'permit',
    '{"department": ["engineering", "security"], "clearance_level": {"min": 3}}'::jsonb,
    '{"type": "audit_logs", "sensitivity": "high"}'::jsonb,
    '{"time": {"start": "09:00", "end": "18:00"}, "location": {"countries": ["US", "CA"]}}'::jsonb
) ON CONFLICT (policy_id) DO NOTHING;

COMMENT ON TABLE users IS 'User accounts with bcrypt password hashing';
COMMENT ON TABLE abac_policies IS 'Attribute-Based Access Control policies stored as JSONB';
COMMENT ON TABLE audit_logs IS 'Security event audit trail';
COMMENT ON TABLE totp_secrets IS 'TOTP 2FA secrets (encrypted)';
