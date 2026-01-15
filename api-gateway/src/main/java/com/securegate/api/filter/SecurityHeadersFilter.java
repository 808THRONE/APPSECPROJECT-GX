package com.securegate.api.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * Filter to inject security-hardening headers into all outgoing responses.
 * Implements HSTS, Content-Security-Policy, and other protection mechanisms.
 */
@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        // Enforce HTTPS for 1 year (HSTS)
        responseContext.getHeaders().add("Strict-Transport-Security", "max-age=31536000; includeSubDomains; preload");

        // Prevent MIME type sniffing
        responseContext.getHeaders().add("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking by disallowing framing
        responseContext.getHeaders().add("X-Frame-Options", "DENY");

        // Prevent XSS reflection
        responseContext.getHeaders().add("X-XSS-Protection", "1; mode=block");

        // Content Security Policy (Basic restrictive policy)
        responseContext.getHeaders().add("Content-Security-Policy",
                "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:;");

        // Referrer Policy
        responseContext.getHeaders().add("Referrer-Policy", "strict-origin-when-cross-origin");
    }
}
