package com.securegate.stego.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;

/**
 * Health check endpoint for Stego Module.
 */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

    private static final Instant START_TIME = Instant.now();

    @GET
    public Response healthCheck() {
        long uptimeSeconds = Instant.now().getEpochSecond() - START_TIME.getEpochSecond();

        String health = String.format(
                "{\"status\":\"UP\",\"service\":\"stego-module\",\"version\":\"1.0.0\",\"algorithm\":\"STC\",\"encryption\":\"ChaCha20-Poly1305\",\"uptimeSeconds\":%d,\"timestamp\":\"%s\"}",
                uptimeSeconds,
                Instant.now().toString());

        return Response.ok(health).build();
    }

    @GET
    @Path("/ready")
    public Response readinessCheck() {
        return Response.ok("{\"status\":\"READY\"}").build();
    }

    @GET
    @Path("/live")
    public Response livenessCheck() {
        return Response.ok("{\"status\":\"ALIVE\"}").build();
    }
}
