package com.securegate.api.config;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Centralized configuration for API Gateway.
 * All service URLs and secrets loaded from system properties or environment
 * variables.
 */
@ApplicationScoped
public class ApiGatewayConfig {

    // Service URLs
    private final String iamServiceUrl;
    private final String stegoServiceUrl;

    // Security Configuration
    private final String jwtSecretKey;
    private final String jwtIssuer;
    private final String jwtAudience;
    private final int jwtExpirationSeconds;

    // Stego Configuration
    private final boolean stegoEnabled;
    private final String stegoEncryptionKey;

    public ApiGatewayConfig() {
        this.iamServiceUrl = getProperty("iam.service.url", "http://iam-service:8080/iam-service/api");
        this.stegoServiceUrl = getProperty("stego.service.url", "http://stego-module:8080/stego-module/api");
        this.jwtSecretKey = getProperty("jwt.secret.key",
                "SecureGate_Production_Secret_Key_Must_Be_Changed_In_Prod_256bit!");
        this.jwtIssuer = getProperty("jwt.issuer", "https://iam.securegate.io");
        this.jwtAudience = getProperty("jwt.audience", "securegate-api");
        this.jwtExpirationSeconds = Integer.parseInt(getProperty("jwt.expiration.seconds", "900"));
        this.stegoEnabled = Boolean.parseBoolean(getProperty("stego.enabled", "true"));
        this.stegoEncryptionKey = getProperty("stego.encryption.key", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=");
    }

    private String getProperty(String key, String defaultValue) {
        // Try system property first, then environment variable
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        // Convert to env var format: jwt.secret.key -> JWT_SECRET_KEY
        String envKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        return defaultValue;
    }

    // Getters
    public String getIamServiceUrl() {
        return iamServiceUrl;
    }

    public String getStegoServiceUrl() {
        return stegoServiceUrl;
    }

    public String getJwtSecretKey() {
        return jwtSecretKey;
    }

    public byte[] getJwtSecretKeyBytes() {
        return jwtSecretKey.getBytes();
    }

    public String getJwtIssuer() {
        return jwtIssuer;
    }

    public String getJwtAudience() {
        return jwtAudience;
    }

    public int getJwtExpirationSeconds() {
        return jwtExpirationSeconds;
    }

    public boolean isStegoEnabled() {
        return stegoEnabled;
    }

    public String getStegoEncryptionKey() {
        return stegoEncryptionKey;
    }
}
