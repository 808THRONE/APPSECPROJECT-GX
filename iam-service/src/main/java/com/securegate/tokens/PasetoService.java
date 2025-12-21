package com.securegate.tokens;

import dev.paseto.jpaseto.Pasetos;
import dev.paseto.jpaseto.lang.Keys;
import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class PasetoService {
    private static final SecretKey SECRET_KEY = Keys.secretKey();

    public static String createAccessToken(String userId, String username, String scope) {
        return Pasetos.V2.LOCAL.builder()
                .setSharedSecret(SECRET_KEY)
                .setSubject(userId)
                .setExpiration(Instant.now().plus(1, ChronoUnit.HOURS))
                .claim("username", username)
                .claim("scope", scope)
                .compact();
    }
}
