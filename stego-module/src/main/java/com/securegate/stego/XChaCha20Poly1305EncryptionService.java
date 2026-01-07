package com.securegate.stego;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * XChaCha20-Poly1305 Encryption Service with PBKDF2-SHA512 key derivation.
 * 
 * Uses Bouncy Castle JCE Provider for modern, high-performance authenticated
 * encryption.
 * 
 * Security Parameters:
 * - 256-bit key (XChaCha20)
 * - 192-bit nonce (24 bytes) - Collision resistant
 * - 128-bit authentication tag (Poly1305)
 * - PBKDF2-SHA512 with 100,000 iterations
 */
public class XChaCha20Poly1305EncryptionService {

    private static final int KEY_SIZE_BITS = 256;
    private static final int NONCE_SIZE_BYTES = 24; // 192 bits for XChaCha20
    private static final int TAG_SIZE_BYTES = 16; // 128 bits for Poly1305
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final int SALT_SIZE_BYTES = 32;
    private static final String ALGORITHM = "XChaCha20-Poly1305";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA512";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Encrypt data using XChaCha20-Poly1305.
     * 
     * Output format: [32-byte salt][24-byte nonce][ciphertext + 16-byte tag]
     */
    public byte[] encrypt(byte[] plaintext, String password) throws Exception {
        byte[] salt = new byte[SALT_SIZE_BYTES];
        byte[] nonce = new byte[NONCE_SIZE_BYTES];
        secureRandom.nextBytes(salt);
        secureRandom.nextBytes(nonce);

        // Derive key
        SecretKey key = deriveKey(password, salt);

        // Encrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));
        byte[] ciphertext = cipher.doFinal(plaintext);

        // Combine: salt || nonce || ciphertext
        byte[] result = new byte[salt.length + nonce.length + ciphertext.length];
        System.arraycopy(salt, 0, result, 0, salt.length);
        System.arraycopy(nonce, 0, result, salt.length, nonce.length);
        System.arraycopy(ciphertext, 0, result, salt.length + nonce.length, ciphertext.length);

        return result;
    }

    /**
     * Decrypt data using XChaCha20-Poly1305.
     */
    public byte[] decrypt(byte[] cipherData, String password) throws Exception {
        int minLength = SALT_SIZE_BYTES + NONCE_SIZE_BYTES + TAG_SIZE_BYTES;
        if (cipherData.length < minLength) {
            throw new IllegalArgumentException("Ciphertext too short");
        }

        byte[] salt = Arrays.copyOfRange(cipherData, 0, SALT_SIZE_BYTES);
        byte[] nonce = Arrays.copyOfRange(cipherData, SALT_SIZE_BYTES, SALT_SIZE_BYTES + NONCE_SIZE_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(cipherData, SALT_SIZE_BYTES + NONCE_SIZE_BYTES, cipherData.length);

        // Derive key
        SecretKey key = deriveKey(password, salt);

        // Decrypt
        Cipher cipher = Cipher.getInstance(ALGORITHM, "BC");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));

        return cipher.doFinal(ciphertext);
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES"); // Use "AES" for key container, algorithm will be XChaCha
    }
}
