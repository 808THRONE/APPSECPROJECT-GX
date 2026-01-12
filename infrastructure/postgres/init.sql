-- SecureGate IAM Portal Database Initialization
-- Production-Ready Schema with Realistic Mock Data

-- Enable pgcrypto for UUID generation and hashing if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1. Identity & Accounts Schema
CREATE SCHEMA IF NOT EXISTS iam_identity;

CREATE TABLE iam_identity.users (
    user_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL, -- Argon2id hash
    full_name VARCHAR(255),
    status VARCHAR(50) DEFAULT 'ACTIVE', -- ACTIVE, SUSPENDED, TERMINATED
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_secret_enc TEXT, -- Encrypted TOTP secret
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP WITH TIME ZONE,
    attributes JSONB DEFAULT '{}' -- Store ABAC attributes (dept, clearance, etc.)
);

CREATE TABLE iam_identity.roles (
    role_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_name VARCHAR(50) UNIQUE NOT NULL,
    description TEXT,
    permissions JSONB DEFAULT '[]' -- List of permissions/scopes
);

CREATE TABLE iam_identity.user_roles (
    user_id UUID REFERENCES iam_identity.users(user_id) ON DELETE CASCADE,
    role_id UUID REFERENCES iam_identity.roles(role_id) ON DELETE CASCADE,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, role_id)
);

-- 2. Tokens & Sessions Schema
CREATE SCHEMA IF NOT EXISTS iam_tokens;

CREATE TABLE iam_tokens.refresh_tokens (
    token_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES iam_identity.users(user_id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL, -- Store hash of the opaque token
    device_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    replaced_by_token_id UUID -- For rotation chains
);

CREATE TABLE iam_tokens.revoked_tokens (
    jti VARCHAR(255) PRIMARY KEY, -- JWT ID
    revocation_reason VARCHAR(255),
    revoked_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE -- When we can prune this record
);

-- 3. Policy & Authorization Schema
CREATE SCHEMA IF NOT EXISTS iam_policy;

CREATE TABLE iam_policy.policies (
    policy_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    effect VARCHAR(10) CHECK (effect IN ('PERMIT', 'DENY')),
    target JSONB NOT NULL, -- Resource and Action matching
    condition JSONB, -- Logic for ABAC (e.g., recursive rules)
    priority INT DEFAULT 0,
    version INT DEFAULT 1,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    created_by UUID -- Audit ref
);

-- 4. Audit & Compliance Schema
CREATE SCHEMA IF NOT EXISTS iam_audit;

CREATE TABLE iam_audit.audit_logs (
    event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    event_type VARCHAR(100) NOT NULL, -- LOGIN_SUCCESS, LOGIN_FAIL, POLICY_CHANGE, etc.
    actor_id UUID, -- NULL if system or unauthenticated
    actor_ip VARCHAR(45),
    action VARCHAR(255),
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    outcome VARCHAR(50), -- SUCCESS, FAILURE, DENIED
    details JSONB, -- Detailed context
    prev_hash VARCHAR(255), -- Hash chaining
    curr_hash VARCHAR(255) -- Hash of this record including prev_hash
);

CREATE INDEX idx_audit_timestamp ON iam_audit.audit_logs(timestamp);
CREATE INDEX idx_audit_actor ON iam_audit.audit_logs(actor_id);

-- 5. Steganography & Watermarking Schema
CREATE SCHEMA IF NOT EXISTS iam_stego;

CREATE TABLE iam_stego.stc_blobs (
    blob_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    blob_type VARCHAR(50), -- CHECKPOINT, NOISE, IMAGE
    content BYTEA, -- The carrier data
    embedded_meta JSONB -- Metadata about what's hidden (for admin reference)
);

-- 6. Notifications Schema
CREATE SCHEMA IF NOT EXISTS iam_notifications;

CREATE TABLE iam_notifications.notifications (
    notification_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'INFO', -- INFO, WARNING, DANGER, SUCCESS
    category VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read BOOLEAN DEFAULT FALSE,
    action_url VARCHAR(512),
    expires_at TIMESTAMP WITH TIME ZONE
);

-- 7. System Settings Schema
CREATE SCHEMA IF NOT EXISTS iam_settings;

CREATE TABLE iam_settings.system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT,
    category VARCHAR(50),
    data_type VARCHAR(20) DEFAULT 'string',
    is_editable BOOLEAN DEFAULT TRUE,
    last_modified_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_modified_by UUID
);

