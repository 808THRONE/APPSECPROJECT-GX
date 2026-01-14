package com.securegate.iam.service;

import com.nimbusds.jose.*;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.securegate.iam.model.User;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class TokenService {

    private static final Logger LOGGER = Logger.getLogger(TokenService.class.getName());

    @Inject
    private KeyManagementService keyService;

    private String issuer;
    private int accessTokenLifetimeSec;

    @PostConstruct
    public void init() {
        this.issuer = getConfigValue("jwt.issuer", "https://iam.securegate.io");
        this.accessTokenLifetimeSec = Integer.parseInt(
                getConfigValue("jwt.expiration.seconds", "900"));
        LOGGER.info("TokenService initialized with RS256 readiness for issuer: " + issuer);
    }

    private String getConfigValue(String key, String defaultValue) {
        String value = System.getProperty(key);
        if (value != null && !value.isEmpty())
            return value;
        String envKey = key.toUpperCase().replace('.', '_');
        value = System.getenv(envKey);
        if (value != null && !value.isEmpty())
            return value;
        return defaultValue;
    }

    public String generateAccessToken(User user, String clientId, String deviceId) {
        try {
            Instant now = Instant.now();
            String jti = UUID.randomUUID().toString();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getUserId().toString())
                    .issuer(issuer)
                    .audience("securegate-api")
                    .claim("preferred_username", user.getUsername())
                    .claim("roles", user.getRoles().stream().map(r -> r.getRoleName()).toList())
                    .claim("device_id", deviceId)
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now.minusSeconds(10))) // 10s clock skew
                    .expirationTime(Date.from(now.plusSeconds(accessTokenLifetimeSec)))
                    .jwtID(jti)
                    .build();

            // Use EdDSA and include KID
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                    .keyID(keyService.getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);

            // Convert Java Key to Nimbus OctetKeyPair for signing
            // Extract the raw public key point (x) from X.509 encoding (last 32 bytes)
            byte[] pubEncoded = keyService.getPublicKey().getEncoded();
            byte[] x = java.util.Arrays.copyOfRange(pubEncoded, pubEncoded.length - 32, pubEncoded.length);

            // Extract the raw private key seed (d) from PKCS#8 encoding (last 32 bytes)
            // For Ed25519, the last 32 bytes of the PKCS#8 encoded private key is the seed.
            byte[] privEncoded = keyService.getPrivateKey().getEncoded();
            byte[] d = java.util.Arrays.copyOfRange(privEncoded, privEncoded.length - 32, privEncoded.length);

            com.nimbusds.jose.jwk.OctetKeyPair jwk = new com.nimbusds.jose.jwk.OctetKeyPair.Builder(
                    com.nimbusds.jose.jwk.Curve.Ed25519,
                    com.nimbusds.jose.util.Base64URL.encode(x))
                    .d(com.nimbusds.jose.util.Base64URL.encode(d))
                    .build();

            JWSSigner signer = new com.nimbusds.jose.crypto.Ed25519Signer(jwk);
            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Error signing token with EdDSA", e);
        }
    }
}
