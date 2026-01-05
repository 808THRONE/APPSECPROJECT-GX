-- IAM Service Database Schema
-- Compatible with PostgreSQL 16+ and H2 2.2+

-- ============================================
-- Tenants (OAuth Clients)
-- ============================================
CREATE TABLE IF NOT EXISTS tenants (
    id SMALLSERIAL PRIMARY KEY,
    tenant_id VARCHAR(191) UNIQUE NOT NULL,
    tenant_secret VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(255) NOT NULL,
    allowed_roles BIGINT NOT NULL DEFAULT 0,
    required_scopes VARCHAR(255) NOT NULL,
    supported_grant_types VARCHAR(255) NOT NULL DEFAULT 'authorization_code,refresh_token'
);

-- ============================================
-- Identities (Users)
-- ============================================
CREATE TABLE IF NOT EXISTS identities (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(191) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL, -- Argon2id hash
    roles BIGINT NOT NULL DEFAULT 0, -- Bitmask: 1=Surfer, 2=Moderator, 4=Administrator
    provided_scopes VARCHAR(255) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE
);

-- ============================================
-- Grants (User Consents)
-- ============================================
CREATE TABLE IF NOT EXISTS grants (
    tenant_id SMALLINT NOT NULL,
    identity_id BIGINT NOT NULL,
    approved_scopes VARCHAR(255) NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, identity_id),
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (identity_id) REFERENCES identities(id) ON DELETE CASCADE
);

-- ============================================
-- Indexes
-- ============================================
CREATE INDEX IF NOT EXISTS idx_tenants_tenant_id ON tenants(tenant_id);
CREATE INDEX IF NOT EXISTS idx_identities_username ON identities(username);

-- ============================================
-- Demo Data (Remove in production!)
-- ============================================

-- Demo tenant (client application)
INSERT INTO tenants (tenant_id, tenant_secret, redirect_uri, allowed_roles, required_scopes, supported_grant_types)
VALUES (
    'demo-client',
    'demo-secret-CHANGE-IN-PRODUCTION',
    'http://localhost:3000/callback',
    7, -- All roles: 111 binary
    'openid profile email',
    'authorization_code,refresh_token'
) ON CONFLICT (tenant_id) DO NOTHING;

-- Demo user
-- Username: admin@mortadha.me
-- Password: Admin123!
-- Generated with: Argon2Utility.hashFromString("Admin123!")
-- Note: Replace with actual hash from your application
INSERT INTO identities (username, password, roles, provided_scopes, enabled)
VALUES (
    'admin@mortadha.me',
    '$argon2id$v=19$m=97579,t=23,p=2$cOBcV7zZ8zBKQTlW8xtEVg$x8nHYEQz7LNZqN7QmQlRSFGlzB8qL6C9D8gC7L2rK8Y9qL8N7Z6K5M4B3A2C1D0', -- CHANGE THIS
    7, -- All roles
    'openid profile email admin',
    TRUE
) ON CONFLICT (username) DO NOTHING;

-- Demo grant (pre-approved consent)
INSERT INTO grants (tenant_id, identity_id, approved_scopes)
SELECT t.id, i.id, 'openid profile email'
FROM tenants t, identities i
WHERE t.tenant_id = 'demo-client' AND i.username = 'admin@mortadha.me'
ON CONFLICT (tenant_id, identity_id) DO NOTHING;

-- ============================================
-- Comments
-- ============================================
COMMENT ON TABLE tenants IS 'OAuth 2.1 clients/applications';
COMMENT ON TABLE identities IS 'User accounts with Argon2id passwords';
COMMENT ON TABLE grants IS 'User consent grants for OAuth clients';
COMMENT ON COLUMN identities.roles IS 'Bitmask: 1=Surfer, 2=Moderator, 4=Administrator';
