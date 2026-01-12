package com.securegate.stego;

import java.util.Random;

import jakarta.enterprise.inject.Vetoed;

/**
 * Syndrome Trellis Codes (STC) Implementation
 * 
 * Simplified implementation for demonstration of concept.
 * In production, this would use matrix embedding with proper Viterbi algorithm
 * for minimizing distortion.
 */
@jakarta.enterprise.context.ApplicationScoped
public class StcEngine {
    private int constraintHeight;

    public StcEngine() {
        this.constraintHeight = 10;
    }

    public StcEngine(int constraintHeight) {
        this.constraintHeight = constraintHeight;
    }

    /**
     * Embeds message bits into cover bits using STC to minimize changes.
     * 
     * @param cover   The cover object (e.g., LSBs of an image)
     * @param message The message bits to hide
     * @return The stego object (modified cover)
     */
    public int[] embed(int[] cover, int[] message) {
        // Mock Implementation of STC Embedding
        // Real STC minimizes: D = w * |x - y| s.t. Hx = m

        int[] stego = cover.clone();

        // Simple LSB replacement for demo ensuring 100% capacity if message <= cover
        // A real STC would spread changes optimally.
        for (int i = 0; i < message.length && i < stego.length; i++) {
            // Clear LSB
            stego[i] &= ~1;
            // Set LSB to message bit
            stego[i] |= (message[i] & 1);
        }

        return stego;
    }

    /**
     * Extracts message bits from stego object.
     * 
     * @param stego  The stego object
     * @param length Length of message to extract
     * @return The extracted message bits
     */
    public int[] extract(int[] stego, int length) {
        int[] message = new int[length];

        for (int i = 0; i < length && i < stego.length; i++) {
            message[i] = stego[i] & 1;
        }

        return message;
    }

    /**
     * Converts string to bit array
     */
    public int[] stringToBits(String data) {
        byte[] bytes = data.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        int[] bits = new int[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = (bytes[i] >> (7 - j)) & 1;
            }
        }
        return bits;
    }

    /**
     * Converts bit array back to string
     */
    public String bitsToString(int[] bits) {
        byte[] bytes = new byte[bits.length / 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                if (bits[i * 8 + j] == 1) {
                    bytes[i] |= (1 << (7 - j));
                }
            }
        }
        return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }
}
