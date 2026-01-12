package com.securegate.iam.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TotpService {

    private final GoogleAuthenticator gAuth;

    public TotpService() {
        this.gAuth = new GoogleAuthenticator();
    }

    public String generateSecret() {
        final GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    public boolean verifyCode(String secret, int code) {
        return gAuth.authorize(secret, code);
    }

    public String getQrCodeUrl(String username, String secret) {
        // Simple manual QR URL gen or use library helper if available.
        // Format: otpauth://totp/SecureGate:username?secret=SECRET&issuer=SecureGate
        return String.format("otpauth://totp/SecureGate:%s?secret=%s&issuer=SecureGate", username, secret);
    }
}
