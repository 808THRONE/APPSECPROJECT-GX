package com.securegate.iam.oauth;

import com.securegate.iam.model.User;
import com.securegate.iam.model.Role;
import com.securegate.iam.repository.UserRepository;
import com.securegate.iam.service.CryptoService;
import com.securegate.iam.service.TokenService;
import com.securegate.iam.service.PasswordPolicyValidator;
import com.securegate.iam.service.TotpService;
import com.securegate.iam.service.KeyManagementService;
import com.securegate.iam.service.RevocationService;
import com.securegate.iam.service.RefreshTokenService;
import com.securegate.iam.service.SanitizationService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.util.Map;
import java.util.UUID;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.Collections;

@Path("/oauth2")
public class OAuth2Resource {

    private static final Logger LOGGER = Logger.getLogger(OAuth2Resource.class.getName());

    private static final Map<String, AuthContext> codeStore = new ConcurrentHashMap<>();
    private static final Map<String, String> mfaStore = new ConcurrentHashMap<>(); // sessionId -> userId

    @Inject
    private TokenService tokenService;

    @Inject
    private UserRepository userRepository;

    @Inject
    private CryptoService cryptoService;

    @Inject
    private PasswordPolicyValidator passwordValidator;

    @Inject
    private TotpService totpService;

    @Inject
    private KeyManagementService keyService;

    @Inject
    private RevocationService revocationService;

    @Inject
    private RefreshTokenService refreshTokenService;

    @Inject
    private SanitizationService sanitizationService;

    @Context
    private UriInfo uriInfo;

    @GET
    @Path("/jwks.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJwks() {
        // Extract the raw 32-byte public key from X.509 encoding (last 32 bytes)
        byte[] encoded = keyService.getPublicKey().getEncoded();
        byte[] x = java.util.Arrays.copyOfRange(encoded, encoded.length - 32, encoded.length);

        com.nimbusds.jose.jwk.OctetKeyPair okp = new com.nimbusds.jose.jwk.OctetKeyPair.Builder(
                com.nimbusds.jose.jwk.Curve.Ed25519,
                com.nimbusds.jose.util.Base64URL.encode(x))
                .keyID(keyService.getKeyId())
                .build();

        JWKSet jwkSet = new JWKSet(Collections.singletonList(okp));
        return Response.ok(jwkSet.toJSONObject()).build();
    }

    static class AuthContext {
        String clientId;
        String redirectUri;
        String codeChallenge;
        String userId;
        String nonce;

        public AuthContext(String clientId, String redirectUri, String codeChallenge) {
            this.clientId = clientId;
            this.redirectUri = redirectUri;
            this.codeChallenge = codeChallenge;
        }
    }

