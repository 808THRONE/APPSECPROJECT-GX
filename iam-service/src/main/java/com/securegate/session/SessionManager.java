package com.securegate.session;

import redis.clients.jedis.JedisPooled;

public class SessionManager {

    private static final JedisPooled redis = new JedisPooled("localhost", 6379);

    public static void saveSession(String sessionId, String userId) {
        redis.setex("session:" + sessionId, 3600, userId);
    }

    public static String getUser(String sessionId) {
        return redis.get("session:" + sessionId);
    }
}
