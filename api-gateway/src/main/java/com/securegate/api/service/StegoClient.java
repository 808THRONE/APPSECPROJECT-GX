package com.securegate.api.service;

import com.securegate.api.config.ApiGatewayConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * HTTP Client for Stego Module communication.
 * Provides steganographic embedding and extraction for secure data
 * transmission.
 */
@ApplicationScoped
public class StegoClient {

    private static final Logger LOGGER = Logger.getLogger(StegoClient.class.getName());

    @Inject
    private ApiGatewayConfig config;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Embed a payload into cover data using STC steganography.
     * 
     * @param cover   The cover data (e.g., image pixel values)
     * @param payload The sensitive payload to embed
     * @return The stego data with embedded payload
     */
    public int[] embedPayload(int[] cover, String payload) {
        if (!config.isStegoEnabled()) {
            LOGGER.info("Stego disabled, returning raw encoding");
            return encodeString(payload);
        }

        try {
            String requestBody = buildEmbedRequest(cover, payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getStegoServiceUrl() + "/stego/embed"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.warning("Stego embed failed: " + response.statusCode());
                return encodeString(payload);
            }

            return parseStegoResponse(response.body());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Stego embed error, falling back to raw encoding", e);
            return encodeString(payload);
        }
    }

    /**
     * Extract a payload from stego data.
     * 
     * @param stego  The stego data containing hidden payload
     * @param length Expected length of message bits
     * @return The extracted and decrypted payload
     */
    public String extractPayload(int[] stego, int length) {
        if (!config.isStegoEnabled()) {
            LOGGER.info("Stego disabled, decoding raw data");
            return decodeString(stego);
        }

        try {
            String requestBody = buildExtractRequest(stego, length);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getStegoServiceUrl() + "/stego/extract"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .timeout(Duration.ofSeconds(30))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                LOGGER.warning("Stego extract failed: " + response.statusCode());
                return decodeString(stego);
            }

            return response.body();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Stego extract error, falling back to raw decoding", e);
            return decodeString(stego);
        }
    }

    /**
     * Generate random cover data suitable for embedding.
     */
    public int[] generateCover(int size) {
        int[] cover = new int[size];
        java.security.SecureRandom random = new java.security.SecureRandom();
        for (int i = 0; i < size; i++) {
            cover[i] = random.nextInt(256);
        }
        return cover;
    }

    /**
     * Securely transmit sensitive data using steganography.
     */
    public StegoTransmission secureTransmit(String sensitiveData) {
        int coverSize = sensitiveData.length() * 8 + 256;
        int[] cover = generateCover(coverSize);
        int[] stego = embedPayload(cover, sensitiveData);
        return new StegoTransmission(stego, sensitiveData.length() * 8);
    }

    // Helper methods
    private String buildEmbedRequest(int[] cover, String payload) {
        StringBuilder sb = new StringBuilder("{\"cover\":[");
        for (int i = 0; i < cover.length; i++) {
            sb.append(cover[i]);
            if (i < cover.length - 1)
                sb.append(",");
        }
        sb.append("],\"payload\":\"").append(escapeJson(payload)).append("\"}");
        return sb.toString();
    }

    private String buildExtractRequest(int[] stego, int length) {
        StringBuilder sb = new StringBuilder("{\"stego\":[");
        for (int i = 0; i < stego.length; i++) {
            sb.append(stego[i]);
            if (i < stego.length - 1)
                sb.append(",");
        }
        sb.append("],\"length\":").append(length).append("}");
        return sb.toString();
    }

    private int[] parseStegoResponse(String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject obj = reader.readObject();
            var arr = obj.getJsonArray("stego");
            int[] result = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                result[i] = arr.getInt(i);
            }
            return result;
        }
    }

    private int[] encodeString(String data) {
        byte[] bytes = data.getBytes();
        int[] result = new int[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = bytes[i] & 0xFF;
        }
        return result;
    }

    private String decodeString(int[] data) {
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        return new String(bytes);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Inner class for stego transmission result
    public static class StegoTransmission {
        public final int[] stego;
        public final int length;

        public StegoTransmission(int[] stego, int length) {
            this.stego = stego;
            this.length = length;
        }
    }

    /**
     * Attempt to extract payload from a potential JSON stego object.
     * If the string is not a valid stego JSON, returns the original string.
     */
    public String extractFromStegoJson(String jsonBody) {
        if (!config.isStegoEnabled() || jsonBody == null || jsonBody.isEmpty()) {
            return jsonBody;
        }

        try (JsonReader reader = Json.createReader(new StringReader(jsonBody))) {
            JsonObject obj = reader.readObject();
            if (!obj.containsKey("stego") || !obj.containsKey("length")) {
                return jsonBody;
            }

            var arr = obj.getJsonArray("stego");
            int[] stego = new int[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                stego[i] = arr.getInt(i);
            }
            int length = obj.getInt("length");

            return extractPayload(stego, length);
        } catch (Exception e) {
            // Not a JSON object or not stego format, treat as raw
            return jsonBody;
        }
    }
}
