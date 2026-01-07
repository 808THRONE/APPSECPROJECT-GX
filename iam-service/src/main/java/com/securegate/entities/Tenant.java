package com.securegate.entities;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "tenants")
public class Tenant {
    @Id
    @Column(name = "client_id")
    private String clientId;

    @Column(name = "client_secret")
    private String clientSecret;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tenant_redirect_uris", joinColumns = @JoinColumn(name = "tenant_id"))
    @Column(name = "uri")
    private Set<String> redirectUris;

    @Column(name = "name")
    private String name;

    public Tenant() {
    }

    public Tenant(String clientId, String clientSecret, Set<String> redirectUris, String name) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUris = redirectUris;
        this.name = name;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
