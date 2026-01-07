package com.securegate.api;

import com.nimbusds.jwt.JWTClaimsSet;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObjectBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.logging.Logger;

/**
 * User Profile and Dashboard REST Resource
 */
@Path("/users")
public class UserResource {

    private static final Logger LOGGER = Logger.getLogger(UserResource.class.getName());

    @Context
    private ContainerRequestContext requestContext;

    @Context
    private SecurityContext securityContext;

    /**
     * Get current user profile from JWT claims
     * GET /api/users/me
     */
    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCurrentUser() {
        String username = securityContext.getUserPrincipal().getName();
        String tenantId = (String) requestContext.getProperty("tenantId");
        String scope = (String) requestContext.getProperty("scope");

        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) requestContext.getProperty("roles");

        JWTClaimsSet claims = (JWTClaimsSet) requestContext.getProperty("claims");

        JsonArrayBuilder rolesArray = Json.createArrayBuilder();
        if (roles != null) {
            roles.forEach(rolesArray::add);
        }

        JsonObjectBuilder user = Json.createObjectBuilder()
                .add("username", username != null ? username : "")
                .add("email", username != null ? username : "")
                .add("tenantId", tenantId != null ? tenantId : "")
                .add("scope", scope != null ? scope : "")
                .add("roles", rolesArray)
                .add("authenticated", true);

        // Add additional claims if available
        if (claims != null) {
            try {
                if (claims.getClaim("upn") != null) {
                    user.add("upn", claims.getStringClaim("upn"));
                }
            } catch (Exception e) {
                LOGGER.fine("Could not extract upn claim: " + e.getMessage());
            }
        }

        LOGGER.fine("Returning user profile for: " + username);

        return Response.ok(user.build()).build();
    }

    /**
     * Get dashboard statistics
     * GET /api/users/dashboard
     */
    @GET
    @Path("/dashboard")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDashboard() {
        String username = securityContext.getUserPrincipal().getName();

        @SuppressWarnings("unchecked")
        Set<String> roles = (Set<String>) requestContext.getProperty("roles");

        // Mock dashboard data - in production, fetch from database
        JsonObjectBuilder dashboard = Json.createObjectBuilder()
                .add("totalUsers", 42)
                .add("activeSessions", 12)
                .add("pendingRequests", 3)
                .add("policiesActive", 8)
                .add("securityAlerts", 0)
                .add("lastLogin", LocalDateTime.now(ZoneId.of("UTC")).minusHours(2).toString())
                .add("systemStatus", "healthy");

        // Add role-specific stats
        if (roles != null && (roles.contains("Administrator") || roles.contains("admin"))) {
            dashboard.add("isAdmin", true)
                    .add("failedLoginAttempts24h", 5)
                    .add("newUsersThisWeek", 3);
        } else {
            dashboard.add("isAdmin", false);
        }

        JsonArrayBuilder recentActivity = Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                        .add("action", "login")
                        .add("timestamp", Instant.now().minusSeconds(300).toString())
                        .add("status", "success"))
                .add(Json.createObjectBuilder()
                        .add("action", "view_policies")
                        .add("timestamp", Instant.now().minusSeconds(600).toString())
                        .add("status", "success"))
                .add(Json.createObjectBuilder()
                        .add("action", "update_profile")
                        .add("timestamp", Instant.now().minusSeconds(3600).toString())
                        .add("status", "success"));

        dashboard.add("recentActivity", recentActivity);

        LOGGER.fine("Returning dashboard for: " + username);

        return Response.ok(dashboard.build()).build();
    }

    /**
     * Health check endpoint (public)
     * GET /api/users/health
     */
    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response healthCheck() {
        return Response.ok(Json.createObjectBuilder()
                .add("status", "healthy")
                .add("service", "api-gateway")
                .add("timestamp", Instant.now().toString())
                .build())
                .build();
    }
}