-- =============================================================================
-- SEED DATA: Roles
-- =============================================================================

INSERT INTO iam_identity.roles (role_name, description, permissions) VALUES
('ROLE_ADMIN', 'System Administrator with full access', 
 '["users:read", "users:write", "users:delete", "policies:read", "policies:write", "policies:delete", "audit:read", "settings:read", "settings:write"]'),
('ROLE_USER', 'Standard authenticated user', 
 '["profile:read", "profile:write"]'),
('ROLE_SECURITY_ADMIN', 'Security Administrator for policy management', 
 '["policies:read", "policies:write", "audit:read", "settings:read"]'),
('ROLE_AUDITOR', 'Compliance Auditor with read-only access', 
 '["users:read", "policies:read", "audit:read"]'),
('ROLE_MANAGER', 'Department Manager with team oversight', 
 '["users:read", "policies:read", "audit:read"]'),
('ROLE_API_CLIENT', 'API integration service account', 
 '["api:access"]');

-- =============================================================================
-- SEED DATA: Users (Production-like data)
-- Password hash for all: "SecureP@ss123!" (Argon2id - BouncyCastle compatible)
-- Hash: $argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=
-- =============================================================================

INSERT INTO iam_identity.users (username, email, password_hash, full_name, status, mfa_enabled, last_login_at, attributes) VALUES
-- Executive Team
('jsmith', 'john.smith@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'John Smith', 'ACTIVE', true, NOW() - INTERVAL '2 hours', '{"department": "Executive", "title": "CEO", "clearance": "TOP_SECRET"}'),
('sjohnson', 'sarah.johnson@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Sarah Johnson', 'ACTIVE', true, NOW() - INTERVAL '5 hours', '{"department": "Executive", "title": "CTO", "clearance": "TOP_SECRET"}'),
('mwilliams', 'michael.williams@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Michael Williams', 'ACTIVE', true, NOW() - INTERVAL '1 day', '{"department": "Executive", "title": "CFO", "clearance": "SECRET"}'),

-- IT Security Team
('admin', 'admin@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'System Administrator', 'ACTIVE', true, NOW() - INTERVAL '30 minutes', '{"department": "IT Security", "title": "Senior Security Engineer", "clearance": "TOP_SECRET"}'),
('ebrown', 'emily.brown@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Emily Brown', 'ACTIVE', true, NOW() - INTERVAL '3 hours', '{"department": "IT Security", "title": "Security Analyst", "clearance": "SECRET"}'),
('dlee', 'david.lee@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'David Lee', 'ACTIVE', false, NOW() - INTERVAL '6 hours', '{"department": "IT Security", "title": "SOC Analyst", "clearance": "SECRET"}'),
('jgarcia', 'jennifer.garcia@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Jennifer Garcia', 'ACTIVE', true, NOW() - INTERVAL '1 day', '{"department": "IT Security", "title": "Security Engineer", "clearance": "SECRET"}'),

-- Engineering Team
('rmiller', 'robert.miller@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Robert Miller', 'ACTIVE', false, NOW() - INTERVAL '4 hours', '{"department": "Engineering", "title": "Senior Developer", "clearance": "CONFIDENTIAL"}'),
('ldavis', 'linda.davis@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Linda Davis', 'ACTIVE', false, NOW() - INTERVAL '8 hours', '{"department": "Engineering", "title": "DevOps Engineer", "clearance": "CONFIDENTIAL"}'),
('jmartinez', 'james.martinez@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'James Martinez', 'ACTIVE', true, NOW() - INTERVAL '2 days', '{"department": "Engineering", "title": "Platform Architect", "clearance": "SECRET"}'),

-- Finance Team
('pthompson', 'patricia.thompson@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Patricia Thompson', 'ACTIVE', true, NOW() - INTERVAL '1 day', '{"department": "Finance", "title": "Finance Manager", "clearance": "CONFIDENTIAL"}'),
('chanderson', 'chris.anderson@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Chris Anderson', 'ACTIVE', false, NOW() - INTERVAL '3 days', '{"department": "Finance", "title": "Accountant", "clearance": "CONFIDENTIAL"}'),

-- Human Resources
('ntaylor', 'nancy.taylor@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Nancy Taylor', 'ACTIVE', true, NOW() - INTERVAL '12 hours', '{"department": "Human Resources", "title": "HR Director", "clearance": "CONFIDENTIAL"}'),
('kmoore', 'kevin.moore@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Kevin Moore', 'ACTIVE', false, NOW() - INTERVAL '2 days', '{"department": "Human Resources", "title": "Recruiter", "clearance": "UNCLASSIFIED"}'),

-- Suspended/Inactive accounts
('bjackson', 'brian.jackson@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Brian Jackson', 'SUSPENDED', false, NOW() - INTERVAL '30 days', '{"department": "Sales", "title": "Sales Rep", "clearance": "UNCLASSIFIED", "suspension_reason": "Policy violation"}'),
('awhite', 'amanda.white@securegate.io', '$argon2id$v=19$m=65536,t=3,p=1$XvsvQ3iJNH51YXcsLxckQA==$rY651rjxedKYTLt7GDXouhzPGUSrCkwkf6wAYUiOa+M=', 'Amanda White', 'TERMINATED', false, NOW() - INTERVAL '90 days', '{"department": "Marketing", "title": "Former Employee", "clearance": "UNCLASSIFIED"}');

-- =============================================================================
-- SEED DATA: Policies (RBAC/ABAC rules)
-- =============================================================================

INSERT INTO iam_policy.policies (name, description, effect, target, condition, priority, is_active) VALUES
('AdminFullAccess', 'Administrators have full access to all resources', 'PERMIT', 
 '{"resource": "*", "action": "*"}', 
 '{"role": "ROLE_ADMIN"}', 100, true),

('UserProfileAccess', 'Users can read and update their own profile', 'PERMIT',
 '{"resource": "profile", "action": ["read", "update"]}',
 '{"operator": "equals", "field": "subject.id", "value": "resource.owner_id"}', 50, true),

('AuditReadOnly', 'Auditors can read all audit logs', 'PERMIT',
 '{"resource": "audit_logs", "action": "read"}',
 '{"role": "ROLE_AUDITOR"}', 80, true),

('DenyExternalAPI', 'Deny API access from external IPs after hours', 'DENY',
 '{"resource": "api/*", "action": "*"}',
 '{"operator": "and", "conditions": [{"operator": "not_in", "field": "request.ip", "value": "10.0.0.0/8"}, {"operator": "between", "field": "request.time", "value": ["22:00", "06:00"]}]}', 200, true),

('FinanceDataAccess', 'Finance team can access financial resources', 'PERMIT',
 '{"resource": "finance/*", "action": ["read", "write"]}',
 '{"operator": "equals", "field": "subject.department", "value": "Finance"}', 70, true),

('TopSecretClearance', 'Only TOP_SECRET clearance can access classified data', 'PERMIT',
 '{"resource": "classified/*", "action": "*"}',
 '{"operator": "equals", "field": "subject.clearance", "value": "TOP_SECRET"}', 150, true),

('MfaRequired', 'MFA required for sensitive operations', 'DENY',
 '{"resource": ["users", "policies", "settings"], "action": ["write", "delete"]}',
 '{"operator": "equals", "field": "subject.mfa_verified", "value": false}', 250, true),

('RateLimitAPI', 'Rate limit API requests to 100/minute', 'DENY',
 '{"resource": "api/*", "action": "*"}',
 '{"operator": "greater_than", "field": "request.rate_count", "value": 100}', 300, true),

('BusinessHoursOnly', 'Restrict access to business hours for non-admins', 'DENY',
 '{"resource": "*", "action": "*"}',
 '{"operator": "and", "conditions": [{"operator": "not_in", "field": "subject.role", "value": ["ROLE_ADMIN", "ROLE_SECURITY_ADMIN"]}, {"operator": "not_between", "field": "request.time", "value": ["08:00", "18:00"]}]}', 50, false),

('GeoRestriction', 'Deny access from high-risk countries', 'DENY',
 '{"resource": "*", "action": "*"}',
 '{"operator": "in", "field": "request.geo_country", "value": ["RU", "CN", "KP", "IR"]}', 400, true);

-- =============================================================================
-- SEED DATA: System Settings
-- =============================================================================

INSERT INTO iam_settings.system_settings (setting_key, setting_value, description, category, data_type, is_editable) VALUES
('auth_session_timeout', '900', 'Access token expiration in seconds (15 minutes)', 'Authentication', 'number', true),
('auth_refresh_token_lifetime', '604800', 'Refresh token lifetime in seconds (7 days)', 'Authentication', 'number', true),
('mfa_enabled', 'true', 'Enable Multi-Factor Authentication globally', 'Security', 'boolean', true),
('mfa_grace_period', '86400', 'Grace period for MFA setup in seconds (24 hours)', 'Security', 'number', true),
('password_min_length', '12', 'Minimum password length requirement', 'Security', 'number', true),
('password_require_special', 'true', 'Require special characters in passwords', 'Security', 'boolean', true),
('password_require_uppercase', 'true', 'Require uppercase letters in passwords', 'Security', 'boolean', true),
('password_require_number', 'true', 'Require numbers in passwords', 'Security', 'boolean', true),
('password_expiry_days', '90', 'Password expiration in days', 'Security', 'number', true),
('account_lockout_attempts', '5', 'Failed login attempts before lockout', 'Security', 'number', true),
('account_lockout_duration', '1800', 'Account lockout duration in seconds (30 min)', 'Security', 'number', true),
('stego_enabled', 'true', 'Enable steganographic transmission', 'Advanced', 'boolean', true),
('audit_retention_days', '365', 'Audit log retention period in days', 'Compliance', 'number', false),
('api_rate_limit', '100', 'API requests per minute per user', 'API', 'number', true),
('cors_allowed_origins', 'https://securegate.io,https://admin.securegate.io', 'Allowed CORS origins', 'API', 'string', true);

-- =============================================================================
-- SEED DATA: Notifications
-- =============================================================================

INSERT INTO iam_notifications.notifications (title, message, type, category, read, created_at) VALUES
('System Deployed', 'SecureGate IAM Portal v1.0.0 has been successfully deployed to production.', 'SUCCESS', 'System', true, NOW() - INTERVAL '7 days'),
('Security Patch Applied', 'Critical security patch CVE-2026-0001 has been applied to all services.', 'INFO', 'Security', true, NOW() - INTERVAL '5 days'),
('New Policy Created', 'A new access policy "GeoRestriction" has been activated by admin@securegate.io.', 'INFO', 'Policy', true, NOW() - INTERVAL '3 days'),
('Suspicious Activity Detected', 'Multiple failed login attempts detected from IP 185.234.72.45. Account bjackson has been suspended.', 'WARNING', 'Security', true, NOW() - INTERVAL '2 days'),
('Database Maintenance Scheduled', 'Scheduled database maintenance window: January 15, 2026 02:00-04:00 UTC.', 'INFO', 'System', false, NOW() - INTERVAL '1 day'),
('MFA Adoption Report', 'MFA adoption rate increased to 75%. 4 users still pending MFA setup.', 'INFO', 'Compliance', false, NOW() - INTERVAL '12 hours'),
('Certificate Expiring Soon', 'TLS certificate for api.securegate.io expires in 30 days. Renewal required.', 'WARNING', 'Security', false, NOW() - INTERVAL '6 hours'),
('New User Onboarded', 'User jgarcia (Jennifer Garcia) has completed onboarding and activated MFA.', 'SUCCESS', 'Users', false, NOW() - INTERVAL '2 hours');

-- =============================================================================
-- SEED DATA: Audit Logs (Recent activity)
-- =============================================================================

INSERT INTO iam_audit.audit_logs (timestamp, event_type, actor_ip, action, resource_type, resource_id, outcome, details) VALUES
(NOW() - INTERVAL '30 minutes', 'LOGIN_SUCCESS', '10.0.1.50', 'authenticate', 'session', 'sess_abc123', 'SUCCESS', '{"username": "admin", "mfa_used": true, "device": "Chrome/Windows"}'),
(NOW() - INTERVAL '45 minutes', 'POLICY_UPDATED', '10.0.1.50', 'update', 'policy', 'GeoRestriction', 'SUCCESS', '{"modified_by": "admin", "changes": {"is_active": [false, true]}}'),
(NOW() - INTERVAL '1 hour', 'USER_CREATED', '10.0.1.50', 'create', 'user', 'jgarcia', 'SUCCESS', '{"created_by": "admin", "department": "IT Security"}'),
(NOW() - INTERVAL '2 hours', 'LOGIN_SUCCESS', '10.0.2.100', 'authenticate', 'session', 'sess_def456', 'SUCCESS', '{"username": "jsmith", "mfa_used": true, "device": "Safari/macOS"}'),
(NOW() - INTERVAL '3 hours', 'SETTING_CHANGED', '10.0.1.50', 'update', 'setting', 'password_min_length', 'SUCCESS', '{"modified_by": "admin", "old_value": "8", "new_value": "12"}'),
(NOW() - INTERVAL '4 hours', 'LOGIN_FAILED', '185.234.72.45', 'authenticate', 'session', NULL, 'FAILURE', '{"username": "bjackson", "reason": "invalid_password", "attempt": 3}'),
(NOW() - INTERVAL '4 hours 5 minutes', 'LOGIN_FAILED', '185.234.72.45', 'authenticate', 'session', NULL, 'FAILURE', '{"username": "bjackson", "reason": "invalid_password", "attempt": 4}'),
(NOW() - INTERVAL '4 hours 10 minutes', 'LOGIN_FAILED', '185.234.72.45', 'authenticate', 'session', NULL, 'FAILURE', '{"username": "bjackson", "reason": "invalid_password", "attempt": 5}'),
(NOW() - INTERVAL '4 hours 11 minutes', 'ACCOUNT_LOCKED', '185.234.72.45', 'security_action', 'user', 'bjackson', 'SUCCESS', '{"reason": "max_attempts_exceeded", "lock_duration": 1800}'),
(NOW() - INTERVAL '5 hours', 'MFA_ENABLED', '10.0.2.50', 'update', 'user', 'ebrown', 'SUCCESS', '{"enabled_by": "self", "method": "TOTP"}'),
(NOW() - INTERVAL '6 hours', 'LOGIN_SUCCESS', '10.0.2.50', 'authenticate', 'session', 'sess_ghi789', 'SUCCESS', '{"username": "ebrown", "mfa_used": true, "device": "Firefox/Linux"}'),
(NOW() - INTERVAL '8 hours', 'ROLE_ASSIGNED', '10.0.1.50', 'update', 'user_role', 'jmartinez', 'SUCCESS', '{"assigned_role": "ROLE_SECURITY_ADMIN", "assigned_by": "admin"}'),
(NOW() - INTERVAL '12 hours', 'PASSWORD_CHANGED', '10.0.3.100', 'update', 'user', 'ntaylor', 'SUCCESS', '{"changed_by": "self", "password_age_days": 45}'),
(NOW() - INTERVAL '1 day', 'API_ACCESS', '10.0.5.10', 'read', 'api', '/v1/users', 'SUCCESS', '{"client_id": "integration_service", "response_time_ms": 45}'),
(NOW() - INTERVAL '1 day 2 hours', 'LOGIN_SUCCESS', '10.0.1.75', 'authenticate', 'session', 'sess_jkl012', 'SUCCESS', '{"username": "sjohnson", "mfa_used": true, "device": "Chrome/macOS"}'),
(NOW() - INTERVAL '2 days', 'POLICY_CREATED', '10.0.1.50', 'create', 'policy', 'BusinessHoursOnly', 'SUCCESS', '{"created_by": "admin", "effect": "DENY"}'),
(NOW() - INTERVAL '2 days 4 hours', 'USER_SUSPENDED', '10.0.1.50', 'update', 'user', 'bjackson', 'SUCCESS', '{"suspended_by": "admin", "reason": "Policy violation - unauthorized data access attempt"}'),
(NOW() - INTERVAL '3 days', 'EXPORT_AUDIT_LOGS', '10.0.1.50', 'export', 'audit_logs', NULL, 'SUCCESS', '{"exported_by": "admin", "date_range": "last_30_days", "format": "CSV"}'),
(NOW() - INTERVAL '5 days', 'SECURITY_PATCH', 'system', 'system_maintenance', 'system', NULL, 'SUCCESS', '{"patch_id": "CVE-2026-0001", "applied_by": "automated"}'),
(NOW() - INTERVAL '7 days', 'SYSTEM_STARTUP', 'system', 'system_event', 'system', NULL, 'SUCCESS', '{"version": "1.0.0", "environment": "production", "deployment_id": "deploy_202601"}');
