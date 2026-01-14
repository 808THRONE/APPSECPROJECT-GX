package com.securegate.api.filter;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.Ed25519Verifier;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import com.securegate.api.service.RevocationService;
import jakarta.inject.Inject;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class.getName());
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private OctetKeyPair publicKeyJwk;

    @Inject
    private RevocationService revocationService;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String authHeader = requestContext.getHeaderString(AUTHORIZATION_HEADER);
        String token = null;

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            token = authHeader.substring(BEARER_PREFIX.length());
        } else {
            jakarta.ws.rs.core.Cookie cookie = requestContext.getCookies().get("access_token");
            if (cookie != null) {
                token = cookie.getValue();
            }
        }

        if (token == null) {
            return;
        }

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            if (!JWSAlgorithm.EdDSA.equals(signedJWT.getHeader().getAlgorithm())) {
                requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity("{\"error\":\"Invalid algorithm - only EdDSA allowed\"}").build());
                return;
            }

            if (publicKeyJwk == null) {
                // Try environment variable first
                String pubKeyEnv = System.getenv("JWT_PUBLIC_KEY");
                if (pubKeyEnv != null && !pubKeyEnv.isEmpty()) {
                    publicKeyJwk = parseEd25519PublicKey(pubKeyEnv);
                } else {
                    // Fetch from IAM JWKS endpoint
                    publicKeyJwk = fetchJwksFromIam();
                }
            }

            if (publicKeyJwk != null) {
                JWSVerifier verifier = new Ed25519Verifier(publicKeyJwk);
                if (!signedJWT.verify(verifier)) {
                    // Verification failed. Token might be signed with a new key (rotation).
                    // Force refresh of the public key and try once more.
                    LOGGER.warning("Signature verification failed. Attempting to refresh public key from IAM...");

                    // Force re-fetch logic
                    String pubKeyEnv = System.getenv("JWT_PUBLIC_KEY");
                    if (pubKeyEnv != null && !pubKeyEnv.isEmpty()) {
                        publicKeyJwk = parseEd25519PublicKey(pubKeyEnv);
                    } else {
                        publicKeyJwk = fetchJwksFromIam();
                    }

                    if (publicKeyJwk != null) {
                        verifier = new Ed25519Verifier(publicKeyJwk);
                        if (!signedJWT.verify(verifier)) {
                            LOGGER.warning(
                                    "Authentication failed: Invalid token signature (after key refresh) for path: "
                                            + requestContext.getUriInfo().getPath());
                            requestContext.abortWith(
                                    Response.status(Response.Status.UNAUTHORIZED)
                                            .entity("{\"error\":\"Invalid token signature\"}").build());
                            return;
                        } else {
                            LOGGER.info("Signature verification successful after key refresh.");
                        }
                    } else {
                        // Key fetch failed during refresh
                        LOGGER.warning("Authentication failed: Could not refresh key.");
                        requestContext.abortWith(
                                Response.status(Response.Status.UNAUTHORIZED)
                                        .entity("{\"error\":\"Invalid token signature\"}").build());
                        return;
                    }
                }
            } else {
                LOGGER.severe("Unable to obtain JWT public key. Blocking request.");
                requestContext.abortWith(
                        Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                .entity("{\"error\":\"Identity verification unavailable\"}").build());
                return;
            }

            // Standard Claim Checks
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp != null && new Date().after(exp)) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Token expired").build());
                return;
            }

            // Revocation Check (JTI)
            String jti = signedJWT.getJWTClaimsSet().getJWTID();
            if (jti != null && revocationService.isJtiRevoked(jti)) {
                requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"Token has been revoked\"}")
                                .build());
                return;
            }

            // Extract User Info
            String username = signedJWT.getJWTClaimsSet().getStringClaim("preferred_username");
            List<String> roles = signedJWT.getJWTClaimsSet().getStringListClaim("roles");
            if (roles == null)
                roles = Collections.emptyList();

            final List<String> finalRoles = roles;
            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> username;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return finalRoles.contains(role);
                }

                @Override
                public boolean isSecure() {
                    return requestContext.getUriInfo().getRequestUri().getScheme().equals("https");
                }

                @Override
                public String getAuthenticationScheme() {
                    return "Bearer";
                }
            });

            // ... end of filter method
        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token").build());
        }
    }

    private OctetKeyPair parseEd25519PublicKey(String base64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64);

        // Standard X.509/Ed25519 public key: last 32 bytes are the raw public key
        byte[] x = java.util.Arrays.copyOfRange(decoded, decoded.length - 32, decoded.length);

        return new OctetKeyPair.Builder(com.nimbusds.jose.jwk.Curve.Ed25519, com.nimbusds.jose.util.Base64URL.encode(x))
                .build();
    }

    private OctetKeyPair fetchJwksFromIam() {
        try {
            String iamUrl = System.getProperty("iam.service.url", "http://localhost:8080/iam-service/api");
            String jwksUrl = iamUrl + "/oauth2/jwks.json";

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(jwksUrl))
                    .GET()
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                com.nimbusds.jose.jwk.JWKSet jwkSet = com.nimbusds.jose.jwk.JWKSet.parse(response.body());
                if (!jwkSet.getKeys().isEmpty()) {
                    com.nimbusds.jose.jwk.JWK jwk = jwkSet.getKeys().get(0);
                    if (jwk instanceof OctetKeyPair) {
                        LOGGER.info("Successfully fetched Ed25519 public key from IAM JWKS endpoint");
                        return (OctetKeyPair) jwk;
                    }
                }
            }
            LOGGER.warning("Failed to fetch JWKS from IAM: HTTP " + response.statusCode());
        } catch (Exception e) {
            LOGGER.severe("Error fetching JWKS from IAM: " + e.getMessage());
        }
        return null;
    }
}
