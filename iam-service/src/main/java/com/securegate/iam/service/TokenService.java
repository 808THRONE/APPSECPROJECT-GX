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

            // Use RS256 and include KID
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                    .keyID(keyService.getKeyId())
                    .type(JOSEObjectType.JWT)
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);

            JWSSigner signer = new com.nimbusds.jose.crypto.RSASSASigner(keyService.getPrivateKey());
            signedJWT.sign(signer);
            return signedJWT.serialize();

        } catch (JOSEException e) {
            throw new RuntimeException("Error signing token with RS256", e);
        }
    }
}
