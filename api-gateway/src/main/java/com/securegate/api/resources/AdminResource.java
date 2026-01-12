package com.securegate.api.resources;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1")
public class AdminResource {

    @GET
    @Path("/roles")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoles() {
        return Response.ok(
                "[{\"id\":\"1\", \"name\":\"ADMIN\"}, {\"id\":\"2\", \"name\":\"USER\"}, {\"id\":\"3\", \"name\":\"FINANCE_MANAGER\"}]")
                .build();
    }

    @GET
    @Path("/policies")
    @RolesAllowed("ADMIN")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPolicies() {
        return Response.ok("[{\"id\":\"p1\", \"effect\":\"PERMIT\", \"resource\":\"PAYMENT\", \"action\":\"APPROVE\"}]")
                .build();
    }
}
