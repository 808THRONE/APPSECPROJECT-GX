package com.securegate.mfa;

public class TOTPService {

    public static String generateSecret() {
        SecureRandom sr = new SecureRandom();
        byte[] bytes = new byte[20];
        sr.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
