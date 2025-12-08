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
}
