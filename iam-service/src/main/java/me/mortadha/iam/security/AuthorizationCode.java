package me.mortadha.iam.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Authorization Code with ChaCha20-Poly1305 encryption
 * Format: urn:mortadha:code:<uuid>:<encrypted_payload>:<encrypted_challenge>
 */
public record AuthorizationCode(
    String tenantId,
    String username,
    String approvedScopes,
    Long expirationEpoch,
    String redirectUri
) {
    private static final String CODE_PREFIX = "urn:mortadha:code:";
    private static final String CIPHER_ALGO = "ChaCha20-Poly1305";
    private static final int NONCE_LENGTH = 12; // 96 bits
    private static final SecretKey SECRET_KEY;

    static {
        try {
            SECRET_KEY = KeyGenerator.getInstance("ChaCha20").generateKey();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to initialize ChaCha20 key", e);
        }
    }

    /**
     * Generate encrypted authorization code with PKCE challenge
     */
    public String encode(String codeChallenge) throws GeneralSecurityException {
        String codeId = UUID.randomUUID().toString();
        
        // Create payload: tenantId:username:scopes:expiration:redirectUri
        String payload = String.join(":", 
            tenantId, username, approvedScopes, 
            String.valueOf(expirationEpoch), redirectUri
        );
        
        String encodedPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        
        // Encrypt the code challenge
        byte[] encryptedChallenge = encrypt(codeChallenge.getBytes(StandardCharsets.UTF_8));
        String encodedChallenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(encryptedChallenge);
        
        // Format: PREFIX + UUID + : + PAYLOAD + : + ENCRYPTED_CHALLENGE
        return CODE_PREFIX + codeId + ":" + encodedPayload + ":" + encodedChallenge;
    }

    /**
     * Decode and verify authorization code with PKCE verifier
     */
    public static AuthorizationCode decode(String authCode, String codeVerifier) 
            throws GeneralSecurityException {
        
        if (!authCode.startsWith(CODE_PREFIX)) {
            throw new IllegalArgumentException("Invalid authorization code format");
        }

        // Remove prefix
        String codeWithoutPrefix = authCode.substring(CODE_PREFIX.length());
        String[] parts = codeWithoutPrefix.split(":");
        
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid authorization code structure");
        }

        // parts[0] = UUID
        // parts[1] = Base64 encoded payload
        // parts[2] = Base64 encoded encrypted challenge
        
        // Verify PKCE challenge
        String encryptedChallenge = parts[2];
        byte[] decryptedChallenge = decrypt(
            Base64.getUrlDecoder().decode(encryptedChallenge)
        );
        
        String storedChallenge = new String(decryptedChallenge, StandardCharsets.UTF_8);
        String computedChallenge = computeS256Challenge(codeVerifier);
        
        if (!storedChallenge.equals(computedChallenge)) {
            throw new SecurityException("PKCE verification failed");
        }

        // Decode payload
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        String payload = new String(payloadBytes, StandardCharsets.UTF_8);
        String[] attributes = payload.split(":");
        
        if (attributes.length != 5) {
            throw new IllegalArgumentException("Invalid payload structure");
        }

        return new AuthorizationCode(
            attributes[0], // tenantId
            attributes[1], // username
            attributes[2], // approvedScopes
            Long.parseLong(attributes[3]), // expirationEpoch
            attributes[4] // redirectUri (may contain colons, so join rest)
        );
    }

    /**
     * Compute SHA-256 of code verifier (PKCE S256)
     */
    private static String computeS256Challenge(String codeVerifier) 
            throws GeneralSecurityException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
    }

    /**
     * Encrypt data using ChaCha20-Poly1305
     */
    private static byte[] encrypt(byte[] plaintext) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        
        byte[] nonce = new byte[NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);
        
        IvParameterSpec iv = new IvParameterSpec(nonce);
        cipher.init(Cipher.ENCRYPT_MODE, SECRET_KEY, iv);
        
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        // Append nonce to ciphertext
        return ByteBuffer.allocate(ciphertext.length + NONCE_LENGTH)
            .put(ciphertext)
            .put(nonce)
            .array();
    }

    /**
     * Decrypt data using ChaCha20-Poly1305
     */
    private static byte[] decrypt(byte[] ciphertextWithNonce) throws GeneralSecurityException {
        ByteBuffer buffer = ByteBuffer.wrap(ciphertextWithNonce);
        
        byte[] ciphertext = new byte[ciphertextWithNonce.length - NONCE_LENGTH];
        byte[] nonce = new byte[NONCE_LENGTH];
        
        buffer.get(ciphertext);
        buffer.get(nonce);
        
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        IvParameterSpec iv = new IvParameterSpec(nonce);
        cipher.init(Cipher.DECRYPT_MODE, SECRET_KEY, iv);
        
        return cipher.doFinal(ciphertext);
    }

    /**
     * Check if authorization code is expired
     */
    public boolean isExpired() {
        return System.currentTimeMillis() / 1000 > expirationEpoch;
    }
}
