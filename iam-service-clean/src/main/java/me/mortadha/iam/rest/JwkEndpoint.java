package me.mortadha.iam.rest;

import com.nimbusds.jose.jwk.OctetKeyPair;
import jakarta.ejb.EJB;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.mortadha.iam.security.JwtManager;

import java.util.Set;
import java.util.logging.Logger;

/**
 * JSON Web Key Set (JWKS) Endpoint
 * Provides public keys for JWT verification
 */
@Path("/jwk")
public class JwkEndpoint {

    private static final Logger LOGGER = Logger.getLogger(JwkEndpoint.class.getName());

    @EJB
    private JwtManager jwtManager;

    /**
     * Get all public keys in JWKS format
     * GET /jwk
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getJwks() {
        try {
            Set<OctetKeyPair> publicKeys = jwtManager.getAllPublicKeys();

            JsonArrayBuilder keysBuilder = Json.createArrayBuilder();

            for (OctetKeyPair key : publicKeys) {
                JsonObjectBuilder keyBuilder = Json.createObjectBuilder()
                    .add("kty", "OKP")
                    .add("crv", "Ed25519")
                    .add("use", "sig")
                    .add("kid", key.getKeyID())
                    .add("x", key.getX().toString());

                keysBuilder.add(keyBuilder);
            }

            var jwks = Json.createObjectBuilder()
                .add("keys", keysBuilder)
                .build();

            LOGGER.fine("Served JWKS with " + publicKeys.size() + " keys");

            return Response.ok(jwks)
                .header("Cache-Control", "public, max-age=3600")
                .build();

        } catch (Exception e) {
            LOGGER.severe("Error generating JWKS: " + e.getMessage());
            return Response.serverError()
                .entity(Json.createObjectBuilder()
                    .add("error", "server_error")
                    .add("error_description", "Failed to generate JWKS")
                    .build())
                .build();
        }
    }
}
