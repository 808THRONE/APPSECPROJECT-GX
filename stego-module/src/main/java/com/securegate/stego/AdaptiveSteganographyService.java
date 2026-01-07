package com.securegate.stego;

import java.util.logging.Logger;

/**
 * Adaptive Steganography Service combining XChaCha20-Poly1305 encryption with
 * STC
 * embedding.
 * 
 * This service provides the main API for:
 * 1. Encrypting secret data with XChaCha20-Poly1305
 * 2. Embedding encrypted data into cover images using STC
 * 3. Extracting and decrypting hidden data from stego images
 * 
 * Security Features:
 * - XChaCha20-Poly1305 authenticated encryption (192-bit nonce)
 * - PBKDF2-SHA512 key derivation (100,000 iterations)
 * - STC near-optimal embedding minimizing distortion
 * - PSNR validation (≥45dB target)
 */
public class AdaptiveSteganographyService {

    private static final Logger LOGGER = Logger.getLogger(AdaptiveSteganographyService.class.getName());
    private static final double MIN_PSNR = 45.0; // Minimum acceptable PSNR in dB

    private final XChaCha20Poly1305EncryptionService encryptionService;
    private final StcStegoEngine stegoEngine;

    public AdaptiveSteganographyService() {
        this.encryptionService = new XChaCha20Poly1305EncryptionService();
        this.stegoEngine = new StcStegoEngine();
    }

    /**
     * Embed encrypted secret into cover image.
     * 
     * @param coverImage Raw image bytes
     * @param secret     Secret data to hide
     * @param password   Password for encryption
     * @return Stego image containing encrypted secret
     */
    public StegoResult embedSecret(byte[] coverImage, byte[] secret, String password) throws Exception {
        LOGGER.info("Starting secure embedding: " + secret.length + " bytes into " + coverImage.length + " byte cover");

        // Step 1: Encrypt the secret with XChaCha20-Poly1305
        byte[] encryptedSecret = encryptionService.encrypt(secret, password);
        LOGGER.fine("Encrypted secret size: " + encryptedSecret.length + " bytes");

        // Step 2: Embed encrypted data using STC
        byte[] stegoImage = stegoEngine.embed(coverImage, encryptedSecret);

        // Step 3: Validate quality (PSNR)
        double psnr = stegoEngine.calculatePSNR(coverImage, stegoImage);
        LOGGER.info("Embedding complete. PSNR: " + String.format("%.2f", psnr) + " dB");

        if (psnr < MIN_PSNR) {
            LOGGER.warning("PSNR below threshold: " + psnr + " dB < " + MIN_PSNR + " dB");
        }

        return new StegoResult(stegoImage, psnr, encryptedSecret.length);
    }

    /**
     * Extract and decrypt secret from stego image.
     * 
     * @param stegoImage Stego image containing hidden data
     * @param password   Password for decryption
     * @return Decrypted secret data
     */
    public byte[] extractSecret(byte[] stegoImage, String password) throws Exception {
        LOGGER.info("Starting extraction from " + stegoImage.length + " byte stego image");

        // Step 1: Extract encrypted data using STC
        byte[] encryptedSecret = stegoEngine.extract(stegoImage);
        LOGGER.fine("Extracted encrypted payload: " + encryptedSecret.length + " bytes");

        // Step 2: Decrypt with XChaCha20-Poly1305
        byte[] secret = encryptionService.decrypt(encryptedSecret, password);
        LOGGER.info("Extraction complete. Decrypted " + secret.length + " bytes");

        return secret;
    }

    /**
     * Check if cover image has sufficient capacity for secret.
     * 
     * @param coverImageSize Size of cover image in bytes
     * @param secretSize     Size of secret in bytes
     * @return true if capacity is sufficient
     */
    public boolean hasCapacity(int coverImageSize, int secretSize) {
        // Account for encryption overhead: 32-byte salt + 24-byte nonce + 16-byte tag =
        // 72 bytes
        int encryptedSize = secretSize + 72;
        // STC header is 4 bytes
        int requiredCapacity = encryptedSize + 4;
        return coverImageSize >= requiredCapacity;
    }

    /**
     * Calculate maximum payload capacity for a cover image.
     * 
     * @param coverImageSize Size of cover image in bytes
     * @return Maximum secret size in bytes
     */
    public int calculateMaxCapacity(int coverImageSize) {
        // Subtract STC header (4 bytes) and encryption overhead (72 bytes)
        return Math.max(0, coverImageSize - 4 - 72);
    }

    /**
     * Result of steganographic embedding operation.
     */
    public record StegoResult(
            byte[] stegoImage,
            double psnr,
            int embeddedPayloadSize) {
        public boolean meetsQualityThreshold() {
            return psnr >= MIN_PSNR;
        }
    }
}
