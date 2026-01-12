package com.securegate.iam.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

@ApplicationScoped
public class RevocationService {

    private static final Logger LOGGER = Logger.getLogger(RevocationService.class.getName());

    private JedisPool jedisPool;
    private String redisHost;
    private int redisPort;
    private String redisPass;
    private static final java.util.Map<String, String> memoryStore = new java.util.concurrent.ConcurrentHashMap<>();
    private boolean redisAvailable = false;

    @PostConstruct
    public void init() {
        redisHost = System.getenv("REDIS_HOST");
        if (redisHost == null)
            redisHost = "localhost";

        String portStr = System.getenv("REDIS_PORT");
        redisPort = (portStr != null) ? Integer.parseInt(portStr) : 6379;

        redisPass = System.getenv("REDIS_PASSWORD");
        if (redisPass == null || redisPass.isEmpty())
            redisPass = "redis_password";

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(128);
            jedisPool = new JedisPool(poolConfig, redisHost, redisPort, 2000, redisPass);
            // Test connection
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                redisAvailable = true;
                LOGGER.info("Successfully connected to Redis at " + redisHost + ":" + redisPort);
            }
        } catch (Exception e) {
            LOGGER.warning("Redis not available for RevocationService, falling back to memory: " + e.getMessage());
            redisAvailable = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public void revokeJti(String jti, long ttlSeconds) {
        if (jti == null)
            return;
        String key = "revoked_jti:" + hash(jti);
        if (redisAvailable) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex(key, ttlSeconds, "1");
                LOGGER.info("Revoked JTI: " + jti + " (digest stored in Redis)");
                return;
            } catch (Exception e) {
                LOGGER.severe("Failed to revoke JTI in Redis, using memory: " + e.getMessage());
            }
        }
        memoryStore.put(key, "1");
    }

    public boolean isJtiRevoked(String jti) {
        if (jti == null)
            return false;
        String key = "revoked_jti:" + hash(jti);
        if (redisAvailable) {
            try (Jedis jedis = jedisPool.getResource()) {
                return jedis.exists(key);
            } catch (Exception e) {
                LOGGER.severe("Failed to check JTI status in Redis, checking memory: " + e.getMessage());
            }
        }
        return memoryStore.containsKey(key);
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return input; // Fallback
        }
    }
}
