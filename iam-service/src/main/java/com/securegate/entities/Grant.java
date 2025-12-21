package com.securegate.entities;

import java.time.Instant;

public class Grant {
    private String code;
    private String identityId;
    private String clientId;
    private Instant expiresAt;
    private String redirectUri;
    private String nonce;
    private String codeChallenge;
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
