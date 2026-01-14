package com.securegate.api.service;

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
    private boolean redisAvailable = false;

    @PostConstruct
    public void init() {
        String host = System.getenv("REDIS_HOST");
        if (host == null)
            host = "localhost";

        String portStr = System.getenv("REDIS_PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 6379;

        String pass = System.getenv("REDIS_PASSWORD");
        if (pass == null || pass.isEmpty())
            pass = "redis_password";

        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(32);
            jedisPool = new JedisPool(poolConfig, host, port, 2000, pass);

            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
                redisAvailable = true;
                LOGGER.info("Gateway RevocationService connected to Redis at " + host);
            }
        } catch (Exception e) {
            LOGGER.warning("Redis not available for Gateway RevocationService: " + e.getMessage());
        }
    }

    @PreDestroy
    public void cleanup() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public boolean isJtiRevoked(String jti) {
        if (jti == null || !redisAvailable)
            return false;

        String key = "revoked_jti:" + hash(jti);
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists(key);
        } catch (Exception e) {
            LOGGER.severe("Failed to check JTI status in Redis: " + e.getMessage());
            return false;
        }
    }

    private String hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            return input;
        }
    }
}
