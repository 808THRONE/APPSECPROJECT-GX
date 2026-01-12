package com.securegate.iam.mfa;

import jakarta.enterprise.context.ApplicationScoped;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

@ApplicationScoped
public class TotpService {

    private static final int SECRET_BYTES = 20; // 160 bits
    private static final int PERIOD = 30; // 30 seconds
    private static final int DIGITS = 6;
    private static final int WINDOW_SIZE = 1; // Allow +/- 1 step

    // Base32 implementation or use library. For now, simple manual or dependency.
    // Assuming no external Base32 lib, let's implement validation raw or use Apache
    // Commons Codec if avail.
    // For specific implementation, we will assume the secret is passed as byte[]
    // for verify,
    // and encoded/decoded outside or helper method added here.

    public byte[] generateSecret() {
        byte[] secret = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(secret);
        return secret;
    }

    public boolean verifyCode(byte[] secret, int code) {
        long timeWindow = System.currentTimeMillis() / 1000 / PERIOD;

        for (int i = -WINDOW_SIZE; i <= WINDOW_SIZE; i++) {
            if (generateTOTP(secret, timeWindow + i) == code) {
                return true;
            }
        }
        return false;
    }

    private int generateTOTP(byte[] key, long time) {
        byte[] data = ByteBuffer.allocate(8).putLong(time).array();
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            // Truncate
            int offset = hash[hash.length - 1] & 0xF;
            long binary = ((hash[offset] & 0x7f) << 24) |
                    ((hash[offset + 1] & 0xff) << 16) |
                    ((hash[offset + 2] & 0xff) << 8) |
                    (hash[offset + 3] & 0xff);

            return (int) (binary % Math.pow(10, DIGITS));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("TOTP Error", e);
        }
    }
}
