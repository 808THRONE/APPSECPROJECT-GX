package me.mortadha.iam.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * User consent grant for a tenant
 */
@Entity
@Table(name = "grants")
public class Grant implements Serializable {

    @EmbeddedId
    private GrantId id;

    @Column(name = "approved_scopes", nullable = false)
    private String approvedScopes;

    @Column(name = "granted_at")
    private Instant grantedAt = Instant.now();

    // Constructors
    public Grant() {}

    public Grant(Short tenantId, Long identityId, String approvedScopes) {
        this.id = new GrantId(tenantId, identityId);
        this.approvedScopes = approvedScopes;
    }

    // Getters and Setters
    public GrantId getId() {
        return id;
    }

    public void setId(GrantId id) {
        this.id = id;
    }

    public String getApprovedScopes() {
        return approvedScopes;
    }

    public void setApprovedScopes(String approvedScopes) {
        this.approvedScopes = approvedScopes;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public void setGrantedAt(Instant grantedAt) {
        this.grantedAt = grantedAt;
    }

    /**
     * Composite Primary Key
     */
    @Embeddable
    public static class GrantId implements Serializable {
        
        @Column(name = "tenant_id")
        private Short tenantId;

        @Column(name = "identity_id")
        private Long identityId;

        public GrantId() {}

        public GrantId(Short tenantId, Long identityId) {
            this.tenantId = tenantId;
            this.identityId = identityId;
        }

        public Short getTenantId() {
            return tenantId;
        }

        public void setTenantId(Short tenantId) {
            this.tenantId = tenantId;
        }

        public Long getIdentityId() {
            return identityId;
        }

        public void setIdentityId(Long identityId) {
            this.identityId = identityId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GrantId grantId = (GrantId) o;
            return Objects.equals(tenantId, grantId.tenantId) &&
                   Objects.equals(identityId, grantId.identityId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tenantId, identityId);
        }
    }
}
