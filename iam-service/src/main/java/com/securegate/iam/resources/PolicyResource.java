package com.securegate.iam.resources;

import com.securegate.iam.model.Policy;
import com.securegate.iam.repository.PolicyRepository;
import com.securegate.iam.service.SanitizationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.util.List;
import java.util.UUID;

@Path("/policies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PolicyResource {

    @Inject
    private PolicyRepository policyRepository;

    @Inject
    private SanitizationService sanitizationService;

    @GET
    public Response getAllPolicies(@Context SecurityContext sc) {
        // RBAC Check: Only authenticated users (or explicitly ADMIN) can see policies
        if (sc.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        List<Policy> policies = policyRepository.findAll();
        return Response.ok(policies).build();
    }

    @POST
    public Response createPolicy(Policy policy, @Context SecurityContext sc) {
        if (policy == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Policy data is missing\"}")
                    .build();
        }

        System.out.println("Processing Policy [Name=" + policy.getName()
                + ", Effect=" + policy.getEffect()
                + ", Resource=" + policy.getResource()
                + ", Action=" + policy.getAction() + "]");

        // RBAC Check
        if (!sc.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\":\"Only administrators can create policies\"}").build();
        }

        // Validation of required fields
        if (policy.getName() == null || policy.getName().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Policy name is required\"}")
                    .build();
        }
        if (policy.getEffect() == null || policy.getEffect().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Policy effect is required\"}")
                    .build();
        }
        if (policy.getResource() == null || policy.getResource().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Policy resource is required\"}")
                    .build();
        }
        if (policy.getAction() == null || policy.getAction().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("{\"error\":\"Policy action is required\"}")
                    .build();
        }

        if (policy.getCreatedBy() == null || policy.getCreatedBy().isEmpty()) {
            policy.setCreatedBy(sc.getUserPrincipal().getName());
        }

        // Sanitize all inputs before persistence
        policy.setName(sanitizationService.sanitize(policy.getName()));
        policy.setResource(sanitizationService.sanitize(policy.getResource()));
        policy.setAction(sanitizationService.sanitize(policy.getAction()));
        policy.setConditions(sanitizationService.sanitize(policy.getConditions()));
        if (policy.getDescription() != null) {
            policy.setDescription(sanitizationService.sanitize(policy.getDescription()));
        }

        try {
            Policy saved = policyRepository.save(policy);
            return Response.status(Response.Status.CREATED).entity(saved).build();
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR SAVING POLICY: " + e.getMessage());
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("{\"error\":\"Transaction failed: " + e.getMessage() + "\"}").build();
        }
    }

    @DELETE
    @Path("/{id}")
    public Response deletePolicy(@PathParam("id") UUID policyId, @Context SecurityContext sc) {
        if (!sc.isUserInRole("ADMIN")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        policyRepository.deleteById(policyId);
        return Response.noContent().build();
    }
}
