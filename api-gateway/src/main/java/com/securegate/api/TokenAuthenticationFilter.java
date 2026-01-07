package com.securegate.api;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;
import java.security.Principal;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * JWT Authentication Filter - Validates tokens using JWKS from IAM service
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class TokenAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(TokenAuthenticationFilter.class.getName());

    // Paths that don't require authentication
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "test", "health", "jwk", "public");

    @Inject
    private JwksCache jwksCache;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String path = requestContext.getUriInfo().getPath();

        // Allow public endpoints
        for (String publicPath : PUBLIC_PATHS) {
            if (path.contains(publicPath)) {
                return;
            }
        }

        String authorizationHeader = requestContext.getHeaderString("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            LOGGER.fine("Missing or invalid Authorization header for path: " + path);
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"unauthorized\",\"message\":\"Missing or invalid Authorization header\"}")
                            .type("application/json")
                            .build());
            return;
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();

        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify signature using JWKS
            if (!jwksCache.verifySignature(signedJWT)) {
                LOGGER.warning("JWT signature verification failed");
                requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity("{\"error\":\"invalid_token\",\"message\":\"Token signature verification failed\"}")
                                .type("application/json")
                                .build());
                return;
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Check expiration
            Date expiration = claims.getExpirationTime();
            if (expiration != null && expiration.toInstant().isBefore(Instant.now())) {
                LOGGER.fine("JWT expired");
                requestContext.abortWith(
                        Response.status(Response.Status.UNAUTHORIZED)
                                .entity("{\"error\":\"token_expired\",\"message\":\"Token has expired\"}")
                                .type("application/json")
                                .build());
                return;
            }

            // Extract user info from claims
            String subject = claims.getSubject();
            String tenantId = claims.getStringClaim("tenant_id");
            String scope = claims.getStringClaim("scope");
            List<String> groups = claims.getStringListClaim("groups");

            Set<String> roles = new HashSet<>();
            if (groups != null) {
                roles.addAll(groups);
            }

            // Also check 'roles' claim as fallback
            List<String> rolesClaim = claims.getStringListClaim("roles");
            if (rolesClaim != null) {
                roles.addAll(rolesClaim);
            }

            LOGGER.fine("Authenticated user: " + subject + " with roles: " + roles);

            // Set security context
            final String finalSubject = subject;
            final String finalTenantId = tenantId;
            final String finalScope = scope;
            final Set<String> finalRoles = roles;
            final SecurityContext currentSecurityContext = requestContext.getSecurityContext();

            requestContext.setSecurityContext(new SecurityContext() {
                @Override
                public Principal getUserPrincipal() {
                    return () -> finalSubject;
                }

                @Override
                public boolean isUserInRole(String role) {
                    return finalRoles.contains(role);
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

            // Store additional claims in request properties for use by resources
            requestContext.setProperty("tenantId", finalTenantId);
            requestContext.setProperty("scope", finalScope);
            requestContext.setProperty("roles", finalRoles);
            requestContext.setProperty("claims", claims);

        } catch (ParseException e) {
            LOGGER.warning("Failed to parse JWT: " + e.getMessage());
            requestContext.abortWith(
                    Response.status(Response.Status.UNAUTHORIZED)
                            .entity("{\"error\":\"invalid_token\",\"message\":\"Invalid token format\"}")
                            .type("application/json")
                            .build());
        }
    }
}
