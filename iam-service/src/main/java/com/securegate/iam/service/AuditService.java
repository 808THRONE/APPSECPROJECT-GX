package com.securegate.iam.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class AuditService {

    // In-memory last hash (in prod, load from DB)
    private AtomicReference<String> lastHash = new AtomicReference<>("GENESIS_HASH_000000000000000000000");

    public void logEvent(String actor, String eventType, String details) {
        String previousHash = lastHash.get();
        String timestamp = Instant.now().toString();

        // entry = prevHash + timestamp + actor + type + details
        String rawEntry = previousHash + "|" + timestamp + "|" + actor + "|" + eventType + "|" + details;
        String currentHash = hash(rawEntry);

        // Verify Chain Integrity (Optimistic locking in prod)
        lastHash.set(currentHash);

        // Save to DB (Stub)
        System.out.println("[AUDIT] " + currentHash + " >> " + rawEntry);
    }

    private String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String getLastHash() {
        return lastHash.get();
    }
}
