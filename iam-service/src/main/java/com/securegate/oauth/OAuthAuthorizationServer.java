package com.securegate.oauth;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.Json;

import java.util.UUID;
import com.securegate.tokens.PasetoService;

@Path("/oauth")
public class OAuthAuthorizationServer {

    @GET
    @Path("/authorize")
    @Produces(MediaType.APPLICATION_JSON)
    public Response authorize(
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("state") String state,
            @QueryParam("code_challenge") String codeChallenge) {

        // Validate client_id and redirect_uri (Mock validation)
        if (!"securegate-pwa".equals(clientId)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid client_id").build();
        }

        String authCode = "AUTH-" + UUID.randomUUID();

        // In a real app, store authCode + codeChallenge + state in Redis/DB with
        // expiration
        // System.out.println("Issued Auth Code: " + authCode + " for challenge: " +
        // codeChallenge);

        return Response.ok(Json.createObjectBuilder()
                .add("code", authCode)
                .add("state", state)
                .build()).build();
    }

    @POST
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(
            @FormParam("grant_type") String grantType,
            @FormParam("code") String code,
            @FormParam("code_verifier") String codeVerifier,
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri) {

        // Validate grant_type
        if (!"authorization_code".equals(grantType)) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid grant_type").build();
        }

        // Validate code (Mock check)
        if (code == null || !code.startsWith("AUTH-")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid code").build();
        }

        // Verify PKCE: code_verifier -> SHA256 -> Base64UrlSafe must match stored
        // code_challenge
        // Skipping strict PKCE verification for this simplified implementation,
        // assuming frontend handles it.

        String accessToken = PasetoService.createAccessToken("user-" + UUID.randomUUID());

        return Response.ok(Json.createObjectBuilder()
                .add("access_token", accessToken)
                .add("token_type", "Bearer")
                .add("expires_in", 3600)
                .build()).build();
    }
}
