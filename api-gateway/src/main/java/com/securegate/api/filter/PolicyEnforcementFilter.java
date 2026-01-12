package com.securegate.api.filter;

import com.securegate.api.security.RequiresPolicy;
import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.security.Principal;

@Provider
@RequiresPolicy(resource = "", action = "") // Binding
@Priority(Priorities.AUTHORIZATION)
public class PolicyEnforcementFilter implements ContainerRequestFilter {

    @Context
    private ResourceInfo resourceInfo;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        RequiresPolicy annotation = resourceInfo.getResourceMethod().getAnnotation(RequiresPolicy.class);
        if (annotation == null) {
            annotation = resourceInfo.getResourceClass().getAnnotation(RequiresPolicy.class);
        }

        if (annotation != null) {
            String resource = annotation.resource();
            String action = annotation.action();
            Principal user = requestContext.getSecurityContext().getUserPrincipal();

            if (user == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                return;
            }

            // PDP Logic: Evaluate if 'user' can perform 'action' on 'resource'
            // For Demo: Allow if action is "READ" or user is "admin"
            // In Prod: Call PDP Service (GRPC/HTTP)

            boolean allowed = evaluatePolicy(user.getName(), resource, action);

            if (!allowed) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN).entity("Policy Denied").build());
            }
        }
    }

    private boolean evaluatePolicy(String username, String resource, String action) {
        // Simple mock PDP
        if ("admin".equals(username))
            return true;
        if ("READ".equalsIgnoreCase(action))
            return true;

        return false;
    }
}
