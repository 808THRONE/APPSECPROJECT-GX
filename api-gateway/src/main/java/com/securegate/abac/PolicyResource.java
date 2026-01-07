package com.securegate.abac;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonReader;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

import java.io.StringReader;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * ABAC Policy REST Resource - CRUD operations for policies
 */
@Path("/policies")
public class PolicyResource {

    private static final Logger LOGGER = Logger.getLogger(PolicyResource.class.getName());

    // In-memory policy store (in production, use database)
    private static final Map<String, JsonObject> policies = new ConcurrentHashMap<>();

    static {
        // Initialize with demo policies
        policies.put("p001", Json.createObjectBuilder()
                .add("policy_id", "p001")
                .add("effect", "permit")
                .add("name", "Engineering Team Access")
                .add("description", "Allow engineering and security teams to access high-sensitivity audit logs")
                .add("subject", Json.createObjectBuilder()
                        .add("department", Json.createArrayBuilder().add("engineering").add("security"))
                        .add("clearance_level", Json.createObjectBuilder().add("min", 3)))
                .add("resource", Json.createObjectBuilder()
                        .add("type", "audit_logs")
                        .add("sensitivity", "high"))
                .add("environment", Json.createObjectBuilder()
                        .add("time", Json.createObjectBuilder().add("start", "09:00").add("end", "18:00"))
                        .add("location", Json.createObjectBuilder()
                                .add("countries", Json.createArrayBuilder().add("US").add("CA"))))
                .add("created_at", Instant.now().minusSeconds(86400).toString())
                .add("updated_at", Instant.now().toString())
                .build());

        policies.put("p002", Json.createObjectBuilder()
                .add("policy_id", "p002")
                .add("effect", "deny")
                .add("name", "Block External Access")
                .add("description", "Deny access from non-approved countries")
                .add("subject", Json.createObjectBuilder())
                .add("resource", Json.createObjectBuilder()
                        .add("type", "all"))
                .add("environment", Json.createObjectBuilder()
                        .add("location", Json.createObjectBuilder()
                                .add("countries_blacklist", Json.createArrayBuilder().add("RU").add("CN").add("KP"))))
                .add("created_at", Instant.now().minusSeconds(172800).toString())
                .add("updated_at", Instant.now().toString())
                .build());
    }

    @Context
    private SecurityContext securityContext;

