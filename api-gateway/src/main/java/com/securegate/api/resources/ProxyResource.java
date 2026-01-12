package com.securegate.api.resources;

import com.securegate.api.service.IamProxyClient;
import com.securegate.api.service.StegoClient;
import com.securegate.api.service.StegoProxyClient;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

/**
 * Proxy Resource for routing all API requests to IAM Service.
 * This is the main entry point for the frontend - all requests go through here.
 */
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProxyResource {

    @Inject
    private IamProxyClient proxyClient;

    @Inject
    private StegoClient stegoClient;

    // ============== USER ENDPOINTS ==============

    @GET
    @Path("/users")
    public Response getUsers(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/users", headers);
        return buildResponse(response);
    }

    @GET
    @Path("/users/{id}")
    public Response getUserById(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/users/" + id, headers);
        return buildResponse(response);
    }

    @POST
    @Path("/users")
    public Response createUser(String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPostWithStego("/users", realBody, headers);
        return buildResponse(response);
    }

    @PUT
    @Path("/users/{id}")
    public Response updateUser(@PathParam("id") String id, String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPutWithStego("/users/" + id, realBody, headers);
        return buildResponse(response);
    }

    @DELETE
    @Path("/users/{id}")
    public Response deleteUser(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyDeleteWithStego("/users/" + id, headers);
        return buildResponse(response);
    }

    @POST
    @Path("/users/{id}/mfa")
    public Response toggleMfa(@PathParam("id") String id, String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPostWithStego("/users/" + id + "/mfa", realBody, headers);
        return buildResponse(response);
    }

    // ============== POLICY ENDPOINTS ==============

    @GET
    @Path("/policies")
    public Response getPolicies(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/policies", headers);
        return buildResponse(response);
    }

    @GET
    @Path("/policies/{id}")
    public Response getPolicyById(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/policies/" + id, headers);
        return buildResponse(response);
    }

    @POST
    @Path("/policies")
    public Response createPolicy(String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPostWithStego("/policies", realBody, headers);
        return buildResponse(response);
    }

    @PUT
    @Path("/policies/{id}")
    public Response updatePolicy(@PathParam("id") String id, String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPutWithStego("/policies/" + id, realBody, headers);
        return buildResponse(response);
    }

    @DELETE
    @Path("/policies/{id}")
    public Response deletePolicy(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyDeleteWithStego("/policies/" + id, headers);
        return buildResponse(response);
    }

    @POST
    @Path("/policies/evaluate")
    public Response evaluatePolicy(String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPostWithStego("/policies/evaluate", realBody, headers);
        return buildResponse(response);
    }

    // ============== AUDIT ENDPOINTS ==============

    @GET
    @Path("/audit")
    public Response getAuditLogs(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/audit", headers);
        return buildResponse(response);
    }

    @GET
    @Path("/audit/{id}")
    public Response getAuditLogById(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/audit/" + id, headers);
        return buildResponse(response);
    }

    @GET
    @Path("/audit/stats")
    public Response getAuditStats(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/audit/stats", headers);
        return buildResponse(response);
    }

    // ============== NOTIFICATION ENDPOINTS ==============

    @GET
    @Path("/notifications")
    public Response getNotifications(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/notifications", headers);
        return buildResponse(response);
    }

    @GET
    @Path("/notifications/unread-count")
    public Response getUnreadCount(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/notifications/unread-count", headers);
        return buildResponse(response);
    }

    @POST
    @Path("/notifications/{id}/read")
    public Response markAsRead(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyPostWithStego("/notifications/" + id + "/read", "", headers);
        return buildResponse(response);
    }

    @POST
    @Path("/notifications/mark-all-read")
    public Response markAllAsRead(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyPostWithStego("/notifications/mark-all-read", "", headers);
        return buildResponse(response);
    }

    @DELETE
    @Path("/notifications/{id}")
    public Response deleteNotification(@PathParam("id") String id, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyDeleteWithStego("/notifications/" + id, headers);
        return buildResponse(response);
    }

    // ============== SETTINGS ENDPOINTS ==============

    @GET
    @Path("/settings")
    public Response getSettings(@Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/settings", headers);
        return buildResponse(response);
    }

    @GET
    @Path("/settings/{key}")
    public Response getSettingByKey(@PathParam("key") String key, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = proxyClient.proxyGetWithStego("/settings/" + key, headers);
        return buildResponse(response);
    }

    @PUT
    @Path("/settings/{key}")
    public Response updateSetting(@PathParam("key") String key, String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        String realBody = stegoClient.extractFromStegoJson(body);
        var response = proxyClient.proxyPutWithStego("/settings/" + key, realBody, headers);
        return buildResponse(response);
    }

    @Inject
    private StegoProxyClient stegoProxyClient;

    // ============== STEGO ENDPOINTS (Direct access) ==============

    @POST
    @Path("/stego/embed")
    public Response stegoEmbed(String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = stegoProxyClient.proxyPost("/stego/embed", body, headers);
        return buildResponse(response);
    }

    @POST
    @Path("/stego/extract")
    public Response stegoExtract(String body, @Context HttpHeaders httpHeaders) {
        var headers = extractHeaders(httpHeaders);
        var response = stegoProxyClient.proxyPost("/stego/extract", body, headers);
        return buildResponse(response);
    }

    // ============== HELPER METHODS ==============

    private Map<String, String> extractHeaders(HttpHeaders httpHeaders) {
        Map<String, String> headers = new HashMap<>();

        // Forward authorization header
        String auth = httpHeaders.getHeaderString("Authorization");
        if (auth != null) {
            headers.put("Authorization", auth);
        }

        // Forward content type
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");

        // Forward cookies for session management
        String cookie = httpHeaders.getHeaderString("Cookie");
        if (cookie != null) {
            headers.put("Cookie", cookie);
        }

        // Forward request ID for tracing
        String requestId = httpHeaders.getHeaderString("X-Request-ID");
        if (requestId != null) {
            headers.put("X-Request-ID", requestId);
        }

        return headers;
    }

    private Response buildResponse(IamProxyClient.ProxyResponse proxyResponse) {
        Response.ResponseBuilder builder = Response.status(proxyResponse.statusCode)
                .entity(proxyResponse.body)
                .type(MediaType.APPLICATION_JSON);

        // Forward relevant headers
        if (proxyResponse.headers != null) {
            proxyResponse.headers.forEach((key, values) -> {
                if (key != null && !key.equalsIgnoreCase("content-length")
                        && !key.equalsIgnoreCase("transfer-encoding")) {
                    values.forEach(v -> builder.header(key, v));
                }
            });
        }

        return builder.build();
    }
}
