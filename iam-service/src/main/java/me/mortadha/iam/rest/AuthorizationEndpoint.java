package me.mortadha.iam.rest;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.mortadha.iam.controllers.IamRepository;
import me.mortadha.iam.entities.Grant;
import me.mortadha.iam.entities.Identity;
import me.mortadha.iam.entities.Tenant;
import me.mortadha.iam.security.Argon2Utility;
import me.mortadha.iam.security.AuthorizationCode;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * OAuth 2.1 Authorization Endpoint
 */
@Path("/")
public class AuthorizationEndpoint {

    private static final String COOKIE_NAME = "iam_session";
    private static final Logger LOGGER = Logger.getLogger(AuthorizationEndpoint.class.getName());

    @Inject
    private IamRepository repository;

    /**
     * OAuth 2.1 Authorization Endpoint
     * GET /authorize?client_id=...&redirect_uri=...&response_type=code&code_challenge=...&code_challenge_method=S256
     */
    @GET
    @Path("/authorize")
    @Produces(MediaType.TEXT_HTML)
    public Response authorize(@Context UriInfo uriInfo) {
        MultivaluedMap<String, String> params = uriInfo.getQueryParameters();

        // Validate required parameters
        String clientId = params.getFirst("client_id");
        String responseType = params.getFirst("response_type");
        String codeChallenge = params.getFirst("code_challenge");
        String codeChallengeMethod = params.getFirst("code_challenge_method");
        String redirectUri = params.getFirst("redirect_uri");
        String scope = params.getFirst("scope");
        String state = params.getFirst("state");

        // Validate client_id
        if (clientId == null || clientId.isEmpty()) {
            return errorResponse("Invalid client_id");
        }

        Optional<Tenant> tenantOpt = repository.findTenantById(clientId);
        if (tenantOpt.isEmpty()) {
            return errorResponse("Unknown client_id: " + clientId);
        }

        Tenant tenant = tenantOpt.get();

        // Validate grant type support
        if (!tenant.getSupportedGrantTypes().contains("authorization_code")) {
            return errorResponse("Authorization code flow not supported for this client");
        }

        // Validate redirect_uri
        if (redirectUri == null || redirectUri.isEmpty()) {
            redirectUri = tenant.getRedirectUri();
        } else if (!redirectUri.equals(tenant.getRedirectUri())) {
            return errorResponse("redirect_uri mismatch");
        }

        // Validate response_type
        if (!"code".equals(responseType)) {
            return errorResponse("Only response_type=code is supported");
        }

        // Validate PKCE (mandatory)
        if (!"S256".equals(codeChallengeMethod)) {
            return errorResponse("code_challenge_method must be S256");
        }

        if (codeChallenge == null || codeChallenge.isEmpty()) {
            return errorResponse("code_challenge is required");
        }

        // Use default scope if not provided
        if (scope == null || scope.isEmpty()) {
            scope = tenant.getRequiredScopes();
        }

        // Store session data in cookie
        String sessionData = String.join("#",
            clientId, scope, redirectUri, codeChallenge, state != null ? state : ""
        );

        // Serve login page
        try (InputStream is = getClass().getResourceAsStream("/login.html")) {
            if (is == null) {
                return Response.ok("<html><body><h1>Login</h1>" +
                    "<form action='/login' method='post'>" +
                    "<input name='username' placeholder='Username' required/><br/>" +
                    "<input name='password' type='password' placeholder='Password' required/><br/>" +
                    "<button type='submit'>Login</button>" +
                    "</form></body></html>")
                    .cookie(createSessionCookie(sessionData))
                    .build();
            }

            return Response.ok(is.readAllBytes())
                .type(MediaType.TEXT_HTML)
                .cookie(createSessionCookie(sessionData))
                .build();

        } catch (IOException e) {
            LOGGER.severe("Error loading login page: " + e.getMessage());
            return errorResponse("Internal server error");
        }
    }

