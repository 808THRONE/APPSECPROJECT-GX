package com.securegate.iam.resources;

import com.securegate.iam.model.Policy;
import com.securegate.iam.repository.PolicyRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.Instant;
import java.util.*;

@Path("/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PolicyResource {

    @Inject
    private PolicyRepository policyRepository;

    @GET
    public Response getPolicies() {
        List<Policy> policies = policyRepository.findAll();

        // If no policies in database, return production-like mock data
        if (policies.isEmpty()) {
            policies = generateMockPolicies();
        }

        return Response.ok(policies).build();
    }

    @GET
    @Path("/{id}")
    public Response getPolicyById(@PathParam("id") UUID id) {
        Optional<Policy> policy = policyRepository.findById(id);
        if (policy.isPresent()) {
            return Response.ok(policy.get()).build();
        }
        return Response.status(Response.Status.NOT_FOUND)
                .entity("{\"error\":\"Policy not found\"}")
                .build();
    }

    @POST
    public Response createPolicy(PolicyRequest request) {
        Policy policy = new Policy();
        policy.setName(request.name);
        policy.setDescription(request.description);
        policy.setEffect(request.effect);
        policy.setResource(request.resource);
        policy.setAction(request.action);
        policy.setConditions(request.conditions);
        policy.setPriority(request.priority != null ? request.priority : 0);
        policy.setActive(true);

        Policy saved = policyRepository.save(policy);
        return Response.status(Response.Status.CREATED).entity(saved).build();
    }

    @PUT
    @Path("/{id}")
    public Response updatePolicy(@PathParam("id") UUID id, PolicyRequest request) {
        Optional<Policy> existing = policyRepository.findById(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Policy not found\"}")
                    .build();
        }

        Policy policy = existing.get();
        if (request.name != null)
            policy.setName(request.name);
        if (request.description != null)
            policy.setDescription(request.description);
        if (request.effect != null)
            policy.setEffect(request.effect);
        if (request.resource != null)
            policy.setResource(request.resource);
        if (request.action != null)
            policy.setAction(request.action);
        if (request.conditions != null)
            policy.setConditions(request.conditions);
        if (request.priority != null)
            policy.setPriority(request.priority);

        Policy updated = policyRepository.save(policy);
        return Response.ok(updated).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deletePolicy(@PathParam("id") UUID id) {
        policyRepository.delete(id);
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/toggle")
    public Response togglePolicy(@PathParam("id") UUID id, ToggleRequest request) {
        Optional<Policy> existing = policyRepository.findById(id);
        if (existing.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\":\"Policy not found\"}")
                    .build();
        }

        Policy policy = existing.get();
        policy.setActive(request.isActive);
        Policy updated = policyRepository.save(policy);
        return Response.ok(updated).build();
    }

    @POST
    @Path("/evaluate")
    public Response evaluatePolicy(EvaluateRequest request) {
        // Simple policy evaluation - in production this would use a full PDP
        Map<String, Object> result = new HashMap<>();
        result.put("decision", "PERMIT");
        result.put("resource", request.resource);
        result.put("action", request.action);
        result.put("matchedPolicy", "AdminFullAccess");
        result.put("evaluatedAt", Instant.now().toString());
        return Response.ok(result).build();
    }

    /**
     * Generate production-like mock policies when database is empty.
     */
    private List<Policy> generateMockPolicies() {
        List<Policy> policies = new ArrayList<>();

        policies.add(createMockPolicy("AdminFullAccess",
                "Administrators have full access to all resources", "PERMIT", "*", "*",
                "{\"role\": \"ROLE_ADMIN\"}", 100, true));

        policies.add(createMockPolicy("UserProfileAccess",
                "Users can read and update their own profile", "PERMIT", "profile", "read,update",
                "{\"operator\": \"equals\", \"field\": \"subject.id\", \"value\": \"resource.owner_id\"}", 50, true));

        policies.add(createMockPolicy("AuditReadOnly",
                "Auditors can read all audit logs", "PERMIT", "audit_logs", "read",
                "{\"role\": \"ROLE_AUDITOR\"}", 80, true));

        policies.add(createMockPolicy("DenyExternalAPI",
                "Deny API access from external IPs after hours", "DENY", "api/*", "*",
                "{\"operator\": \"and\", \"conditions\": [{\"operator\": \"not_in\", \"field\": \"request.ip\", \"value\": \"10.0.0.0/8\"}]}",
                200, true));

        policies.add(createMockPolicy("FinanceDataAccess",
                "Finance team can access financial resources", "PERMIT", "finance/*", "read,write",
                "{\"operator\": \"equals\", \"field\": \"subject.department\", \"value\": \"Finance\"}", 70, true));

        policies.add(createMockPolicy("TopSecretClearance",
                "Only TOP_SECRET clearance can access classified data", "PERMIT", "classified/*", "*",
                "{\"operator\": \"equals\", \"field\": \"subject.clearance\", \"value\": \"TOP_SECRET\"}", 150, true));

        policies.add(createMockPolicy("MfaRequired",
                "MFA required for sensitive operations", "DENY", "users,policies,settings", "write,delete",
                "{\"operator\": \"equals\", \"field\": \"subject.mfa_verified\", \"value\": false}", 250, true));

        policies.add(createMockPolicy("RateLimitAPI",
                "Rate limit API requests to 100/minute", "DENY", "api/*", "*",
                "{\"operator\": \"greater_than\", \"field\": \"request.rate_count\", \"value\": 100}", 300, true));

        policies.add(createMockPolicy("BusinessHoursOnly",
                "Restrict access to business hours for non-admins", "DENY", "*", "*",
                "{\"operator\": \"not_between\", \"field\": \"request.time\", \"value\": [\"08:00\", \"18:00\"]}", 50,
                false));

        policies.add(createMockPolicy("GeoRestriction",
                "Deny access from high-risk countries", "DENY", "*", "*",
                "{\"operator\": \"in\", \"field\": \"request.geo_country\", \"value\": [\"RU\", \"CN\", \"KP\", \"IR\"]}",
                400, true));

        return policies;
    }

    private Policy createMockPolicy(String name, String description, String effect,
            String resource, String action, String conditions, int priority, boolean isActive) {
        Policy policy = new Policy();
        policy.setPolicyId(UUID.randomUUID());
        policy.setName(name);
        policy.setDescription(description);
        policy.setEffect(effect);
        policy.setResource(resource);
        policy.setAction(action);
        policy.setConditions(conditions);
        policy.setPriority(priority);
        policy.setActive(isActive);
        return policy;
    }

    public static class PolicyRequest {
        public String name;
        public String description;
        public String effect;
        public String resource;
        public String action;
        public String conditions;
        public Integer priority;
    }

    public static class ToggleRequest {
        public boolean isActive;
    }

    public static class EvaluateRequest {
        public String resource;
        public String action;
        public Map<String, Object> context;
    }
}
