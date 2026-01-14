package com.securegate.ui.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import lombok.Data;
import lombok.extern.java.Log;
import org.primefaces.model.file.UploadedFile;

@Named
@RequestScoped
@Data
@Log
public class StegoController {

    @Inject
    private UserSession userSession;

    private UploadedFile file;
    private String secretMessage;
    private String passphrase;
    private String result;

    private int stegoLength; // To store result length for extraction

    public void embed() {
        if (file == null || secretMessage == null || secretMessage.isEmpty()) {
            addMessage(FacesMessage.SEVERITY_ERROR, "Error", "Please provide an image and a message.");
            return;
        }

        try {
            // 1. Extract cover pixels (simplified to first 1000 pixels for demo
            // performance)
            int[] cover = extractPixels(file.getInputStream(), 2000);

            // 2. Wrap in JSON request
            Jsonb jsonb = JsonbBuilder.create();
            String payload = jsonb.toJson(new EmbedRequest(cover, secretMessage));

            String gatewayBase = System.getProperty("PUBLIC_BASE_URL");
            if (gatewayBase == null)
                gatewayBase = System.getenv("PUBLIC_BASE_URL");
            if (gatewayBase == null)
                gatewayBase = "https://mortadha.me";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayBase + "/api-gateway/api/v1/stego/embed"))
                    .header("Authorization", "Bearer " + userSession.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                EmbedResponse res = jsonb.fromJson(response.body(), EmbedResponse.class);
                this.stegoLength = res.length;
                this.result = "Success! Message embedded via STC Engine. Stego Bits Length: " + res.length;
                // Store stego bits in session/cache for extraction demo?
                userSession.setLastStegoBits(res.stego);
                userSession.setLastStegoLength(res.length);
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Stego embedding complete.");
            } else {
                this.result = "Error: " + response.body();
                addMessage(FacesMessage.SEVERITY_ERROR, "API Error", response.body());
            }
        } catch (Exception e) {
            this.result = "Critical Error: " + e.getMessage();
            log.severe("Stego error: " + e.getMessage());
        }
    }

    public void extract() {
        if (userSession.getLastStegoBits() == null) {
            result = "Error: Please embed a message first to have stego-bits available for extraction logic.";
            return;
        }

        try {
            Jsonb jsonb = JsonbBuilder.create();
            String payload = jsonb
                    .toJson(new ExtractRequest(userSession.getLastStegoBits(), userSession.getLastStegoLength()));

            String gatewayBase = System.getProperty("PUBLIC_BASE_URL");
            if (gatewayBase == null)
                gatewayBase = System.getenv("PUBLIC_BASE_URL");
            if (gatewayBase == null)
                gatewayBase = "https://mortadha.me";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(gatewayBase + "/api-gateway/api/v1/stego/extract"))
                    .header("Authorization", "Bearer " + userSession.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ExtractResponse res = jsonb.fromJson(response.body(), ExtractResponse.class);
                this.result = "Extracted Message: '" + res.payload + "'";
                addMessage(FacesMessage.SEVERITY_INFO, "Success", "Extraction complete.");
            } else {
                this.result = "Error: " + response.body();
            }
        } catch (Exception e) {
            this.result = "Error: " + e.getMessage();
        }
    }

    private int[] extractPixels(InputStream is, int limit) throws Exception {
        BufferedImage img = ImageIO.read(is);
        int w = img.getWidth();
        int h = img.getHeight();
        int count = Math.min(w * h, limit);
        int[] pixels = new int[count];
        int k = 0;
        for (int y = 0; y < h && k < count; y++) {
            for (int x = 0; x < w && k < count; x++) {
                pixels[k++] = img.getRGB(x, y) & 0xFF; // Use Blue channel for simplicity
            }
        }
        return pixels;
    }

    private void addMessage(FacesMessage.Severity severity, String summary, String detail) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severity, summary, detail));
    }

    // DTOs
    public static class EmbedRequest {
        public int[] cover;
        public String payload;

        public EmbedRequest(int[] cover, String payload) {
            this.cover = cover;
            this.payload = payload;
        }
    }

    public static class EmbedResponse {
        public int[] stego;
        public int length;
    }

    public static class ExtractRequest {
        public int[] stego;
        public int length;

        public ExtractRequest(int[] stego, int length) {
            this.stego = stego;
            this.length = length;
        }
    }

    public static class ExtractResponse {
        public String payload;
    }
}
