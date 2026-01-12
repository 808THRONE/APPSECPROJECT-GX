package com.securegate.iam.service;

import jakarta.enterprise.context.ApplicationScoped;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;

@ApplicationScoped
public class CryptoService {

    private static final int SALT_LENGTH = 16;
    private static final int HASH_LENGTH = 32;
    private static final int OPS_LIMIT = 3; // Iterations
    private static final int MEM_LIMIT = 65536; // 64MB
    private static final int PARALLELISM = 1;

    public String hashPassword(String password) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);

        byte[] hash = computeArgon2(password, salt);

        // Format: $argon2id$v=19$m=65536,t=3,p=1$SALT$HASH
        return String.format("$argon2id$v=19$m=%d,t=%d,p=%d$%s$%s",
                MEM_LIMIT, OPS_LIMIT, PARALLELISM,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash));
    }

    public boolean verifyPassword(String password, String encodedHash) {
        // Simple parser for the format above
        // NOTE: Production code should use a robust parser/library to handle variants
        try {
            String[] parts = encodedHash.split("\\$");
            // parts[0] is empty, parts[1]=argon2id, parts[2]=v=19, parts[3]=params,
            // parts[4]=salt, parts[5]=hash
            if (parts.length < 6)
                return false;

            // Extract salt and re-compute
            byte[] salt = Base64.getDecoder().decode(parts[4]);
            byte[] originalHash = Base64.getDecoder().decode(parts[5]);

            byte[] computedHash = computeArgon2(password, salt);

            return MessageDigest.isEqual(originalHash, computedHash);
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] computeArgon2(String password, byte[] salt) {
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withParallelism(PARALLELISM)
                .withMemoryAsKB(MEM_LIMIT)
                .withIterations(OPS_LIMIT);

        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(builder.build());

        byte[] result = new byte[HASH_LENGTH];
        generator.generateBytes(password.getBytes(StandardCharsets.UTF_8), result, 0, result.length);
        return result;
    }

    public String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
