package com.securegate.iam.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class RefreshTokenService {

    private static final Logger LOGGER = Logger.getLogger(RefreshTokenService.class.getName());

    private JedisPool jedisPool;
    private static final java.util.Map<String, String> memoryStore = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean redisAvailable = false;

    @PostConstruct
    public void init() {
        String host = System.getenv("REDIS_HOST");
        if (host == null)
            host = "localhost";
        String pass = System.getenv("REDIS_PASSWORD");
        if (pass == null || pass.isEmpty())
            pass = "redis_password";

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            jedisPool = new JedisPool(poolConfig, host, 6379, 2000, pass);
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                redisAvailable = true;
                LOGGER.info("Successfully connected to Redis at " + host);
            }
        } catch (Exception e) {
            LOGGER.warning("Redis not available, falling back to in-memory store: " + e.getMessage());
            redisAvailable = false;
        }
    }

    public String createRefreshToken(String userId) {
        String token = UUID.randomUUID().toString();
        String hashedToken = hash(token);

        if (redisAvailable) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex("rt:" + hashedToken, 7 * 24 * 3600, userId);
                return token;
            } catch (Exception e) {
                LOGGER.warning("Redis error during createRefreshToken, using memory: " + e.getMessage());
            }
        }
        
        memoryStore.put("rt:" + hashedToken, userId);
        return token;
    }

    public String rotateToken(String oldToken) {
        String hashedOld = hash(oldToken);
        if (redisAvailable) {
            try (Jedis jedis = jedisPool.getResource()) {
                String userId = jedis.get("rt:" + hashedOld);
                if (userId != null) {
                    jedis.del("rt:" + hashedOld);
                    return createRefreshToken(userId);
                }
            } catch (Exception e) {
                LOGGER.warning("Redis error during rotateToken, checking memory: " + e.getMessage());
            }
        }

        String userId = memoryStore.remove("rt:" + hashedOld);
        if (userId == null) {
            LOGGER.warning("Refresh token replay or invalid token detected!");
            return null;
        }
        return createRefreshToken(userId);
    }

    public String verifyAndGetUserId(String token) {
        String hashed = hash(token);
        if (redisAvailable) {
            try (Jedis jedis = jedisPool.getResource()) {
                String userId = jedis.get("rt:" + hashed);
                if (userId != null) return userId;
            } catch (Exception e) {
                LOGGER.warning("Redis error during verifyAndGetUserId, checking memory: " + e.getMessage());
            }
        }
        return memoryStore.get("rt:" + hashed);
    }

    public void revokeByUser(String userId) {
        // This is tricky without a secondary index (userId -> tokenList).
        // Enterprise usually stores this in a Set: "user_tokens:userId" -> [jti1, jti2]
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return Base64.getEncoder().encodeToString(md.digest(input.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
