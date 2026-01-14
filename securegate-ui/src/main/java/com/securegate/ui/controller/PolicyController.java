package com.securegate.ui.controller;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.Data;
import lombok.extern.java.Log;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import com.securegate.ui.model.Policy;

@Named
@RequestScoped
@Data
@Log
public class PolicyController {

    @Inject
    private UserSession userSession;

    private List<Policy> policies;
    private Policy newPolicy = new Policy();
    private String message;

    @PostConstruct
    public void init() {
        loadPolicies();
    }

    public void loadPolicies() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/iam-service/api/policies"))
                    .header("Authorization", "Bearer " + userSession.getAccessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Jsonb jsonb = JsonbBuilder.create();
                this.policies = jsonb.fromJson(response.body(), new java.util.ArrayList<Policy>() {
                }.getClass().getGenericSuperclass());
            }
        } catch (Exception e) {
            log.severe("Error loading policies: " + e.getMessage());
        }
    }

    public String savePolicy() {
        try {
            Jsonb jsonb = JsonbBuilder.create();
            String json = jsonb.toJson(newPolicy);
            log.info("Sending Policy JSON: " + json);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/iam-service/api/policies"))
                    .header("Authorization", "Bearer " + userSession.getAccessToken())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_INFO, "Success", "Policy created successfully"));
                return "policies?faces-redirect=true";
            } else {
                FacesContext.getCurrentInstance().addMessage(null,
                        new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", response.body()));
            }
        } catch (Exception e) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error", e.getMessage()));
        }
        return null;
    }
}
