package com.securegate.iam.service;

import jakarta.enterprise.context.ApplicationScoped;
import com.securegate.iam.model.AuditLog;
import com.securegate.iam.repository.AuditLogRepository;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;

@ApplicationScoped
public class AuditService {

    @Inject
    private AuditLogRepository auditLogRepository;

    // In-memory last hash (in prod, load from DB on startup)
    private AtomicReference<String> lastHash = new AtomicReference<>("GENESIS_HASH_000000000000000000000");

    public void logEvent(String actor, String eventType, String resource, String details, String status) {
        String previousHash = lastHash.get();
        String timestamp = Instant.now().toString();

        // entry = prevHash + timestamp + actor + type + details
        String rawEntry = previousHash + "|" + timestamp + "|" + actor + "|" + eventType + "|" + resource + "|"
                + details;
        String currentHash = hash(rawEntry);

        // Update Chain Integrity
        lastHash.set(currentHash);

        // Persist to DB
        AuditLog log = new AuditLog();
        log.setActor(actor);
        log.setAction(eventType);
        log.setResource(resource);
        log.setDetails(details + " | ChainHash: " + currentHash);
        log.setStatus(status != null ? status : "success");
        log.setTimestamp(LocalDateTime.now());

        auditLogRepository.save(log);
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
