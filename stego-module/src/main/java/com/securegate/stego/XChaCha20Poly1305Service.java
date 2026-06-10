package com.securegate.stego;

import org.bouncycastle.crypto.modes.ChaCha20Poly1305;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class XChaCha20Poly1305Service {

    private static final int NONCE_SIZE = 24; // 192 bits for XChaCha20
    private static final int KEY_SIZE = 32;   // 256 bits

    public XChaCha20Poly1305Service() {
    }

    /**
     * Derives a 256-bit key from a password using Argon2id.
     */
    public byte[] deriveKey(char[] password, byte[] salt) {
        org.bouncycastle.crypto.generators.Argon2BytesGenerator gen = new org.bouncycastle.crypto.generators.Argon2BytesGenerator();
        org.bouncycastle.crypto.params.Argon2Parameters params = new org.bouncycastle.crypto.params.Argon2Parameters.Builder(org.bouncycastle.crypto.params.Argon2Parameters.ARGON2_id)
                .withSalt(salt)
                .withIterations(2)
                .withMemoryAsKB(65536)
                .withParallelism(1)
                .build();
        gen.init(params);

        byte[] key = new byte[KEY_SIZE];
        byte[] passBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        gen.generateBytes(passBytes, key);
        Arrays.fill(passBytes, (byte) 0);

        return key;
    }

    public byte[] generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    public byte[] generateNonce() {
        byte[] nonce = new byte[NONCE_SIZE];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    /**
     * Applies HChaCha20 to derive a 256-bit subkey from the 256-bit key and first 16 bytes of the nonce.
     */
    private byte[] hChaCha20(byte[] key, byte[] nonce16) {
        // HChaCha20 uses the ChaCha20 core block function.
        // Bouncy Castle doesn't expose HChaCha20 directly, but we can implement it or use standard ChaChaEngine setup.
        // HChaCha20 is essentially running the ChaCha20 block function with a specific constant and returning specific words.

        // Let's implement HChaCha20 manually to fully support XChaCha20.
        int[] state = new int[16];

        // Constants "expand 32-byte k"
        state[0] = 0x61707865;
        state[1] = 0x3320646e;
        state[2] = 0x79622d32;
        state[3] = 0x6b206574;

        // Key
        state[4] = pack(key, 0);
        state[5] = pack(key, 4);
        state[6] = pack(key, 8);
        state[7] = pack(key, 12);
        state[8] = pack(key, 16);
        state[9] = pack(key, 20);
        state[10] = pack(key, 24);
        state[11] = pack(key, 28);

        // Nonce (16 bytes)
        state[12] = pack(nonce16, 0);
        state[13] = pack(nonce16, 4);
        state[14] = pack(nonce16, 8);
        state[15] = pack(nonce16, 12);

        // 20 rounds (10 double rounds)
        for (int i = 0; i < 10; i++) {
            quarterRound(state, 0, 4, 8, 12);
            quarterRound(state, 1, 5, 9, 13);
            quarterRound(state, 2, 6, 10, 14);
            quarterRound(state, 3, 7, 11, 15);
            quarterRound(state, 0, 5, 10, 15);
            quarterRound(state, 1, 6, 11, 12);
            quarterRound(state, 2, 7, 8, 13);
            quarterRound(state, 3, 4, 9, 14);
        }

        byte[] subkey = new byte[32];
        unpack(state[0], subkey, 0);
        unpack(state[1], subkey, 4);
        unpack(state[2], subkey, 8);
        unpack(state[3], subkey, 12);
        unpack(state[12], subkey, 16);
        unpack(state[13], subkey, 20);
        unpack(state[14], subkey, 24);
        unpack(state[15], subkey, 28);

        return subkey;
    }

    private void quarterRound(int[] x, int a, int b, int c, int d) {
        x[a] += x[b]; x[d] = Integer.rotateLeft(x[d] ^ x[a], 16);
        x[c] += x[d]; x[b] = Integer.rotateLeft(x[b] ^ x[c], 12);
        x[a] += x[b]; x[d] = Integer.rotateLeft(x[d] ^ x[a], 8);
        x[c] += x[d]; x[b] = Integer.rotateLeft(x[b] ^ x[c], 7);
    }

    private int pack(byte[] b, int offset) {
        return (b[offset] & 0xFF) |
               ((b[offset + 1] & 0xFF) << 8) |
               ((b[offset + 2] & 0xFF) << 16) |
               ((b[offset + 3] & 0xFF) << 24);
    }

    private void unpack(int v, byte[] b, int offset) {
        b[offset] = (byte) v;
        b[offset + 1] = (byte) (v >>> 8);
        b[offset + 2] = (byte) (v >>> 16);
        b[offset + 3] = (byte) (v >>> 24);
    }

    /**
     * Encrypts plaintext using true XChaCha20-Poly1305.
     */
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] nonce, byte[] associatedData) throws Exception {
        if (nonce.length != NONCE_SIZE) {
            throw new IllegalArgumentException("Nonce must be 24 bytes for XChaCha20");
        }

        byte[] nonce16 = new byte[16];
        System.arraycopy(nonce, 0, nonce16, 0, 16);

        byte[] subkey = hChaCha20(key, nonce16);

        // The final 8 bytes of the 24-byte nonce are prepended with 4 null bytes to form the 12-byte nonce for ChaCha20Poly1305
        byte[] finalNonce = new byte[12];
        System.arraycopy(nonce, 16, finalNonce, 4, 8);

        ChaCha20Poly1305 cipher = new ChaCha20Poly1305();
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(subkey), finalNonce);

        cipher.init(true, params);
        if (associatedData != null) {
            cipher.processAADBytes(associatedData, 0, associatedData.length);
        }

        byte[] ciphertext = new byte[cipher.getOutputSize(plaintext.length)];
        int len = cipher.processBytes(plaintext, 0, plaintext.length, ciphertext, 0);
        cipher.doFinal(ciphertext, len);

        return ciphertext;
    }

    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] nonce, byte[] associatedData) throws Exception {
        if (nonce.length != NONCE_SIZE) {
            throw new IllegalArgumentException("Nonce must be 24 bytes for XChaCha20");
        }

        byte[] nonce16 = new byte[16];
        System.arraycopy(nonce, 0, nonce16, 0, 16);

        byte[] subkey = hChaCha20(key, nonce16);

        byte[] finalNonce = new byte[12];
        System.arraycopy(nonce, 16, finalNonce, 4, 8);

        ChaCha20Poly1305 cipher = new ChaCha20Poly1305();
        ParametersWithIV params = new ParametersWithIV(new KeyParameter(subkey), finalNonce);

        cipher.init(false, params);
        if (associatedData != null) {
            cipher.processAADBytes(associatedData, 0, associatedData.length);
        }

        byte[] plaintext = new byte[cipher.getOutputSize(ciphertext.length)];
        int len = cipher.processBytes(ciphertext, 0, ciphertext.length, plaintext, 0);
        cipher.doFinal(plaintext, len);

        return plaintext;
    }
}
