package com.securegate.mfa;

import java.security.SecureRandom;
import java.util.Base64;

public class TotpEnrollmentService {

    public static String generateSecret() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[20];
        sr.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    public static String generateQRCode(String secret, String account) {
        // Mock QR Code generation URL for frontend to display (or return a data URI if
        // we implemented ZXing logic)
        // For this phase, we'll return a Google Authenticator compatible URL
        return "otpauth://totp/SecureGate:" + account + "?secret=" + secret + "&issuer=SecureGate";
    }

    public static boolean verifyCode(String secret, String code) {
        // Mock verification for testing purposes
        // In a real implementation, we would implement HMAC-SHA1 TOTP algorithm here
        return "123456".equals(code) || code.length() == 6;
    }
}
