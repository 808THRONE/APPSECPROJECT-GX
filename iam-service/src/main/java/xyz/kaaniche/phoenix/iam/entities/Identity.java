package xyz.kaaniche.phoenix.iam.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import xyz.kaaniche.phoenix.core.entities.SimplePKEntity;

import java.security.Principal;

@Entity
@Table(name = "identities")
public class Identity extends SimplePKEntity<Long> implements Principal {
    @Column(length = 191, unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column(nullable = false)
    private Long roles;
    @Column(name = "provided_scopes", nullable = false)
    private String providedScopes;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getName() {
        return username;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Identity))
            return false;
        if (!super.equals(o))
            return false;
        Identity identity = (Identity) o;
        return username != null && username.equals(identity.username);
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Identity{username='" + username + "', id=" + getId() + "}";
    }
}
