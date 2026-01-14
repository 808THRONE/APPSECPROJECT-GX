package com.securegate.ui.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.Data;

@Named
@RequestScoped
@Data
public class SecuritySettingsController {

    @Inject
    private UserSession userSession;

    private String mfaSecret;
    private String qrCodeUrl;
    private String verificationCode;
    private String message;

    public void setupMfa() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "userId=" + userSession.getUserId();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/iam-service/api/mfa/setup"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String json = response.body();
                this.mfaSecret = json.split("\"secret\":\"")[1].split("\"")[0];
                this.qrCodeUrl = json.split("\"qr\":\"")[1].split("\"")[0];
            }
        } catch (Exception e) {
            this.message = "Setup failed: " + e.getMessage();
        }
    }

    public void verifyAndEnable() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            String body = "userId=" + userSession.getUserId() + "&code=" + verificationCode;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(java.net.URI.create("http://localhost:8080/iam-service/api/mfa/verify"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                userSession.setMfaEnabled(true);
                this.message = "MFA successfully enabled!";
            } else {
                this.message = "Invalid code. Please try again.";
            }
        } catch (Exception e) {
            this.message = "Verification failed: " + e.getMessage();
        }
    }
}
