package me.mortadha.iam.entities;

import jakarta.persistence.*;
import me.mortadha.iam.core.BaseEntity;

/**
 * OAuth 2.1 Client (Tenant/Application)
 */
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity<Short> {

    @Column(name = "tenant_id", nullable = false, unique = true, length = 191)
    private String tenantId;

    @Column(name = "tenant_secret", nullable = false)
    private String secret;

    @Column(name = "redirect_uri", nullable = false)
    private String redirectUri;

    @Column(name = "allowed_roles", nullable = false)
    private Long allowedRoles = 0L;

    @Column(name = "required_scopes", nullable = false)
    private String requiredScopes;

    @Column(name = "supported_grant_types", nullable = false)
    private String supportedGrantTypes = "authorization_code,refresh_token";

    // Constructors
    public Tenant() {}

    public Tenant(String tenantId, String secret, String redirectUri, String requiredScopes) {
        this.tenantId = tenantId;
        this.secret = secret;
        this.redirectUri = redirectUri;
        this.requiredScopes = requiredScopes;
    }

    // Getters and Setters
    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public Long getAllowedRoles() {
        return allowedRoles;
    }

    public void setAllowedRoles(Long allowedRoles) {
        this.allowedRoles = allowedRoles;
    }

    public String getRequiredScopes() {
        return requiredScopes;
    }

    public void setRequiredScopes(String requiredScopes) {
        this.requiredScopes = requiredScopes;
    }

    public String getSupportedGrantTypes() {
        return supportedGrantTypes;
    }

    public void setSupportedGrantTypes(String supportedGrantTypes) {
        this.supportedGrantTypes = supportedGrantTypes;
    }

    @Override
    public String toString() {
        return "Tenant{tenantId='" + tenantId + "', id=" + getId() + "}";
    }
}
