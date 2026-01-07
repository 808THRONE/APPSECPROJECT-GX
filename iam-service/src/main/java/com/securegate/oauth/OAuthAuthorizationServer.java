package com.securegate.oauth;

import com.securegate.entities.Grant;
import com.securegate.entities.Identity;
import com.securegate.entities.Tenant;
import com.securegate.repositories.GrantRepository;
import com.securegate.repositories.IdentityRepository;
import com.securegate.repositories.TenantRepository;
import com.securegate.tokens.PasetoService;
import com.securegate.auth.SessionService;
import jakarta.ws.rs.CookieParam;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.Json;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Path("/oauth")
public class OAuthAuthorizationServer {

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private GrantRepository grantRepository;

    @Inject
    private IdentityRepository identityRepository;

    @Inject
    private SessionService sessionService;

    @GET
    @Path("/authorize")
    public Response authorize(
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("state") String state,
            @QueryParam("response_type") String responseType,
            @QueryParam("scope") String scope,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod,
            @CookieParam("SESSION_ID") String sessionId) {

        // 1. Validate Client
        if (clientId == null || !tenantRepository.findByClientId(clientId).isPresent()) {
            return errorResponse("invalid_client", "Unknown client_id");
        }

        // 2. Validate Redirect URI
        if (redirectUri == null || !tenantRepository.validateRedirectUri(clientId, redirectUri)) {
            return errorResponse("invalid_request", "Invalid redirect_uri");
        }

        // 3. Authenticate User via Session
        String username = null;
        if (sessionId != null) {
            username = sessionService.getUsername(sessionId).orElse(null);
        }

        if (username == null) {
            // User is not authenticated.
            // In a full OAuth server, we would redirect to a login page hosted by IAM.
            // For this PWA/SPA hybrid, we can redirect back to the frontend login page with
            // error,
            // OR return 401. Redirecting is safer for the flow.
            // Assuming frontend runs on localhost:5173 for now or we use a configured login
            // URL.
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(Json.createObjectBuilder().add("error", "login_required").build())
                    .build();
        }

        Identity user = identityRepository.findByUsername(username).orElseThrow();

        // 4. Create Authorization Code
        String code = UUID.randomUUID().toString();
        Grant grant = new Grant(
                code,
                user.getId(),
                clientId,
                Instant.now().plus(10, ChronoUnit.MINUTES), // 10 min validity
                redirectUri,
                null, // nonce not used yet
                codeChallenge,
                codeChallengeMethod);
        grantRepository.save(grant);

        // it now redirect directelly
        String redirect = redirectUri +
                "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8) +
                (state != null ? "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8) : "");

        return Response.seeOther(URI.create(redirect)).build();
    }

    @POST
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(
            @FormParam("grant_type") String grantType,
            @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @FormParam("code_verifier") String codeVerifier) {

        // 1. Validate Grant Type
        if (!"authorization_code".equals(grantType)) {
            return errorResponse("unsupported_grant_type", "Only authorization_code is supported");
        }

        // 2. Validate Client
        Optional<Tenant> tenantOpt = tenantRepository.findByClientId(clientId);
        if (tenantOpt.isEmpty()) {
            return errorResponse("invalid_client", "Unknown client");
        }
        Tenant tenant = tenantOpt.get();

        // Client Authentication: Secret OR PKCE
        boolean isClientAuthenticated = false;

        // Check Secret if provided
        if (clientSecret != null && !clientSecret.isEmpty()) {
            if (tenant.getClientSecret().equals(clientSecret)) {
                isClientAuthenticated = true;
            }
        } else {
            // No secret provided, check if client allows public access (for now assume yes
            // if PKCE is used)
            // In strict mode we'd check tenant.isPublic()
            // For PWA demo we rely on PKCE
            isClientAuthenticated = true;
        }

        if (!isClientAuthenticated) {
            return errorResponse("invalid_client", "Invalid client credentials");
        }

        // 3. Find and Consume Code
        Optional<Grant> grantOpt = grantRepository.consume(code); // Consume immediately (one-time use)
        if (grantOpt.isEmpty()) {
            return errorResponse("invalid_grant", "Invalid or expired code");
        }
        Grant grant = grantOpt.get();

        // 4. Validate Code Bindings
        if (!grant.getClientId().equals(clientId)) {
            return errorResponse("invalid_grant", "Code was issued to another client");
        }
        if (Instant.now().isAfter(grant.getExpiresAt())) {
            return errorResponse("invalid_grant", "Code expired");
        }
        if (redirectUri != null && !redirectUri.equals(grant.getRedirectUri())) {
            return errorResponse("invalid_grant", "Redirect URI mismatch");
        }

        // 5. PKCE Validation
        if (grant.getCodeChallenge() != null) {
            if (codeVerifier == null) {
                return errorResponse("invalid_request", "Code verifier missing");
            }
            if (!validatePKCE(codeVerifier, grant.getCodeChallenge())) {
                return errorResponse("invalid_grant", "PKCE verification failed");
            }
        }

        String accessToken = PasetoService.createAccessToken(grant.getIdentityId(), "admin", "openid");

        String idToken = generateMockIdToken("admin", "admin@securegate.com");

        return Response.ok(Json.createObjectBuilder()
                .add("access_token", accessToken)
                .add("id_token", idToken)
                .add("token_type", "Bearer")
                .add("expires_in", 3600)
                .build()).build();
    }

    // Simple Mock JWT generator (Header.Payload.Signature)
    private String generateMockIdToken(String name, String email) {
        String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payload = String.format("{\"sub\":\"1\",\"name\":\"%s\",\"email\":\"%s\",\"exp\":%d}",
                name, email, System.currentTimeMillis() / 1000 + 3600);

        String b64Header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(header.getBytes(StandardCharsets.UTF_8));
        String b64Payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        String signature = "mock-signature"; // Frontend doesn't verify signature in this demo

        return b64Header + "." + b64Payload + "." + signature;
    }

    private boolean validatePKCE(String verifier, String challenge) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(verifier.getBytes(StandardCharsets.US_ASCII));
            String calculatedChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return calculatedChallenge.equals(challenge);
        } catch (Exception e) {
            return false;
        }
    }

    private Response errorResponse(String error, String description) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(Json.createObjectBuilder()
                        .add("error", error)
                        .add("error_description", description)
                        .build())
                .build();
    }
}
