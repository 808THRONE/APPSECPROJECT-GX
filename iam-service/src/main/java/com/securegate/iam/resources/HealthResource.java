package com.securegate.iam.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;

/**
 * Health check endpoint for IAM Service.
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final Instant START_TIME = Instant.now();

    @GET
    public Response healthCheck() {
        long uptimeSeconds = Instant.now().getEpochSecond() - START_TIME.getEpochSecond();

        String health = String.format(
                "{\"status\":\"UP\",\"service\":\"iam-service\",\"version\":\"1.0.0\",\"uptimeSeconds\":%d,\"timestamp\":\"%s\"}",
                uptimeSeconds,
                Instant.now().toString());

        return Response.ok(health).build();
    }

    @GET
    @Path("/ready")
    public Response readinessCheck() {
        // Could check database connectivity here
        return Response.ok("{\"status\":\"READY\"}").build();
    }

    @GET
    @Path("/live")
    public Response livenessCheck() {
        return Response.ok("{\"status\":\"ALIVE\"}").build();
    }
}
