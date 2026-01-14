package com.securegate.iam.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    // Allowed origins for CORS - covers Nginx proxy (80), dev server (5173),
    // direct container access (8080, 8081)
    private static final Set<String> ALLOWED_ORIGINS = new HashSet<>();

    static {
        // Default allowed origins
        ALLOWED_ORIGINS.add("http://localhost");
        ALLOWED_ORIGINS.add("http://localhost:80");
        ALLOWED_ORIGINS.add("http://localhost:5173"); // Vite dev server
        ALLOWED_ORIGINS.add("http://localhost:8080"); // API Gateway direct
        ALLOWED_ORIGINS.add("http://localhost:8081"); // IAM direct
        ALLOWED_ORIGINS.add("http://localhost:8084"); // Stego direct

        // Allow configuration from environment variable
        String extraOrigins = System.getenv("CORS_ALLOWED_ORIGINS");
        if (extraOrigins != null && !extraOrigins.trim().isEmpty()) {
            ALLOWED_ORIGINS.addAll(Arrays.asList(extraOrigins.split(",")));
        }
    }

    private String getAllowedOrigin(String origin) {
        if (origin != null && ALLOWED_ORIGINS.contains(origin)) {
            return origin;
        }
        return "http://localhost"; // Default fallback
    }

    /**
     * Pre-matching filter to handle CORS preflight (OPTIONS) requests.
     * This MUST run before JAX-RS tries to match a resource method.
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");
        String allowedOrigin = getAllowedOrigin(origin);

        // Handle preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            Response.ResponseBuilder builder = Response.ok();
            builder.header("Access-Control-Allow-Origin", allowedOrigin);
            builder.header("Access-Control-Allow-Credentials", "true");
            builder.header("Access-Control-Allow-Headers",
                    "origin, content-type, accept, authorization, x-requested-with, x-csrf-token");
            builder.header("Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS, HEAD");
            builder.header("Access-Control-Max-Age", "86400");

            // Abort the filter chain and return the preflight response
            requestContext.abortWith(builder.build());
        }
    }

    /**
     * Response filter to add CORS headers to all responses.
     */
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        String origin = requestContext.getHeaderString("Origin");
        String allowedOrigin = getAllowedOrigin(origin);

        responseContext.getHeaders().add("Access-Control-Allow-Origin", allowedOrigin);
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
        responseContext.getHeaders().add("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, x-requested-with, x-csrf-token");
        responseContext.getHeaders().add("Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        responseContext.getHeaders().add("Access-Control-Max-Age", "86400");
    }
}
