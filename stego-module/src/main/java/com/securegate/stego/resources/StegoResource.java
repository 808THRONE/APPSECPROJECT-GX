package com.securegate.stego.resources;

import com.securegate.stego.EncryptionService;
import com.securegate.stego.StcEngine;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/stego")
public class StegoResource {

    @Inject
    private StcEngine stcEngine;

    @Inject
    private EncryptionService encryptionService;

    private String getEncryptionKey() {
        String key = System.getenv("STEGO_ENCRYPTION_KEY");
        return (key != null) ? key : "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE="; // Fallback for dev
    }

    // In a real app, inputs would be multipart/form-data (Images)
    // Here we accept JSON arrays for logic demo

    @POST
    @Path("/embed")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response embed(EmbedRequest request) {
        try {
            // 1. Encrypt Payload
            String encrypted = encryptionService.encrypt(request.payload, getEncryptionKey());

            // 2. Convert to bits
            int[] messageBits = stcEngine.stringToBits(encrypted);

            // 3. Embed
            int[] stego = stcEngine.embed(request.cover, messageBits);

            StegoResponse response = new StegoResponse();
            response.setStego(stego);
            response.setLength(messageBits.length);
            return Response.ok(response).build();
        } catch (Exception e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    public static class StegoResponse {
        public int[] stego;
        public int length;

        public int[] getStego() {
            return stego;
        }

        public void setStego(int[] stego) {
            this.stego = stego;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    @POST
    @Path("/extract")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response extract(ExtractRequest request) {
        try {
            // 1. Extract from cover
            int[] messageBits = stcEngine.extract(request.stego, request.length);

            // 2. Convert bit array to string
            String encrypted = stcEngine.bitsToString(messageBits);

            // 3. Decrypt
            String decrypted = encryptionService.decrypt(encrypted, getEncryptionKey());

            return Response.ok(new ExtractResponse(decrypted)).build();
        } catch (Exception e) {
            return Response.serverError().entity(new ErrorResponse(e.getMessage())).build();
        }
    }

    public static class ExtractResponse {
        public String payload;

        public ExtractResponse() {
        }

        public ExtractResponse(String payload) {
            this.payload = payload;
        }
    }

    public static class ErrorResponse {
        public String error;

        public ErrorResponse() {
        }

        public ErrorResponse(String error) {
            this.error = error;
        }
    }

    public static class ExtractRequest {
        public int[] stego;
        public int length;

        public int[] getStego() {
            return stego;
        }

        public void setStego(int[] stego) {
            this.stego = stego;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    public static class EmbedRequest {
        public int[] cover;
        public String payload;

        public int[] getCover() {
            return cover;
        }

        public void setCover(int[] cover) {
            this.cover = cover;
        }

        public String getPayload() {
            return payload;
        }

        public void setPayload(String payload) {
            this.payload = payload;
        }
    }
}
