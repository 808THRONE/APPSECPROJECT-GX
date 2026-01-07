package com.securegate.stego;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import java.io.StringReader;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Steganography REST API for secure covert data transmission.
 * 
 * Endpoints:
 * - POST /api/stego/embed - Embed secret into cover image
 * - POST /api/stego/extract - Extract secret from stego image
 * - GET /api/stego/capacity - Calculate capacity for image size
 * 
 * Use Cases:
 * - Covert transmission of audit logs
 * - Secure key transfer
 * - ABAC policy updates via hidden channels
 */
@Path("/stego")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SteganographyAPI {

        private static final Logger LOGGER = Logger.getLogger(SteganographyAPI.class.getName());

        private final AdaptiveSteganographyService stegoService;

        public SteganographyAPI() {
                this.stegoService = new AdaptiveSteganographyService();
        }

        /**
         * Embed secret data into cover image.
         * 
         * Request body:
         * {
         * "coverImage": "base64-encoded-image",
         * "secret": "base64-encoded-secret-data",
         * "password": "encryption-password"
         * }
         * 
         * Response:
         * {
         * "stegoImage": "base64-encoded-stego-image",
         * "psnr": 48.5,
         * "payloadSize": 1024,
         * "qualityOk": true
         * }
         */
        @POST
        @Path("/embed")
        public Response embed(String body) {
                try (JsonReader reader = Json.createReader(new StringReader(body))) {
                        JsonObject input = reader.readObject();

                        String coverImageB64 = input.getString("coverImage");
                        String secretB64 = input.getString("secret");
                        String password = input.getString("password");

                        byte[] coverImage = Base64.getDecoder().decode(coverImageB64);
                        byte[] secret = Base64.getDecoder().decode(secretB64);

                        LOGGER.info("Embed request: cover=" + coverImage.length + " bytes, secret=" + secret.length
                                        + " bytes");

                        // Check capacity
                        if (!stegoService.hasCapacity(coverImage.length, secret.length)) {
                                return Response.status(Response.Status.BAD_REQUEST)
                                                .entity(Json.createObjectBuilder()
                                                                .add("error", "insufficient_capacity")
                                                                .add("message", "Cover image too small for secret. Max capacity: "
                                                                                +
                                                                                stegoService.calculateMaxCapacity(
                                                                                                coverImage.length)
                                                                                + " bytes")
                                                                .build())
                                                .build();
                        }

                        // Perform embedding
                        AdaptiveSteganographyService.StegoResult result = stegoService.embedSecret(coverImage, secret,
                                        password);

                        String stegoImageB64 = Base64.getEncoder().encodeToString(result.stegoImage());

                        return Response.ok(Json.createObjectBuilder()
                                        .add("stegoImage", stegoImageB64)
                                        .add("psnr", result.psnr())
                                        .add("payloadSize", result.embeddedPayloadSize())
                                        .add("qualityOk", result.meetsQualityThreshold())
                                        .build())
                                        .build();

                } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid embed request: " + e.getMessage());
                        return Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Json.createObjectBuilder()
                                                        .add("error", "invalid_request")
                                                        .add("message", e.getMessage())
                                                        .build())
                                        .build();
                } catch (Exception e) {
                        LOGGER.severe("Embed failed: " + e.getMessage());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity(Json.createObjectBuilder()
                                                        .add("error", "embed_failed")
                                                        .add("message", e.getMessage())
                                                        .build())
                                        .build();
                }
        }

        /**
         * Extract secret data from stego image.
         * 
         * Request body:
         * {
         * "stegoImage": "base64-encoded-stego-image",
         * "password": "decryption-password"
         * }
         * 
         * Response:
         * {
         * "secret": "base64-encoded-secret-data",
         * "size": 1024
         * }
         */
        @POST
        @Path("/extract")
        public Response extract(String body) {
                try (JsonReader reader = Json.createReader(new StringReader(body))) {
                        JsonObject input = reader.readObject();

                        String stegoImageB64 = input.getString("stegoImage");
                        String password = input.getString("password");

                        byte[] stegoImage = Base64.getDecoder().decode(stegoImageB64);

                        LOGGER.info("Extract request: stegoImage=" + stegoImage.length + " bytes");

                        // Perform extraction
                        byte[] secret = stegoService.extractSecret(stegoImage, password);

                        String secretB64 = Base64.getEncoder().encodeToString(secret);

                        return Response.ok(Json.createObjectBuilder()
                                        .add("secret", secretB64)
                                        .add("size", secret.length)
                                        .build())
                                        .build();

                } catch (javax.crypto.AEADBadTagException e) {
                        LOGGER.warning("Decryption failed - wrong password or tampered data");
                        return Response.status(Response.Status.UNAUTHORIZED)
                                        .entity(Json.createObjectBuilder()
                                                        .add("error", "decryption_failed")
                                                        .add("message", "Invalid password or corrupted data")
                                                        .build())
                                        .build();
                } catch (IllegalArgumentException e) {
                        LOGGER.warning("Invalid extract request: " + e.getMessage());
                        return Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Json.createObjectBuilder()
                                                        .add("error", "invalid_request")
                                                        .add("message", e.getMessage())
                                                        .build())
                                        .build();
                } catch (Exception e) {
                        LOGGER.severe("Extract failed: " + e.getMessage());
                        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                                        .entity(Json.createObjectBuilder()
                                                        .add("error", "extract_failed")
                                                        .add("message", e.getMessage())
                                                        .build())
                                        .build();
                }
        }

        /**
         * Calculate capacity for a given image size.
         * 
         * GET /api/stego/capacity?size=1048576
         */
        @GET
        @Path("/capacity")
        public Response getCapacity(@QueryParam("size") int imageSize) {
                if (imageSize <= 0) {
                        return Response.status(Response.Status.BAD_REQUEST)
                                        .entity(Json.createObjectBuilder()
                                                        .add("error", "invalid_size")
                                                        .add("message", "Image size must be positive")
                                                        .build())
                                        .build();
                }

                int maxCapacity = stegoService.calculateMaxCapacity(imageSize);

                return Response.ok(Json.createObjectBuilder()
                                .add("imageSize", imageSize)
                                .add("maxSecretSize", maxCapacity)
                                .add("capacityPercent", (double) maxCapacity / imageSize * 100)
                                .build())
                                .build();
        }

        /**
         * Health check endpoint.
         */
        @GET
        @Path("/health")
        public Response health() {
                return Response.ok(Json.createObjectBuilder()
                                .add("status", "healthy")
                                .add("service", "stego-module")
                                .add("algorithm", "STC (Syndrome Trellis Codes)")
                                .add("encryption", "AES-256-GCM")
                                .build())
                                .build();
        }
}