    @GET
    @Path("/authorize")
    public Response authorize(
            @QueryParam("response_type") String responseType,
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("scope") String scope,
            @QueryParam("state") String state,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod) {

        if (!"code".equals(responseType) || codeChallenge == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid request").build();
        }

        String username = uriInfo.getQueryParameters().getFirst("username");
        String password = uriInfo.getQueryParameters().getFirst("password");
        String mfaCode = uriInfo.getQueryParameters().getFirst("mfa_code");
        String setupAction = uriInfo.getQueryParameters().getFirst("setup_mfa");
        String mfaSessionId = uriInfo.getQueryParameters().getFirst("mfa_session_id");

        User user = null;

        // Step 1: Resolve User (either via Session or Credentials)
        if (mfaSessionId != null && !mfaSessionId.isEmpty()) {
            String userId = mfaStore.get(mfaSessionId);
            if (userId != null) {
                user = userRepository.findById(UUID.fromString(userId)).orElse(null);
            }
        }

        if (user == null) {
            if (username == null || password == null) {
                return renderLoginForm(responseType, clientId, redirectUri, scope, state, codeChallenge,
                        codeChallengeMethod, null);
            }

            // Verify credentials
            user = userRepository.findByUsername(username).orElse(null);
            if (user == null || !cryptoService.verifyPassword(password, user.getPasswordHash())) {
                return renderLoginForm(responseType, clientId, redirectUri, scope, state, codeChallenge,
                        codeChallengeMethod, "Invalid credentials");
            }

            // Valid credentials, create MFA session
            mfaSessionId = UUID.randomUUID().toString();
            mfaStore.put(mfaSessionId, user.getUserId().toString());
        }

        // MFA CHECK - Mandatory per user request
        String secret = user.getMfaSecretEnc();

        // Case 1: MFA set up but no code provided yet
        if (secret != null && !secret.isEmpty()) {
            if (mfaCode == null || mfaCode.isEmpty()) {
                return renderMfaVerify(responseType, clientId, redirectUri, scope, state, codeChallenge,
                        codeChallengeMethod, user.getUsername(), mfaSessionId, null);
            }

            try {
                int code = Integer.parseInt(mfaCode);
                if (!totpService.verifyCode(secret, code)) {
                    return renderMfaVerify(responseType, clientId, redirectUri, scope, state, codeChallenge,
                            codeChallengeMethod, user.getUsername(), mfaSessionId, "Invalid 6-digit code");
                }
            } catch (NumberFormatException e) {
                return renderMfaVerify(responseType, clientId, redirectUri, scope, state, codeChallenge,
                        codeChallengeMethod, user.getUsername(), mfaSessionId, "Code must be numbers");
            }
        }
        // Case 2: MFA not set up yet (MFA is mandatory)
        else {
            if ("confirm".equals(setupAction) && mfaCode != null) {
                String tempSecret = uriInfo.getQueryParameters().getFirst("temp_secret");
                try {
                    int code = Integer.parseInt(mfaCode);
                    if (totpService.verifyCode(tempSecret, code)) {
                        user.setMfaSecretEnc(tempSecret);
                        user.setMfaEnabled(true);
                        userRepository.save(user);
                        // Success, proceed to issue code
                    } else {
                        return renderMfaSetup(user, tempSecret, responseType, clientId, redirectUri, scope, state,
                                codeChallenge, codeChallengeMethod, mfaSessionId, "Invalid Code");
                    }
                } catch (NumberFormatException e) {
                    return renderMfaSetup(user, tempSecret, responseType, clientId, redirectUri, scope, state,
                            codeChallenge, codeChallengeMethod, mfaSessionId, "Code must be numbers");
                }
            } else {
                // Generate a new secret and show setup page
                String newSecret = (setupAction != null) ? uriInfo.getQueryParameters().getFirst("temp_secret")
                        : totpService.generateSecret();
                return renderMfaSetup(user, newSecret, responseType, clientId, redirectUri, scope, state,
                        codeChallenge, codeChallengeMethod, mfaSessionId, null);
            }
        }

        // Clean up MFA session before issuing auth code
        mfaStore.remove(mfaSessionId);

        // Issue auth code
        String code = UUID.randomUUID().toString();
        AuthContext ctx = new AuthContext(clientId, redirectUri, codeChallenge);
        ctx.userId = user.getUserId().toString();
        codeStore.put(code, ctx);

        UriBuilder uriBuilder = UriBuilder.fromUri(redirectUri)
                .queryParam("code", code)
                .queryParam("state", state);

        return Response.seeOther(uriBuilder.build()).build();
    }

    // ... (Token, Refresh, Logout, Userinfo, RegisterPage methods unchanged)

    @POST
    @Path("/token")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(
            @FormParam("grant_type") String grantType,
            @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("client_id") String clientId,
            @FormParam("code_verifier") String codeVerifier) {

        if (!"authorization_code".equals(grantType)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"unsupported_grant_type\"}")
                    .build();
        }

