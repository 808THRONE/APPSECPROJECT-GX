package me.mortadha.iam.rest;

import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.mortadha.iam.controllers.IamRepository;
import me.mortadha.iam.security.AuthorizationCode;
import me.mortadha.iam.security.JwtManager;

import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * OAuth 2.1 Token Endpoint
 */
@Path("/token")
public class TokenEndpoint {

    private static final Logger LOGGER = Logger.getLogger(TokenEndpoint.class.getName());

    @Inject
    private IamRepository repository;

    @EJB
    private JwtManager jwtManager;

    /**
     * OAuth 2.1 Token Exchange
     * POST /token
     */
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(
        @FormParam("grant_type") String grantType,
        @FormParam("code") String code,
        @FormParam("code_verifier") String codeVerifier,
        @FormParam("redirect_uri") String redirectUri
    ) {
        LOGGER.info("Token request: grant_type=" + grantType);

        // Validate grant_type
        if (grantType == null || grantType.isEmpty()) {
            return errorResponse("invalid_request", "grant_type is required");
        }

        if ("authorization_code".equals(grantType)) {
            return handleAuthorizationCodeGrant(code, codeVerifier, redirectUri);
        } else if ("refresh_token".equals(grantType)) {
            return errorResponse("unsupported_grant_type", "Refresh token not yet implemented");
        } else {
            return errorResponse("unsupported_grant_type", 
                "Only authorization_code and refresh_token are supported");
        }
    }

    /**
     * Handle authorization_code grant type
     */
    private Response handleAuthorizationCodeGrant(String code, String codeVerifier, String redirectUri) {
        // Validate parameters
        if (code == null || code.isEmpty()) {
            return errorResponse("invalid_request", "code is required");
        }

        if (codeVerifier == null || codeVerifier.isEmpty()) {
            return errorResponse("invalid_request", "code_verifier is required (PKCE)");
        }

        try {
            // Decode and verify authorization code with PKCE
            AuthorizationCode authCode = AuthorizationCode.decode(code, codeVerifier);

            // Check expiration
            if (authCode.isExpired()) {
                LOGGER.warning("Authorization code expired");
                return errorResponse("invalid_grant", "Authorization code expired");
            }

            // Validate redirect_uri
            if (redirectUri != null && !redirectUri.equals(authCode.redirectUri())) {
                LOGGER.warning("redirect_uri mismatch");
                return errorResponse("invalid_grant", "redirect_uri mismatch");
            }

            LOGGER.info("Authorization code validated for user: " + authCode.username());

            // Get user roles
            String[] roles = repository.getRolesForIdentity(authCode.username());

            // Generate access token
            String accessToken = jwtManager.generateAccessToken(
                authCode.tenantId(),
                authCode.username(),
                authCode.approvedScopes(),
                roles
            );

            // Generate refresh token
            String refreshToken = jwtManager.generateRefreshToken(
                authCode.tenantId(),
                authCode.username(),
                authCode.approvedScopes()
            );

            LOGGER.info("Tokens issued for user: " + authCode.username());

            // Return tokens
            return Response.ok(Json.createObjectBuilder()
                .add("token_type", "Bearer")
                .add("access_token", accessToken)
                .add("expires_in", 1020) // From config
                .add("scope", authCode.approvedScopes())
                .add("refresh_token", refreshToken)
                .build())
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .build();

        } catch (GeneralSecurityException e) {
            LOGGER.severe("PKCE verification failed: " + e.getMessage());
            return errorResponse("invalid_grant", "Invalid authorization code or verifier");
        } catch (Exception e) {
            LOGGER.severe("Token exchange error: " + e.getMessage());
            return errorResponse("server_error", "Internal server error");
        }
    }

    /**
     * Create OAuth 2.1 error response
     */
    private Response errorResponse(String error, String description) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(Json.createObjectBuilder()
                .add("error", error)
                .add("error_description", description)
                .build())
            .build();
    }
}
