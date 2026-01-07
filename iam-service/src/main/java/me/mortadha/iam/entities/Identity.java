package me.mortadha.iam.entities;

import jakarta.persistence.*;
import me.mortadha.iam.core.BaseEntity;

import java.security.Principal;

/**
 * User Identity with Argon2id password
 */
@Entity
@Table(name = "identities")
public class Identity extends BaseEntity<Long> implements Principal {

    @Column(nullable = false, unique = true, length = 191)
    private String username;

    @Column(nullable = false)
    private String password; // Argon2id hash

    @Column(nullable = false)
    private Long roles = 0L; // Bitmask: 1=Surfer, 2=Moderator, 4=Administrator

    @Column(name = "provided_scopes", nullable = false)
    private String providedScopes;

    @Column(name = "enabled")
    private Boolean enabled = true;

    // Constructors
    public Identity() {}

    public Identity(String username, String password, Long roles, String providedScopes) {
        this.username = username;
        this.password = password;
        this.roles = roles;
        this.providedScopes = providedScopes;
    }

    // Principal implementation
    @Override
    public String getName() {
        return username;
    }

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getRoles() {
        return roles;
    }

    public void setRoles(Long roles) {
        this.roles = roles;
    }

    public String getProvidedScopes() {
        return providedScopes;
    }

    public void setProvidedScopes(String providedScopes) {
        this.providedScopes = providedScopes;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "Identity{username='" + username + "', id=" + getId() + "}";
    }
}
