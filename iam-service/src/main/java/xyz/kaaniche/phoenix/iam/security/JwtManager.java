package xyz.kaaniche.phoenix.iam.security;

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
import jakarta.ejb.EJBException;
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

@Startup
@Singleton
@LocalBean
public class  JwtManager {
    private final Config config = ConfigProvider.getConfig();
    private final Map<String,Long> keyPairExpirationTimes = new HashMap<>();
    private final Set<OctetKeyPair> cachedKeyPairs = new HashSet<>();
    private final Long keyPairLifetimeDuration = config.getValue("key.pair.lifetime.duration",Long.class);
    private final Short keyPairCacheSize = config.getValue("key.pair.cache.size",Short.class);
    private final Integer jwtLifetimeDuration = config.getValue("jwt.lifetime.duration",Integer.class);
    private final String issuer = config.getValue("jwt.issuer",String.class);
    private final List<String> audiences = config.getValues("jwt.audiences",String.class);
    private final String claimRoles = config.getValue("jwt.claim.roles",String.class);
    private final OctetKeyPairGenerator keyPairGenerator = new OctetKeyPairGenerator(Curve.Ed25519);

    @PostConstruct
    public void start(){
        while (cachedKeyPairs.size()<keyPairCacheSize){
            cachedKeyPairs.add(generateKeyPair());
        }
    }

    public String generateAccessToken(String tenantId, String subject, String approvedScopes, String[] roles){
        try {
            OctetKeyPair octetKeyPair = getKeyPair()
                    .orElseThrow(()->new EJBException("Unable to retrieve a valid Ed25519 KeyPair"));
            JWSSigner signer = new Ed25519Signer(octetKeyPair);
            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                    .keyID(octetKeyPair.getKeyID())
                    .type(JOSEObjectType.JWT)
                    .build();
            Instant now = Instant.now();
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .audience(audiences)
                    .subject(subject)
                    .claim("upn",subject)
                    .claim("tenant_id",tenantId)
                    .claim("scope", approvedScopes)
                    .claim(claimRoles, roles)
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(now))
                    .notBeforeTime(Date.from(now))
                    .expirationTime(Date.from(now.plus(jwtLifetimeDuration, ChronoUnit.SECONDS)))
                    .build();
            SignedJWT signedJWT = new SignedJWT(header,claimsSet);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new EJBException(e);
        }
    }
    public String generateRefreshToken(String clientId, String subject, String approvedScope) throws Exception {
        OctetKeyPair octetKeyPair = getKeyPair()
                .orElseThrow(()->new EJBException("Unable to retrieve a valid Ed25519 KeyPair"));
        JWSSigner signer = new Ed25519Signer(octetKeyPair);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID(octetKeyPair.getKeyID())
                .type(JOSEObjectType.JWT)
                .build();
        Instant now = Instant.now();
        //6.Build refresh token
        JWTClaimsSet refreshTokenClaims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("tenant_id", clientId)
                .claim("scope", approvedScope)
                //refresh token for 3 hours.
                .expirationTime(Date.from(now.plus(3, ChronoUnit.HOURS)))
                .build();
        SignedJWT signedRefreshToken = new SignedJWT(header,refreshTokenClaims);
        signedRefreshToken.sign(signer);
        return signedRefreshToken.serialize();
    }

    public Optional<JWT> validateJWT(String token){
        try {
            SignedJWT parsed = SignedJWT.parse(token);
            OctetKeyPair publicKey = cachedKeyPairs.stream()
                    .filter(kp -> kp.getKeyID().equals(parsed.getHeader().getKeyID()))
                    .findFirst()
                    .orElseThrow(()->new EJBException("Unable to retrieve the key pair associated with the kid"))
                    .toPublicJWK();
            JWSVerifier verifier = new Ed25519Verifier(publicKey);
            if(parsed.verify(verifier)){
                if(parsed.getJWTClaimsSet().getExpirationTime().toInstant().isBefore(Instant.now())){
                    return Optional.empty();
                }
                return Optional.of(JWTParser.parse(token));
            }
            return Optional.empty();
        } catch (ParseException | JOSEException e) {
            throw new EJBException(e);
        }
    }

    public OctetKeyPair getPublicValidationKey(String kid){
        return cachedKeyPairs.stream()
                .filter(kp -> kp.getKeyID().equals(kid))
                .findFirst()
                .orElseThrow(()->new EJBException("Unable to retrieve the key pair associated with the kid"))
                .toPublicJWK();
    }

    private OctetKeyPair generateKeyPair(){
        //Generate a key pair with Ed25519 curve
        try {
            Long currentUTCSeconds = LocalDateTime.now(ZoneId.of("UTC")).toEpochSecond(ZoneOffset.UTC);
            String kid = UUID.randomUUID().toString();
            keyPairExpirationTimes.put(kid,currentUTCSeconds+keyPairLifetimeDuration);
            return keyPairGenerator.keyUse(KeyUse.SIGNATURE)
                    .keyID(kid).generate();
        } catch (JOSEException e) {
            throw new EJBException(e);
        }
    }

    private boolean hasNotExpired(OctetKeyPair keyPair){
        long currentUTCSeconds = LocalDateTime.now(ZoneId.of("UTC")).toEpochSecond(ZoneOffset.UTC);
        return currentUTCSeconds <= keyPairExpirationTimes.get(keyPair.getKeyID());
    }

    private boolean isPublicKeyExpired(OctetKeyPair keyPair){
        long currentUTCSeconds = LocalDateTime.now(ZoneId.of("UTC")).toEpochSecond(ZoneOffset.UTC);
        return currentUTCSeconds > (keyPairExpirationTimes.get(keyPair.getKeyID())+jwtLifetimeDuration);
    }

    private Optional<OctetKeyPair> getKeyPair(){
        cachedKeyPairs.removeIf(this::isPublicKeyExpired);
        while(cachedKeyPairs.stream().filter(this::hasNotExpired).count()<keyPairCacheSize) {
            cachedKeyPairs.add(generateKeyPair());
        }
        return cachedKeyPairs.stream().filter(this::hasNotExpired).findAny();
    }

    public String getClaimRoles() {
        return claimRoles;
    }
}
