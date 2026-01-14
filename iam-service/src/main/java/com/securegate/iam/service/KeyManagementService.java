package com.securegate.iam.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import java.security.*;
import java.util.logging.Logger;

@ApplicationScoped
public class KeyManagementService {

    private static final Logger LOGGER = Logger.getLogger(KeyManagementService.class.getName());

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String keyId;

    @PostConstruct
    public void init() {
        try {
            // Ed25519 is supported natively in Java 15+
            String envPrivate = System.getenv("JWT_PRIVATE_KEY");
            String envPublic = System.getenv("JWT_PUBLIC_KEY");
            String envKid = System.getenv("JWT_KID");

            if (envPrivate != null && envPublic != null) {
                LOGGER.info("Loading Ed25519 Keys from environment variables");
                // TODO: Implement parsing for Ed25519 if needed
            } else {
                LOGGER.warning("JWT Ed25519 Keys not found in environment. Generating new ones for this session.");
                generateKeyPair();
            }

            this.keyId = (envKid != null) ? envKid : "sg-eddsa-key-2026-01";

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to initialize KeyManagementService", e);
        }
    }

    private void generateKeyPair() throws NoSuchAlgorithmException {
        // Use Ed25519 (EdDSA)
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("Ed25519");
        KeyPair kp = kpg.generateKeyPair();
        this.privateKey = kp.getPrivate();
        this.publicKey = kp.getPublic();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
