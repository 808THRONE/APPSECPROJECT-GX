package com.securegate.iam.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.logging.Logger;

@ApplicationScoped
public class KeyManagementService {

    private static final Logger LOGGER = Logger.getLogger(KeyManagementService.class.getName());

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String keyId;

    @PostConstruct
    public void init() {
        try {
            // In a real prod environment, keys would be loaded from a KeyStore or Vault.
            // For this demo/POC, we generate on startup if not provided via Env.

            String envPrivate = System.getenv("JWT_PRIVATE_KEY");
            String envPublic = System.getenv("JWT_PUBLIC_KEY");
            String envKid = System.getenv("JWT_KID");

            if (envPrivate != null && envPublic != null) {
                // Load from env... (Simplified for now)
                LOGGER.info("Loading RSA Keys from environment variables");
                // TODO: Implement parsing if needed
            } else {
                LOGGER.warning("JWT RSA Keys not found in environment. Generating new ones for this session.");
                generateKeyPair();
            }

            this.keyId = (envKid != null) ? envKid : "sg-key-2026-01";

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize KeyManagementService", e);
        }
    }

    private void generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        this.privateKey = (RSAPrivateKey) kp.getPrivate();
        this.publicKey = (RSAPublicKey) kp.getPublic();
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
