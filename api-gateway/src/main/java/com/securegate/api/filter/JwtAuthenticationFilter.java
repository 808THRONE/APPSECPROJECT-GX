package com.securegate.api.filter;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
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
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtAuthenticationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(JwtAuthenticationFilter.class.getName());
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private RSAPublicKey publicKey;

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

            // For RS256 we need the public key.
            // In this version, we expect it in the environment if we want to verify.
            // If not found, we fallback to a warning (or rejection in prod).
            if (publicKey == null) {
                String pubKeyEnv = System.getenv("JWT_PUBLIC_KEY");
                if (pubKeyEnv != null) {
                    publicKey = parsePublicKey(pubKeyEnv);
                }
            }

            if (publicKey != null) {
                JWSVerifier verifier = new RSASSAVerifier(publicKey);
                if (!signedJWT.verify(verifier)) {
                    requestContext.abortWith(
                            Response.status(Response.Status.UNAUTHORIZED).entity("Invalid RS256 signature").build());
                    return;
                }
            } else {
                LOGGER.warning("Missing JWT_PUBLIC_KEY in Gateway. Skipping signature verification (INSECURE)");
            }

            // Standard Claim Checks
            Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
            if (exp != null && new Date().after(exp)) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Token expired").build());
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

        } catch (Exception e) {
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).entity("Invalid token").build());
        }
    }

    private RSAPublicKey parsePublicKey(String base64) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(base64);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(decoded));
    }
}
