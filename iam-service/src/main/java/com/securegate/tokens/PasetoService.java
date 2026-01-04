package com.securegate.tokens;

import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.lang.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

public class PasetoService {
    private static final SecretKey SECRET_KEY;

    static {
        SecretKey tempKey;
        String envKey = System.getenv("PASETO_SECRET_KEY");
        if (envKey != null && envKey.length() >= 32) {
            try {
                // Try decoding if base64
                byte[] decodedKey = Base64.getDecoder().decode(envKey);
                tempKey = new javax.crypto.spec.SecretKeySpec(decodedKey, "Ed25519");
            } catch (IllegalArgumentException e) {
                // Treat as raw bytes padding or hashing if needed, or just warn.
                // For simplicity, fallback to random if invalid
                tempKey = Keys.secretKey();
            }
        } else {
            tempKey = Keys.secretKey();
        }
        SECRET_KEY = tempKey;
    }

    public static String createAccessToken(String userId) {
        return Pasetos.V2.LOCAL.builder()
                .setSharedSecret(SECRET_KEY)
                .setSubject(userId)
                .setExpiration(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("role", "admin")
                .compact();
    }
}
