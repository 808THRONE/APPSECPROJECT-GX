package com.securegate.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private static final Map<String, String> sessionStore = new ConcurrentHashMap<>();

    public static void saveSession(String sessionId, String userId) {
        sessionStore.put("session:" + sessionId, userId);
        // Note: Expiry handling is omitted for simple in-memory version
    }

    public static String getUser(String sessionId) {
        return sessionStore.get("session:" + sessionId);
    }
}
