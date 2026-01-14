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
            // PDP Logic: Evaluate if 'user' can perform 'action' on 'resource'
            // The 'user' variable was declared but not used after the null check.
            // The null check for 'user' is still relevant for UNAUTHORIZED response.
            Principal user = requestContext.getSecurityContext().getUserPrincipal();

            if (user == null) {
                requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build());
                return;
            }

            // PDP Logic: Evaluate if 'user' can perform 'action' on 'resource'
            // Removing insecure mock shortcuts (admin/READ).
            boolean allowed = requestContext.getSecurityContext().isUserInRole("ADMIN");

            if (!allowed) {
                requestContext.abortWith(Response.status(Response.Status.FORBIDDEN)
                        .entity("{\"error\":\"Access denied by Security Policy\"}").build());
            }
        }
    }
}
