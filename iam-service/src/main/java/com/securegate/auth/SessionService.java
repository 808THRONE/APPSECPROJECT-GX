package com.securegate.auth;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class SessionService {
    // In-memory session store: SessionID -> Username
    private final Map<String, String> activeSessions = new ConcurrentHashMap<>();

    public String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, username);
        return sessionId;
    }

    public Optional<String> getUsername(String sessionId) {
        return Optional.ofNullable(activeSessions.get(sessionId));
    }

    public void invalidateSession(String sessionId) {
        activeSessions.remove(sessionId);
    }

    public boolean isValid(String sessionId) {
        return activeSessions.containsKey(sessionId);
    }
}
