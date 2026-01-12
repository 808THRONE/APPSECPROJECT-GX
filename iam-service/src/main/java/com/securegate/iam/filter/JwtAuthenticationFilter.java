package com.securegate.iam.filter;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.securegate.iam.service.KeyManagementService;
import com.securegate.iam.service.RevocationService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.security.Principal;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import jakarta.annotation.PostConstruct;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class.getName());

    @Inject
    private KeyManagementService keyService;

    private String expectedIssuer;
    private static final String EXPECTED_AUDIENCE = "securegate-api";

    @Inject
    private RevocationService revocationService;

    @PostConstruct
    public void init() {
        this.expectedIssuer = getConfigValue("jwt.issuer", "https://iam.securegate.io");
        LOGGER.info("JwtAuthenticationFilter initialized with expected issuer: " + expectedIssuer);
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

    private static final List<String> PUBLIC_PATHS = List.of(
            "/oauth2/authorize",
            "/oauth2/token",
            "/oauth2/register",
            "/oauth2/login",
            "/health",
            "/oauth2/keys"
    );

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        
        // Skip auth for public endpoints
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return;
        }

        String authHeader = requestContext.getHeaderString("Authorization");
        String token = null;

        if (authHeader != null && !authHeader.trim().isEmpty() && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring("Bearer ".length());
        } else {
            // Check cookie
            jakarta.ws.rs.core.Cookie cookie = requestContext.getCookies().get("access_token");
            if (cookie != null) {
                LOGGER.info("Access token cookie found in request");
                token = cookie.getValue();
            } else {
                LOGGER.fine("No access token cookie found in request. Cookies present: " + requestContext.getCookies().keySet());
            }
        }

        if (token == null) {
            return;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // 1. Strict Algorithm Enforcement
            if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())) {
                abortWithUnauthorized(requestContext, "Invalid algorithm - only RS256 allowed");
                return;
            }

            // 2. Strict KID Handling
            String kid = signedJWT.getHeader().getKeyID();
            if (kid == null || !kid.equals(keyService.getKeyId())) {
                abortWithUnauthorized(requestContext, "Invalid or missing Key ID (kid)");
                return;
            }

            // 3. Signature Verification
            JWSVerifier verifier = new com.nimbusds.jose.crypto.RSASSAVerifier(keyService.getPublicKey());
            if (!signedJWT.verify(verifier)) {
                abortWithUnauthorized(requestContext, "Invalid token signature");
                return;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date now = new Date();

            // 4. Expiration & NotBefore (with 30s skew tolerance)
            if (claims.getExpirationTime() == null || now.after(claims.getExpirationTime())) {
                abortWithUnauthorized(requestContext, "Token expired");
                return;
            }
            if (claims.getNotBeforeTime() != null
                    && now.before(new Date(claims.getNotBeforeTime().getTime() - 30000))) {
                abortWithUnauthorized(requestContext, "Token not yet valid (nbf)");
                return;
            }

            // 5. Issuer & Audience Validation
            if (!expectedIssuer.equals(claims.getIssuer())) {
                abortWithUnauthorized(requestContext, "Invalid token issuer: " + claims.getIssuer());
                return;
            }
            if (claims.getAudience() == null || !claims.getAudience().contains(EXPECTED_AUDIENCE)) {
                abortWithUnauthorized(requestContext, "Invalid token audience");
                return;
            }

            // 6. JTI Revocation Check
            String jti = claims.getJWTID();
            if (jti == null) {
                abortWithUnauthorized(requestContext, "Missing token identifier (jti)");
                return;
            }
            if (revocationService.isJtiRevoked(jti)) {
                abortWithUnauthorized(requestContext, "Token has been revoked");
                return;
            }

            // Token is valid
            String subject = signedJWT.getJWTClaimsSet().getSubject(); // UserId
            String username = signedJWT.getJWTClaimsSet().getStringClaim("preferred_username");
            List<String> roles = signedJWT.getJWTClaimsSet().getStringListClaim("roles");

            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();
            requestContext.setSecurityContext(new SecurityContext() {

                @Override
                public Principal getUserPrincipal() {
                    return () -> username != null ? username : subject;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return roles != null && roles.contains(role);
                }

                @Override
                public boolean isSecure() {
                    return currentSecurityContext.isSecure();
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });

        } catch (ParseException | com.nimbusds.jose.JOSEException e) {
            abortWithUnauthorized(requestContext, "Invalid token format");
        }
    }

    private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
        LOGGER.warning("Authentication failed: " + message + " for path: " + requestContext.getUriInfo().getPath());
        requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("{\"error\":\"" + message + "\"}").build());
    }
}
