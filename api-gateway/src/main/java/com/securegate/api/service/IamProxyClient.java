package com.securegate.api.service;

import com.securegate.api.config.ApiGatewayConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP Client for proxying requests to IAM Service.
 * Routes all IAM-related API calls through the gateway.
 */
@ApplicationScoped
public class IamProxyClient {

    private static final Logger LOGGER = Logger.getLogger(IamProxyClient.class.getName());

    @Inject
    private ApiGatewayConfig config;

    @Inject
    private StegoClient stegoClient;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Proxy a GET request to IAM service.
     */
    public ProxyResponse proxyGet(String path, Map<String, String> headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getIamServiceUrl() + path))
                    .GET()
                    .timeout(Duration.ofSeconds(30));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new ProxyResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Proxy GET failed for: " + path, e);
            return new ProxyResponse(502, "{\"error\":\"Gateway error\"}", Map.of());
        }
    }

    /**
     * Proxy a POST request to IAM service.
     */
    public ProxyResponse proxyPost(String path, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getIamServiceUrl() + path))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new ProxyResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Proxy POST failed for: " + path, e);
            return new ProxyResponse(502, "{\"error\":\"Gateway error\"}", Map.of());
        }
    }

    /**
     * Proxy a PUT request to IAM service.
     */
    public ProxyResponse proxyPut(String path, String body, Map<String, String> headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getIamServiceUrl() + path))
                    .PUT(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new ProxyResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Proxy PUT failed for: " + path, e);
            return new ProxyResponse(502, "{\"error\":\"Gateway error\"}", Map.of());
        }
    }

    /**
     * Proxy a DELETE request to IAM service.
     */
    public ProxyResponse proxyDelete(String path, Map<String, String> headers) {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getIamServiceUrl() + path))
                    .DELETE()
                    .timeout(Duration.ofSeconds(30));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new ProxyResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Proxy DELETE failed for: " + path, e);
            return new ProxyResponse(502, "{\"error\":\"Gateway error\"}", Map.of());
        }
    }

    /**
     * Proxy with stego-enhanced response for sensitive data.
     */
    public ProxyResponse proxyGetWithStego(String path, Map<String, String> headers) {
        ProxyResponse response = proxyGet(path, headers);
        return enhanceResponseWithStego(response);
    }

    public ProxyResponse proxyPostWithStego(String path, String body, Map<String, String> headers) {
        ProxyResponse response = proxyPost(path, body, headers);
        return enhanceResponseWithStego(response);
    }

    public ProxyResponse proxyPutWithStego(String path, String body, Map<String, String> headers) {
        ProxyResponse response = proxyPut(path, body, headers);
        return enhanceResponseWithStego(response);
    }

    public ProxyResponse proxyDeleteWithStego(String path, Map<String, String> headers) {
        ProxyResponse response = proxyDelete(path, headers);
        return enhanceResponseWithStego(response);
    }

    private ProxyResponse enhanceResponseWithStego(ProxyResponse response) {
        if (response.statusCode >= 200 && response.statusCode < 300 && config.isStegoEnabled() && response.body != null
                && !response.body.isEmpty()) {
            try {
                // Embed the response in stego carrier
                StegoClient.StegoTransmission transmission = stegoClient.secureTransmit(response.body);

                // Create stego response JSON
                StringBuilder sb = new StringBuilder("{\"stego\":[");
                for (int i = 0; i < transmission.stego.length; i++) {
                    sb.append(transmission.stego[i]);
                    if (i < transmission.stego.length - 1)
                        sb.append(",");
                }
                sb.append("],\"length\":").append(transmission.length).append("}");

                return new ProxyResponse(response.statusCode, sb.toString(), response.headers);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to embed response in stego", e);
                return response;
            }
        }
        return response;
    }

    // Response wrapper
    public static class ProxyResponse {
        public final int statusCode;
        public final String body;
        public final Map<String, java.util.List<String>> headers;

        public ProxyResponse(int statusCode, String body, Map<String, java.util.List<String>> headers) {
            this.statusCode = statusCode;
            this.body = body;
            this.headers = headers;
        }
    }
}
