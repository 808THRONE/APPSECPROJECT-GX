package xyz.kaaniche.phoenix.iam.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import xyz.kaaniche.phoenix.core.entities.SimplePKEntity;

@Entity
@Table(name = "tenants")
public class Tenant extends SimplePKEntity<Short> {
    @Column(name = "tenant_id",nullable = false,unique = true,length = 191)
    private String name;
    @Column(name = "tenant_secret", nullable = false)
    private String secret;
    @Column(name = "redirect_uri",nullable = false)
    private String redirectUri;

    @Column(name = "allowed_roles",nullable = false)
    private Long allowedRoles;

    @Column(name = "required_scopes",nullable = false)
    private String requiredScopes;

    @Column(name = "supported_grant_types",nullable = false)
    private String supportedGrantTypes;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}
