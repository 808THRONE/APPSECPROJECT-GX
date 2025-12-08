package xyz.kaaniche.phoenix.iam.entities;

import jakarta.persistence.*;
import xyz.kaaniche.phoenix.core.entities.CompoundPKEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "issued_grants")
public class Grant extends CompoundPKEntity<GrantPK> {
    @MapsId("tenantId")
    @ManyToOne
    private Tenant tenant;
    @MapsId("identityId")
    @ManyToOne
    private Identity identity;

    @Column(name = "approved_scopes")
    private String approvedScopes;

    @Column(name = "issuance_date_time")
    private LocalDateTime issuanceDateTime;

    public Tenant getTenant() {
        return tenant;
    }

    public void setTenant(Tenant tenant) {
        this.tenant = tenant;
    }

    public Identity getIdentity() {
        return identity;
    }

    public void setIdentity(Identity identity) {
        this.identity = identity;
    }

    public String getApprovedScopes() {
        return approvedScopes;
    }

    public void setApprovedScopes(String approvedScopes) {
        this.approvedScopes = approvedScopes;
    }

    public LocalDateTime getIssuanceDateTime() {
        return issuanceDateTime;
    }

    public void setIssuanceDateTime(LocalDateTime issuanceDateTime) {
        this.issuanceDateTime = issuanceDateTime;
    }
}
