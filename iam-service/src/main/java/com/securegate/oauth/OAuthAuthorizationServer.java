

@Path("/oauth")
public class AuthorizationEndpoint {
    @GET
    @Path("/authorize")
    @Produces(MediaType.APPLICATION_JSON)
    public Response authorize(
        @QueryParam("client_id") String clientId,
        @QueryParam("redirect_uri") String redirectUri,
        @QueryParam("state") String state,
        @QueryParam("code_challenge") String codeChallenge
    ) {
        String authCode = "AUTH-" + UUID.randomUUID();

        return Response.ok(Json.createObjectBuilder()
                .add("code", authCode)
                .add("state", state)
                .build()
        ).build();
    }
}

---TokenEndpoint---
@Path("/oauth")
public class TokenEndpoint {

    @POST
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    public Response token(
        @FormParam("code") String code,
        @FormParam("code_verifier") String codeVerifier
    ) {
        String accessToken = PasetoService.createAccessToken("user123");

        return Response.ok(Json.createObjectBuilder()
                .add("access_token", accessToken)
                .add("token_type", "Bearer")
                .add("expires_in", 3600)
                .build()
        ).build();
    }
}
