package xyz.kaaniche.phoenix.iam.boundaries;

import jakarta.ejb.EJB;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import xyz.kaaniche.phoenix.iam.security.JwtManager;

@Path("jwk")
@ApplicationScoped
public class JWKEndpoint {
    @EJB
    private JwtManager jwtManager;

    @GET
    public Response getPublicVerificationKey(@QueryParam ("kid") String kid) throws Exception {
        try {
            return Response.ok(jwtManager.getPublicValidationKey(kid).toJSONString()).type(MediaType.APPLICATION_JSON).build();
        }catch (Throwable t){
            return Response.status(Response.Status.BAD_REQUEST).entity(t.getMessage()).build();
        }
    }
}