        AuthContext ctx = codeStore.remove(code);
        if (ctx == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"invalid_grant\"}").build();
        }

        User user = null;
        if (ctx.userId != null) {
            user = userRepository.findById(UUID.fromString(ctx.userId)).orElse(null);
        }

        if (user == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"user_not_found\"}").build();
        }

        String accessToken = tokenService.generateAccessToken(user, clientId, "unknown_device");
        String refreshToken = refreshTokenService.createRefreshToken(user.getUserId().toString());
        String csrfToken = UUID.randomUUID().toString();

        NewCookie accessCookie = new NewCookie.Builder("access_token")
                .value(accessToken)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(900)
                .build();

        NewCookie refreshCookie = new NewCookie.Builder("refresh_token")
                .value(refreshToken)
                .path("/iam-service/api/oauth2/refresh")
                .httpOnly(true)
                .secure(false)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(7 * 24 * 3600)
                .build();

        NewCookie csrfCookie = new NewCookie.Builder("XSRF-TOKEN")
                .value(csrfToken)
                .path("/")
                .httpOnly(false)
                .secure(false)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(900)
                .build();

        // Create response map for secure serialization
        Map<String, Object> responseMap = new java.util.HashMap<>();
        responseMap.put("status", "success");
        responseMap.put("username", sanitizationService.sanitize(user.getUsername()));
        responseMap.put("userId", user.getUserId());
        responseMap.put("mfaEnabled", user.isMfaEnabled());
        responseMap.put("roles",
                user.getRoles().stream().map(Role::getRoleName).collect(java.util.stream.Collectors.toList()));

        return Response.ok(responseMap)
                .cookie(accessCookie, refreshCookie, csrfCookie)
                .header("X-CSRF-TOKEN", csrfToken)
                .build();
    }

    @POST
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@Context HttpHeaders headers) {
        Cookie cookie = headers.getCookies().get("refresh_token");
        if (cookie == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"missing_refresh_token\"}")
                    .build();
        }
        String oldToken = cookie.getValue();
        String userId = refreshTokenService.verifyAndGetUserId(oldToken);
        if (userId == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\":\"invalid_refresh_token\"}")
                    .build();
        }
        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String newToken = refreshTokenService.rotateToken(oldToken);
        String newAccess = tokenService.generateAccessToken(user, "pwa-client", "unknown_device");

        NewCookie accessCookie = new NewCookie.Builder("access_token")
                .value(newAccess)
                .path("/")
                .httpOnly(true)
                .secure(false)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(900)
                .build();

        NewCookie refreshCookie = new NewCookie.Builder("refresh_token")
                .value(newToken)
                .path("/iam-service/api/oauth2/refresh")
                .httpOnly(true)
                .secure(false)
                .sameSite(NewCookie.SameSite.LAX)
                .maxAge(7 * 24 * 3600)
                .build();

        return Response.ok("{\"status\":\"success\"}")
                .cookie(accessCookie, refreshCookie)
                .build();
    }

    @POST
    @Path("/logout")
    public Response logout(@Context HttpHeaders headers) {
        Cookie cookie = headers.getCookies().get("access_token");
        if (cookie != null) {
            String token = cookie.getValue();
            try {
                SignedJWT signedJWT = SignedJWT.parse(token);
                String jti = signedJWT.getJWTClaimsSet().getJWTID();
                Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
                if (jti != null && exp != null) {
                    long ttl = (exp.getTime() - System.currentTimeMillis()) / 1000;
                    if (ttl > 0) {
                        revocationService.revokeJti(jti, ttl);
                    }
                }
            } catch (Exception e) {
                LOGGER.warning("Failed to parse token for revocation: " + e.getMessage());
            }
        }
        NewCookie clearAccess = new NewCookie.Builder("access_token").value("").path("/").maxAge(0).httpOnly(true)
                .build();
        NewCookie clearCsrf = new NewCookie.Builder("XSRF-TOKEN").value("").path("/").maxAge(0).build();
        NewCookie clearRefresh = new NewCookie.Builder("refresh_token").value("")
                .path("/iam-service/api/oauth2/refresh").maxAge(0).build();
        return Response.ok("{\"message\":\"Logged out\"}").cookie(clearAccess, clearRefresh, clearCsrf).build();
    }

    @GET
    @Path("/userinfo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response userinfo(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String username = securityContext.getUserPrincipal().getName();
        return Response.ok("{\"username\":\"" + username + "\"}").build();
    }

    @GET
    @Path("/register")
    public Response registerPage(
            @QueryParam("response_type") String responseType,
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("scope") String scope,
            @QueryParam("state") String state,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod) {
        return Response
                .ok(getRegisterHtml(responseType, clientId, redirectUri, scope, state, codeChallenge,
                        codeChallengeMethod))
                .type(MediaType.TEXT_HTML).build();
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response register(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("email") String email,
            @FormParam("response_type") String responseType,
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("scope") String scope,
            @FormParam("state") String state,
            @FormParam("code_challenge") String codeChallenge,
            @FormParam("code_challenge_method") String codeChallengeMethod) {
        try {
            passwordValidator.validate(password);
        } catch (Exception e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid Password: " + e.getMessage()).build();
        }
        if (userRepository.findByUsername(username).isPresent()) {
            return Response.status(Response.Status.CONFLICT).entity("Username already exists").build();
        }
        User u = new User();
        u.setUsername(sanitizationService.sanitize(username));
        u.setEmail(sanitizationService.sanitize(email));
        u.setPasswordHash(cryptoService.hashPassword(password));
        u.setStatus("ACTIVE");
        userRepository.save(u);

        String baseUrl = getBaseUrl();

        UriBuilder uriBuilder = UriBuilder.fromUri(baseUrl + "/iam-service/api/oauth2/authorize")
                .queryParam("response_type", responseType)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", codeChallengeMethod)
                .queryParam("username", username)
                .queryParam("password", password);
        return Response.seeOther(uriBuilder.build()).build();
    }

    private String getBaseUrl() {
        String baseUrl = System.getProperty("PUBLIC_BASE_URL");
        if (baseUrl == null)
            baseUrl = System.getenv("PUBLIC_BASE_URL");
        if (baseUrl == null)
            baseUrl = "https://mortadha.me";
        return baseUrl;
    }

    private Response renderLoginForm(String rt, String cid, String ruri, String scp, String st, String cc, String ccm,
            String error) {
        String baseUrl = getBaseUrl();
        String errHtml = error != null
                ? "<div style='background:#fee2e2; border:1px solid #ef4444; color:#b91c1c; padding:0.75rem; border-radius:0.5rem; margin-bottom:1.5rem; text-align:center;'>"
                        + error + "</div>"
                : "";
        String html = "<html><body style='background:#0f172a; color:white; font-family:sans-serif; display:flex; align-items:center; justify-content:center; min-height:100vh; margin:0;'>"
                + "<div style='background:#1e293b; padding:2rem; border-radius:1rem; border:1px solid #334155; width:350px; box-shadow:0 10px 15px -3px rgba(0,0,0,0.1);'>"
                + "<h2 style='text-align:center; color:#3b82f6; margin-bottom:1.5rem;'>SecureGate Login</h2>"
                + errHtml
                + "<form method='GET' action='" + baseUrl + "/iam-service/api/oauth2/authorize'>"
                + "<input type='hidden' name='response_type' value='" + sanitizationService.sanitize(rt) + "'>"
                + "<input type='hidden' name='client_id' value='" + sanitizationService.sanitize(cid) + "'>"
                + "<input type='hidden' name='redirect_uri' value='" + sanitizationService.sanitize(ruri) + "'>"
                + "<input type='hidden' name='scope' value='" + sanitizationService.sanitize(scp) + "'>"
                + "<input type='hidden' name='state' value='" + sanitizationService.sanitize(st) + "'>"
                + "<input type='hidden' name='code_challenge' value='" + sanitizationService.sanitize(cc) + "'>"
                + "<input type='hidden' name='code_challenge_method' value='" + sanitizationService.sanitize(ccm) + "'>"
                + "<div style='margin-bottom:1rem;'><label style='display:block; margin-bottom:0.5rem; color:#94a3b8;'>Username</label><input name='username' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; outline:none; box-sizing:border-box;'></div>"
                + "<div style='margin-bottom:1.5rem;'><label style='display:block; margin-bottom:0.5rem; color:#94a3b8;'>Password</label><input type='password' name='password' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; outline:none; box-sizing:border-box;'></div>"
                + "<button type='submit' style='width:100%; padding:0.75rem; background:linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border:none; color:white; border-radius:0.5rem; cursor:pointer; font-weight:bold;'>Sign In</button>"
                + "</form>"
                + "<p style='text-align:center; margin-top:1.5rem; color:#94a3b8;'>Need account? <a href='/iam-service/api/oauth2/register?response_type="
                + sanitizationService.sanitize(rt) + "&client_id=" + sanitizationService.sanitize(cid)
                + "&redirect_uri=" + sanitizationService.sanitize(ruri) + "&scope="
                + sanitizationService.sanitize(scp) + "&state=" + sanitizationService.sanitize(st) + "&code_challenge="
                + sanitizationService.sanitize(cc) + "&code_challenge_method="
                + sanitizationService.sanitize(ccm) + "' style='color:#3b82f6;'>Sign Up</a></p>"
                + "</div></body></html>";
        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    private Response renderMfaVerify(String rt, String cid, String ruri, String scp, String st, String cc, String ccm,
            String username, String mfaSessionId, String error) {
        String errHtml = error != null
                ? "<div style='background:#fee2e2; border:1px solid #ef4444; color:#b91c1c; padding:0.75rem; border-radius:0.5rem; margin-bottom:1.5rem; text-align:center;'>"
                        + error + "</div>"
                : "";
        String html = "<html><body style='background:#0f172a; color:white; font-family:sans-serif; display:flex; align-items:center; justify-content:center; min-height:100vh; margin:0;'>"
                + "<div style='background:#1e293b; padding:2rem; border-radius:1rem; border:1px solid #334155; width:350px;'>"
                + "<h2 style='text-align:center; color:#3b82f6;'>MFA Verification</h2>"
                + "<p style='text-align:center; color:#94a3b8;'>User: " + username + "</p>"
                + errHtml
                + "<form method='GET' action='" + getBaseUrl() + "/iam-service/api/oauth2/authorize'>"
                + "<input type='hidden' name='response_type' value='" + sanitizationService.sanitize(rt) + "'>"
                + "<input type='hidden' name='client_id' value='" + sanitizationService.sanitize(cid) + "'>"
                + "<input type='hidden' name='redirect_uri' value='" + sanitizationService.sanitize(ruri) + "'>"
                + "<input type='hidden' name='scope' value='" + sanitizationService.sanitize(scp) + "'>"
                + "<input type='hidden' name='state' value='" + sanitizationService.sanitize(st) + "'>"
                + "<input type='hidden' name='code_challenge' value='" + sanitizationService.sanitize(cc) + "'>"
                + "<input type='hidden' name='code_challenge_method' value='" + sanitizationService.sanitize(ccm) + "'>"
                + "<input type='hidden' name='mfa_session_id' value='" + sanitizationService.sanitize(mfaSessionId)
                + "'>"
                + "<div style='margin-bottom:1.5rem;'><input name='mfa_code' placeholder='000000' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; text-align:center; font-size:1.5rem; letter-spacing:0.5rem;' maxlength='6' autofocus></div>"
                + "<button type='submit' style='width:100%; padding:0.75rem; background:linear-gradient(135deg, #3b82f6 0%, #2563eb 100%); border:none; color:white; border-radius:0.5rem; cursor:pointer; font-weight:bold;'>Verify</button>"
                + "</form>"
                + "</div></body></html>";
        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    private Response renderMfaSetup(User user, String secret, String rt, String cid, String ruri, String scp, String st,
            String cc, String ccm, String mfaSessionId, String error) {
        String errHtml = error != null
                ? "<div style='background:#fee2e2; border:1px solid #ef4444; color:#b91c1c; padding:0.75rem; border-radius:0.5rem; margin-bottom:1.5rem; text-align:center;'>"
                        + error + "</div>"
                : "";
        String s_secret = sanitizationService.sanitize(secret);
        String html = "<html><body style='background:#0f172a; color:white; font-family:sans-serif; display:flex; align-items:center; justify-content:center; min-height:100vh; margin:0;'>"
                + "<div style='background:#1e293b; padding:2rem; border-radius:1rem; border:1px solid #334155; width:400px;'>"
                + "<h2 style='text-align:center; color:#3b82f6;'>Setup MFA</h2>"
                + "<p style='text-align:center; color:#94a3b8;'>Scan this Key in Google Authenticator:</p>"
                + "<div style='background:white; color:black; padding:1rem; text-align:center; font-family:monospace; margin-bottom:1rem; border-radius:0.5rem;'>"
                + s_secret + "</div>"
                + "<p style='text-align:center; color:#94a3b8; font-size:0.8rem;'>Manual Entry: " + s_secret + "</p>"
                + errHtml
                + "<form method='GET' action='" + getBaseUrl() + "/iam-service/api/oauth2/authorize'>"
                + "<input type='hidden' name='response_type' value='" + sanitizationService.sanitize(rt) + "'>"
                + "<input type='hidden' name='client_id' value='" + sanitizationService.sanitize(cid) + "'>"
                + "<input type='hidden' name='redirect_uri' value='" + sanitizationService.sanitize(ruri) + "'>"
                + "<input type='hidden' name='scope' value='" + sanitizationService.sanitize(scp) + "'>"
                + "<input type='hidden' name='state' value='" + sanitizationService.sanitize(st) + "'>"
                + "<input type='hidden' name='code_challenge' value='" + sanitizationService.sanitize(cc) + "'>"
                + "<input type='hidden' name='code_challenge_method' value='" + sanitizationService.sanitize(ccm) + "'>"
                + "<input type='hidden' name='mfa_session_id' value='" + sanitizationService.sanitize(mfaSessionId)
                + "'>"
                + "<input type='hidden' name='setup_mfa' value='confirm'>"
                + "<input type='hidden' name='temp_secret' value='" + sanitizationService.sanitize(secret) + "'>"
                + "<div style='margin-bottom:1.5rem;'><input name='mfa_code' placeholder='Enter Code' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; text-align:center; font-size:1.5rem;' maxlength='6' autofocus></div>"
                + "<button type='submit' style='width:100%; padding:0.75rem; background:linear-gradient(135deg, #10b981 0%, #059669 100%); border:none; color:white; border-radius:0.5rem; cursor:pointer; font-weight:bold;'>Activate & Login</button>"
                + "</form>"
                + "</div></body></html>";
        return Response.ok(html).type(MediaType.TEXT_HTML).build();
    }

    private String getRegisterHtml(String rt, String cid, String ruri, String scp, String st, String cc, String ccm) {
        String s_rt = sanitizationService.sanitize(rt);
        String s_cid = sanitizationService.sanitize(cid);
        String s_ruri = sanitizationService.sanitize(ruri);
        String s_scp = sanitizationService.sanitize(scp);
        String s_st = sanitizationService.sanitize(st);
        String s_cc = sanitizationService.sanitize(cc);
        String s_ccm = sanitizationService.sanitize(ccm);
        String baseUrl = getBaseUrl();

        return "<html><body style='background:#0f172a; color:white; font-family:sans-serif; display:flex; align-items:center; justify-content:center; min-height:100vh; margin:0;'>"
                + "<div style='background:#1e293b; padding:2rem; border-radius:1rem; border:1px solid #334155; width:350px; box-shadow:0 10px 15px -3px rgba(0,0,0,0.1);'>"
                + "<h2 style='text-align:center; color:#818cf8; margin-bottom:1.5rem;'>Create Account</h2>"
                + "<form method='POST' action='" + baseUrl + "/iam-service/api/oauth2/register'>"
                + "<input type='hidden' name='response_type' value='" + s_rt + "'>"
                + "<input type='hidden' name='client_id' value='" + s_cid + "'>"
                + "<input type='hidden' name='redirect_uri' value='" + s_ruri + "'>"
                + "<input type='hidden' name='scope' value='" + s_scp + "'>"
                + "<input type='hidden' name='state' value='" + s_st + "'>"
                + "<input type='hidden' name='code_challenge' value='" + s_cc + "'>"
                + "<input type='hidden' name='code_challenge_method' value='" + s_ccm + "'>"
                + "<div style='margin-bottom:1rem;'><label style='display:block; margin-bottom:0.5rem; color:#94a3b8;'>Username</label><input required name='username' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; outline:none; box-sizing:border-box;'></div>"
                + "<div style='margin-bottom:1rem;'><label style='display:block; margin-bottom:0.5rem; color:#94a3b8;'>Email</label><input required type='email' name='email' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; outline:none; box-sizing:border-box;'></div>"
                + "<div style='margin-bottom:1.5rem;'><label style='display:block; margin-bottom:0.5rem; color:#94a3b8;'>Password</label><input required type='password' name='password' style='width:100%; padding:0.75rem; background:#0f172a; border:1px solid #334155; color:white; border-radius:0.5rem; outline:none; box-sizing:border-box;'></div>"
                + "<button type='submit' style='width:100%; padding:0.75rem; background:linear-gradient(135deg, #818cf8 0%, #6366f1 100%); border:none; color:white; border-radius:0.5rem; cursor:pointer; font-weight:bold; transition:opacity 0.2s;'>Create Account</button>"
                + "</form>"
                + "<p style='text-align:center; margin-top:1.5rem; color:#94a3b8;'>Already have an account? <a href='/iam-service/api/oauth2/authorize?response_type="
                + s_rt + "&client_id=" + s_cid + "&redirect_uri=" + s_ruri + "&scope="
                + s_scp + "&state=" + s_st + "&code_challenge=" + s_cc + "&code_challenge_method="
                + s_ccm + "' style='color:#818cf8; text-decoration:none;'>Sign In</a></p>"
                + "</div></body></html>";
    }

}
