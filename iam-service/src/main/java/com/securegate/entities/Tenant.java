package com.securegate.entities;

import java.util.Set;

public class Tenant {
    private String clientId;
    private String clientSecret;
    private Set<String> redirectUris;
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
