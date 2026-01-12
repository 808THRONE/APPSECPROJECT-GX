package com.securegate.stego;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Modern Encryption Service
 * Attempts to use ChaCha20-Poly1305 (Java 11+).
 */
@jakarta.enterprise.context.ApplicationScoped
public class EncryptionService {

    private static final String ALGORITHM = "ChaCha20-Poly1305";
    private static final int NONCE_LEN = 12; // 96 bits for standard ChaCha20
    private static final int KEY_LEN = 256;

    public String encrypt(String plaintext, String keyBase64) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        System.out.println("DEBUG: Key length: " + keyBytes.length);
        SecretKey key = new SecretKeySpec(keyBytes, "ChaCha20");

        byte[] nonce = new byte[NONCE_LEN];
        new SecureRandom().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(nonce));

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

        // Return Nonce + Ciphertext
        byte[] result = new byte[nonce.length + ciphertext.length];
        System.arraycopy(nonce, 0, result, 0, nonce.length);
        System.arraycopy(ciphertext, 0, result, nonce.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(result);
    }

    public String decrypt(String ciphertextBase64, String keyBase64) throws Exception {
        byte[] data = Base64.getDecoder().decode(ciphertextBase64);
        byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
        SecretKey key = new SecretKeySpec(keyBytes, "ChaCha20");

        if (data.length < NONCE_LEN)
            throw new IllegalArgumentException("Invalid ciphertext");

        byte[] nonce = new byte[NONCE_LEN];
        System.arraycopy(data, 0, nonce, 0, NONCE_LEN);

        byte[] rawCipher = new byte[data.length - NONCE_LEN];
        System.arraycopy(data, NONCE_LEN, rawCipher, 0, rawCipher.length);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(nonce));

        return new String(cipher.doFinal(rawCipher), java.nio.charset.StandardCharsets.UTF_8);
    }

    public String generateKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("ChaCha20");
        keyGen.init(KEY_LEN);
        SecretKey key = keyGen.generateKey();
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
}
