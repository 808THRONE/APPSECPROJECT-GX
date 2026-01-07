package com.securegate.api;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.ParseException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * JWKS Cache - Fetches and caches public keys from IAM service for JWT
 * verification
 */
@Startup
@Singleton
public class JwksCache {

    private static final Logger LOGGER = Logger.getLogger(JwksCache.class.getName());

    // Default to Docker internal URL, can be overridden via environment
    private String jwksUrl;

    private final Map<String, OctetKeyPair> publicKeys = new ConcurrentHashMap<>();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @PostConstruct
    public void init() {
        jwksUrl = System.getenv("IAM_JWKS_URL");
        if (jwksUrl == null || jwksUrl.isEmpty()) {
            jwksUrl = "http://iam-service:8080/iam/jwk";
        }
        LOGGER.info("JWKS Cache initialized with URL: " + jwksUrl);
        refreshKeys();
    }

    /**
     * Refresh JWKS every hour
     */
    @Schedule(hour = "*", minute = "0", persistent = false)
    @Lock(LockType.WRITE)
    public void refreshKeys() {
        try {
            LOGGER.info("Refreshing JWKS from: " + jwksUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jwksUrl))
                    .timeout(Duration.ofSeconds(30))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JWKSet jwkSet = JWKSet.parse(response.body());

                publicKeys.clear();
                for (JWK jwk : jwkSet.getKeys()) {
                    if (jwk instanceof OctetKeyPair okp) {
                        publicKeys.put(okp.getKeyID(), okp);
                        LOGGER.fine("Cached key: " + okp.getKeyID());
                    }
                }
                LOGGER.info("JWKS refreshed: " + publicKeys.size() + " keys cached");
            } else {
                LOGGER.warning("Failed to fetch JWKS: HTTP " + response.statusCode());
            }
        } catch (IOException | InterruptedException | ParseException e) {
            LOGGER.warning("Error refreshing JWKS: " + e.getMessage());
            // Keep existing keys if refresh fails
        }
    }

    /**
     * Get public key by key ID
     */
    @Lock(LockType.READ)
    public Optional<OctetKeyPair> getPublicKey(String keyId) {
        return Optional.ofNullable(publicKeys.get(keyId));
    }

    /**
     * Verify JWT signature using cached public keys
     */
    @Lock(LockType.READ)
    public boolean verifySignature(SignedJWT signedJWT) {
        String keyId = signedJWT.getHeader().getKeyID();

        OctetKeyPair publicKey = publicKeys.get(keyId);
        if (publicKey == null) {
            LOGGER.warning("Key ID not found in cache: " + keyId);
            // Try refreshing keys once
            refreshKeys();
            publicKey = publicKeys.get(keyId);
            if (publicKey == null) {
                return false;
            }
        }

        try {
            JWSVerifier verifier = new Ed25519Verifier(publicKey);
            return signedJWT.verify(verifier);
        } catch (JOSEException e) {
            LOGGER.warning("Signature verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Check if cache has any keys (for health checks)
     */
    @Lock(LockType.READ)
    public boolean hasKeys() {
        return !publicKeys.isEmpty();
    }
}
