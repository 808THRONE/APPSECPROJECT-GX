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
 * Proxy Client specifically for Stego Module communication.
 * Unlike IamProxyClient, this routes directly to the Stego service.
 */
@ApplicationScoped
public class StegoProxyClient {

    private static final Logger LOGGER = Logger.getLogger(StegoProxyClient.class.getName());

    @Inject
    private ApiGatewayConfig config;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Proxy a POST request to Stego service.
     */
    public IamProxyClient.ProxyResponse proxyPost(String path, String body, Map<String, String> headers) {
        try {
            LOGGER.info("Proxying stego request to: " + config.getStegoServiceUrl() + path);
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(config.getStegoServiceUrl() + path))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(30));

            headers.forEach(requestBuilder::header);

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return new IamProxyClient.ProxyResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Stego proxy post failed for: " + path, e);
            return new IamProxyClient.ProxyResponse(502, "{\"error\":\"Stego Gateway error\"}", Map.of());
        }
    }
}