    /**
     * Login form submission
     * POST /login
     */
    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(
        @CookieParam(COOKIE_NAME) Cookie sessionCookie,
        @FormParam("username") String username,
        @FormParam("password") String password,
        @Context UriInfo uriInfo
    ) {
        if (sessionCookie == null) {
            return errorResponse("Session expired");
        }

        String[] sessionParts = sessionCookie.getValue().split("#");
        if (sessionParts.length < 4) {
            return errorResponse("Invalid session");
        }

        String clientId = sessionParts[0];
        String scope = sessionParts[1];
        String redirectUri = sessionParts[2];
        String codeChallenge = sessionParts[3];
        String state = sessionParts.length > 4 ? sessionParts[4] : null;

        // Authenticate user
        Optional<Identity> identityOpt = repository.findIdentityByUsername(username);
        if (identityOpt.isEmpty()) {
            LOGGER.warning("Login failed: user not found - " + username);
            return errorResponse("Invalid credentials");
        }

        Identity identity = identityOpt.get();

        if (!Argon2Utility.verifyFromString(identity.getPassword(), password)) {
            LOGGER.warning("Login failed: wrong password - " + username);
            return errorResponse("Invalid credentials");
        }

        if (!identity.getEnabled()) {
            LOGGER.warning("Login failed: account disabled - " + username);
            return errorResponse("Account disabled");
        }

        LOGGER.info("User authenticated: " + username);

        // Check for existing grant (consent)
        Optional<Tenant> tenant = repository.findTenantById(clientId);
        if (tenant.isEmpty()) {
            return errorResponse("Invalid client");
        }

        Optional<Grant> grantOpt = repository.findGrant(tenant.get().getId(), identity.getId());

        if (grantOpt.isPresent()) {
            // User has already consented, issue authorization code
            try {
                String authCode = generateAuthorizationCode(
                    clientId, username, scope, redirectUri, codeChallenge
                );

                UriBuilder redirectBuilder = UriBuilder.fromUri(redirectUri)
                    .queryParam("code", authCode);

                if (state != null && !state.isEmpty()) {
                    redirectBuilder.queryParam("state", state);
                }

                return Response.seeOther(redirectBuilder.build()).build();

            } catch (Exception e) {
                LOGGER.severe("Error generating authorization code: " + e.getMessage());
                return errorResponse("Internal server error");
            }
        } else {
            // Show consent page
            return showConsentPage(clientId, scope);
        }
    }

    /**
     * User consent approval
     * POST /consent
     */
    @POST
    @Path("/consent")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response consent(
        @CookieParam(COOKIE_NAME) Cookie sessionCookie,
        @FormParam("approved_scopes") String approvedScopes,
        @FormParam("approval_status") String approvalStatus
    ) {
        if (sessionCookie == null) {
            return errorResponse("Session expired");
        }

        String[] sessionParts = sessionCookie.getValue().split("#");
        String clientId = sessionParts[0];
        String redirectUri = sessionParts[2];
        String codeChallenge = sessionParts[3];
        String state = sessionParts.length > 4 ? sessionParts[4] : null;

        // Check approval status
        if (!"YES".equals(approvalStatus)) {
            UriBuilder redirectBuilder = UriBuilder.fromUri(redirectUri)
                .queryParam("error", "access_denied")
                .queryParam("error_description", "User denied the request");

            if (state != null) {
                redirectBuilder.queryParam("state", state);
            }

            return Response.seeOther(redirectBuilder.build()).build();
        }

        // TODO: Save grant to database and generate authorization code

        return errorResponse("Consent flow not fully implemented");
    }

    /**
     * Generate encrypted authorization code
     */
    private String generateAuthorizationCode(String clientId, String username,
                                             String scope, String redirectUri,
                                             String codeChallenge) throws Exception {
        long expiration = Instant.now().plus(2, ChronoUnit.MINUTES).getEpochSecond();

        AuthorizationCode authCode = new AuthorizationCode(
            clientId, username, scope, expiration, redirectUri
        );

        return authCode.encode(codeChallenge);
    }

    /**
     * Show consent page
     */
    private Response showConsentPage(String clientId, String scope) {
        String html = "<html><body>" +
            "<h1>Authorization Request</h1>" +
            "<p>Application <strong>" + clientId + "</strong> requests access to:</p>" +
            "<ul><li>" + scope + "</li></ul>" +
            "<form action='/consent' method='post'>" +
            "<input type='hidden' name='approved_scopes' value='" + scope + "'/>" +
            "<button type='submit' name='approval_status' value='YES'>Allow</button>" +
            "<button type='submit' name='approval_status' value='NO'>Deny</button>" +
            "</form></body></html>";

        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    /**
     * Create session cookie
     */
    private NewCookie createSessionCookie(String value) {
        return new NewCookie.Builder(COOKIE_NAME)
            .value(value)
            .path("/")
            .httpOnly(true)
            .secure(true) // HTTPS only in production
            .sameSite(NewCookie.SameSite.STRICT)
            .maxAge(600) // 10 minutes
            .build();
    }

    /**
     * Error response
     */
    private Response errorResponse(String message) {
        String html = "<html><body><h1>Error</h1><p>" + message + "</p></body></html>";
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(html)
            .type(MediaType.TEXT_HTML)
            .build();
    }
}
