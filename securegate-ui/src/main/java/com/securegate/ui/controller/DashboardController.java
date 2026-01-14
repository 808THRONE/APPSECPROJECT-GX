package com.securegate.ui.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.Data;
import lombok.extern.java.Log;

@Named
@RequestScoped
@Data
@Log
public class DashboardController {

    @Inject
    private UserSession userSession;

    private String apiResponse;

    public void callUserInfoApi() {
        if (!userSession.isLoggedIn()) {
            apiResponse = "Not logged in.";
            return;
        }

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/iam-service/api/oauth2/userinfo"))
                    .header("Authorization", "Bearer " + userSession.getAccessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            this.apiResponse = "API Response (" + response.statusCode() + "): " + response.body();
        } catch (Exception e) {
            this.apiResponse = "Error calling API: " + e.getMessage();
            log.severe(e.getMessage());
        }
    }
}