    /**
     * List all policies
     * GET /api/policies
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listPolicies() {
        LOGGER.fine("Listing all policies");

        JsonArrayBuilder array = Json.createArrayBuilder();
        policies.values().forEach(array::add);

        return Response.ok(Json.createObjectBuilder()
                .add("policies", array)
                .add("total", policies.size())
                .build())
                .build();
    }

    /**
     * Get policy by ID
     * GET /api/policies/{id}
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPolicy(@PathParam("id") String id) {
        LOGGER.fine("Getting policy: " + id);

        JsonObject policy = policies.get(id);
        if (policy == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder()
                            .add("error", "not_found")
                            .add("message", "Policy not found: " + id)
                            .build())
                    .build();
        }

        return Response.ok(policy).build();
    }

    /**
     * Create new policy
     * POST /api/policies
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPolicy(String body) {
        LOGGER.info("Creating new policy");

        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject input = reader.readObject();

            // Generate policy ID if not provided
            String policyId = input.containsKey("policy_id")
                    ? input.getString("policy_id")
                    : "p" + String.format("%03d", policies.size() + 1);

            // Check if already exists
            if (policies.containsKey(policyId)) {
                return Response.status(Response.Status.CONFLICT)
                        .entity(Json.createObjectBuilder()
                                .add("error", "conflict")
                                .add("message", "Policy already exists: " + policyId)
                                .build())
                        .build();
            }

            // Build policy with metadata
            JsonObjectBuilder policyBuilder = Json.createObjectBuilder()
                    .add("policy_id", policyId)
                    .add("effect", input.getString("effect", "permit"))
                    .add("name", input.getString("name", "Unnamed Policy"))
                    .add("description", input.getString("description", ""))
                    .add("created_at", Instant.now().toString())
                    .add("updated_at", Instant.now().toString())
                    .add("created_by", securityContext.getUserPrincipal().getName());

            // Copy subject, resource, environment
            if (input.containsKey("subject")) {
                policyBuilder.add("subject", input.getJsonObject("subject"));
            } else {
                policyBuilder.add("subject", Json.createObjectBuilder().build());
            }

            if (input.containsKey("resource")) {
                policyBuilder.add("resource", input.getJsonObject("resource"));
            } else {
                policyBuilder.add("resource", Json.createObjectBuilder().build());
            }

            if (input.containsKey("environment")) {
                policyBuilder.add("environment", input.getJsonObject("environment"));
            }

            JsonObject policy = policyBuilder.build();
            policies.put(policyId, policy);

            LOGGER.info("Created policy: " + policyId);

            return Response.status(Response.Status.CREATED)
                    .entity(policy)
                    .build();

        } catch (Exception e) {
            LOGGER.warning("Error creating policy: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                            .add("error", "invalid_request")
                            .add("message", e.getMessage())
                            .build())
                    .build();
        }
    }

    /**
     * Update policy
     * PUT /api/policies/{id}
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePolicy(@PathParam("id") String id, String body) {
        LOGGER.info("Updating policy: " + id);

        if (!policies.containsKey(id)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder()
                            .add("error", "not_found")
                            .add("message", "Policy not found: " + id)
                            .build())
                    .build();
        }

        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            JsonObject input = reader.readObject();
            JsonObject existing = policies.get(id);

            // Merge with existing
            JsonObjectBuilder policyBuilder = Json.createObjectBuilder()
                    .add("policy_id", id)
                    .add("effect", input.getString("effect", existing.getString("effect")))
                    .add("name", input.getString("name", existing.getString("name")))
                    .add("description", input.getString("description", existing.getString("description", "")))
                    .add("created_at", existing.getString("created_at"))
                    .add("updated_at", Instant.now().toString())
                    .add("updated_by", securityContext.getUserPrincipal().getName());

            // Update conditions
            if (input.containsKey("subject")) {
                policyBuilder.add("subject", input.getJsonObject("subject"));
            } else if (existing.containsKey("subject")) {
                policyBuilder.add("subject", existing.getJsonObject("subject"));
            }

            if (input.containsKey("resource")) {
                policyBuilder.add("resource", input.getJsonObject("resource"));
            } else if (existing.containsKey("resource")) {
                policyBuilder.add("resource", existing.getJsonObject("resource"));
            }

            if (input.containsKey("environment")) {
                policyBuilder.add("environment", input.getJsonObject("environment"));
            } else if (existing.containsKey("environment")) {
                policyBuilder.add("environment", existing.getJsonObject("environment"));
            }

            JsonObject policy = policyBuilder.build();
            policies.put(id, policy);

            LOGGER.info("Updated policy: " + id);

            return Response.ok(policy).build();

        } catch (Exception e) {
            LOGGER.warning("Error updating policy: " + e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Json.createObjectBuilder()
                            .add("error", "invalid_request")
                            .add("message", e.getMessage())
                            .build())
                    .build();
        }
    }

    /**
     * Delete policy
     * DELETE /api/policies/{id}
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePolicy(@PathParam("id") String id) {
        LOGGER.info("Deleting policy: " + id);

        JsonObject removed = policies.remove(id);
        if (removed == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Json.createObjectBuilder()
                            .add("error", "not_found")
                            .add("message", "Policy not found: " + id)
                            .build())
                    .build();
        }

        LOGGER.info("Deleted policy: " + id);

        return Response.ok(Json.createObjectBuilder()
                .add("message", "Policy deleted")
                .add("policy_id", id)
                .build())
                .build();
    }
}
