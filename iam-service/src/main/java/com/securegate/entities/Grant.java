package com.securegate.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "grants")
public class Grant {
    @Id
    @Column(name = "code")
    private String code;

    @Column(name = "identity_id")
    private String identityId;

    @Column(name = "client_id")
    private String clientId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "redirect_uri")
    private String redirectUri;

    @Column(name = "nonce")
    private String nonce;

    @Column(name = "code_challenge")
    private String codeChallenge;

    @Column(name = "code_challenge_method")
    private String codeChallengeMethod;

    public Grant() {
    }

    public Grant(String code, String identityId, String clientId, Instant expiresAt, String redirectUri, String nonce,
            String codeChallenge, String codeChallengeMethod) {
        this.code = code;
        this.identityId = identityId;
        this.clientId = clientId;
        this.expiresAt = expiresAt;
        this.redirectUri = redirectUri;
        this.nonce = nonce;
        this.codeChallenge = codeChallenge;
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getIdentityId() {
        return identityId;
    }

    public void setIdentityId(String identityId) {
        this.identityId = identityId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }
}
