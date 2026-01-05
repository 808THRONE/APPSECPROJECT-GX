package me.mortadha.iam.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.logging.Logger;

/**
 * JWT Manager with Ed25519 signing and automatic key rotation
 */
@Startup
@Singleton
@LocalBean
public class JwtManager {

    private static final Logger LOGGER = Logger.getLogger(JwtManager.class.getName());

    private final Config config = ConfigProvider.getConfig();
    private final Map<String, Long> keyPairExpirations = new HashMap<>();
    private final Set<OctetKeyPair> cachedKeyPairs = new HashSet<>();
    
    private Long keyPairLifetime;
    private Short keyPairCacheSize;
    private Integer jwtLifetime;
    private String issuer;
    private List<String> audiences;
    private String claimRoles;
    private OctetKeyPairGenerator keyPairGenerator;

    @PostConstruct
    public void initialize() {
        LOGGER.info("Initializing JWT Manager with Ed25519 key rotation");
        
        // Load configuration
        keyPairLifetime = config.getValue("key.pair.lifetime.duration", Long.class);
        keyPairCacheSize = config.getValue("key.pair.cache.size", Short.class);
        jwtLifetime = config.getValue("jwt.lifetime.duration", Integer.class);
        issuer = config.getValue("jwt.issuer", String.class);
        audiences = config.getValues("jwt.audiences", String.class);
        claimRoles = config.getValue("jwt.claim.roles", String.class);
        
        keyPairGenerator = new OctetKeyPairGenerator(Curve.Ed25519);
        
        // Pre-generate key pairs
        while (cachedKeyPairs.size() < keyPairCacheSize) {
            cachedKeyPairs.add(generateKeyPair());
        }
        
        LOGGER.info("JWT Manager initialized with " + cachedKeyPairs.size() + " key pairs");
    }

    /**
     * Generate access token (JWT) with EdDSA signature
     */
    public String generateAccessToken(String tenantId, String subject, 
                                     String scopes, String[] roles) {
        try {
            OctetKeyPair keyPair = getValidKeyPair()
                .orElseThrow(() -> new IllegalStateException("No valid key pair available"));
            
            JWSSigner signer = new Ed25519Signer(keyPair);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID(keyPair.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
            
            Instant now = Instant.now();
            
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audiences)
                .subject(subject)
                .claim("upn", subject)
                .claim("tenant_id", tenantId)
                .claim("scope", scopes)
                .claim(claimRoles, roles)
                .jwtID(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .notBeforeTime(Date.from(now))
                .expirationTime(Date.from(now.plus(jwtLifetime, ChronoUnit.SECONDS)))
                .build();
            
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
            
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate access token", e);
        }
    }

    /**
     * Generate refresh token (longer-lived, fewer claims)
     */
    public String generateRefreshToken(String tenantId, String subject, String scopes) {
        try {
            OctetKeyPair keyPair = getValidKeyPair()
                .orElseThrow(() -> new IllegalStateException("No valid key pair available"));
            
            JWSSigner signer = new Ed25519Signer(keyPair);
            
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID(keyPair.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
            
            Instant now = Instant.now();
            
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("tenant_id", tenantId)
                .claim("scope", scopes)
                .claim("token_type", "refresh")
                .expirationTime(Date.from(now.plus(3, ChronoUnit.HOURS)))
                .build();
            
            SignedJWT signedJWT = new SignedJWT(header, claimsSet);
            signedJWT.sign(signer);
            
            return signedJWT.serialize();
            
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate refresh token", e);
        }
    }

    /**
     * Validate and parse JWT
     */
    public Optional<JWT> validateJWT(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String keyId = signedJWT.getHeader().getKeyID();
            
            // Find the public key
            OctetKeyPair publicKey = cachedKeyPairs.stream()
                .filter(kp -> kp.getKeyID().equals(keyId))
                .findFirst()
                .orElse(null);
            
            if (publicKey == null) {
                LOGGER.warning("Key ID not found: " + keyId);
                return Optional.empty();
            }
            
            // Verify signature
            JWSVerifier verifier = new Ed25519Verifier(publicKey.toPublicJWK());
            if (!signedJWT.verify(verifier)) {
                LOGGER.warning("JWT signature verification failed");
                return Optional.empty();
            }
            
            // Check expiration
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            if (claims.getExpirationTime().toInstant().isBefore(Instant.now())) {
                LOGGER.fine("JWT expired");
                return Optional.empty();
            }
            
            return Optional.of(JWTParser.parse(token));
            
        } catch (ParseException | JOSEException e) {
            LOGGER.warning("JWT validation error: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get public key for external verification (JWKS)
     */
    public OctetKeyPair getPublicKey(String keyId) {
        return cachedKeyPairs.stream()
            .filter(kp -> kp.getKeyID().equals(keyId))
            .findFirst()
            .map(OctetKeyPair::toPublicJWK)
            .orElseThrow(() -> new IllegalArgumentException("Key ID not found: " + keyId));
    }

    /**
     * Get all public keys (for JWKS endpoint)
     */
    public Set<OctetKeyPair> getAllPublicKeys() {
        Set<OctetKeyPair> publicKeys = new HashSet<>();
        for (OctetKeyPair kp : cachedKeyPairs) {
            publicKeys.add(kp.toPublicJWK());
        }
        return publicKeys;
    }

    /**
     * Generate a new Ed25519 key pair
     */
    private OctetKeyPair generateKeyPair() {
        try {
            long currentEpoch = LocalDateTime.now(ZoneId.of("UTC"))
                .toEpochSecond(ZoneOffset.UTC);
            String keyId = UUID.randomUUID().toString();
            
            keyPairExpirations.put(keyId, currentEpoch + keyPairLifetime);
            
            return keyPairGenerator
                .keyUse(KeyUse.SIGNATURE)
                .keyID(keyId)
                .generate();
                
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to generate key pair", e);
        }
    }

    /**
     * Check if key pair has not expired
     */
    private boolean isKeyPairValid(OctetKeyPair keyPair) {
        long currentEpoch = LocalDateTime.now(ZoneId.of("UTC"))
            .toEpochSecond(ZoneOffset.UTC);
        Long expiration = keyPairExpirations.get(keyPair.getKeyID());
        return expiration != null && currentEpoch <= expiration;
    }

    /**
     * Check if public key can still verify tokens (grace period)
     */
    private boolean isPublicKeyExpired(OctetKeyPair keyPair) {
        long currentEpoch = LocalDateTime.now(ZoneId.of("UTC"))
            .toEpochSecond(ZoneOffset.UTC);
        Long expiration = keyPairExpirations.get(keyPair.getKeyID());
        return expiration != null && currentEpoch > (expiration + jwtLifetime);
    }

    /**
     * Get a valid key pair for signing
     */
    private Optional<OctetKeyPair> getValidKeyPair() {
        // Remove fully expired keys
        cachedKeyPairs.removeIf(this::isPublicKeyExpired);
        
        // Ensure we have enough valid signing keys
        while (cachedKeyPairs.stream().filter(this::isKeyPairValid).count() < keyPairCacheSize) {
            cachedKeyPairs.add(generateKeyPair());
        }
        
        return cachedKeyPairs.stream()
            .filter(this::isKeyPairValid)
            .findAny();
    }

    public String getClaimRoles() {
        return claimRoles;
    }
}
