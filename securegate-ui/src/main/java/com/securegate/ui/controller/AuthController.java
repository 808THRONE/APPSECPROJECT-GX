package com.securegate.ui.controller;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import lombok.extern.java.Log;

@Named
@RequestScoped
@Log
public class AuthController {

    @Inject
    private UserSession userSession;

    private static final String IAM_URL;
    private static final String CLIENT_ID = "securegate-frontend";
    private static final String REDIRECT_URI;

    static {
        String base = System.getProperty("PUBLIC_BASE_URL");
        if (base == null)
            base = System.getenv("PUBLIC_BASE_URL");
        if (base == null)
            base = "https://mortadha.me";

        String iam = System.getProperty("iam.service.url");
        if (iam == null)
            iam = System.getenv("iam.service.url");
        if (iam == null)
            iam = base + "/iam-service/api/oauth2";
        else if (iam.endsWith("/api"))
            iam += "/oauth2";

        IAM_URL = iam;
        REDIRECT_URI = base + "/callback.xhtml";
    }

    private String code;

    public void login() throws Exception {
        // Redirect to IAM Authorize
        String authUrl = String.format(
                "%s/authorize?response_type=code&client_id=%s&redirect_uri=%s&scope=openid&state=123&code_challenge=mock&code_challenge_method=S256",
                IAM_URL, CLIENT_ID, URLEncoder.encode(REDIRECT_URI, StandardCharsets.UTF_8));

        FacesContext.getCurrentInstance().getExternalContext().redirect(authUrl);
    }

    public void handleCallback() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest();
        String codeParam = request.getParameter("code");

        if (codeParam != null && !codeParam.isEmpty()) {
            try {
                exchangeToken(codeParam);
                FacesContext.getCurrentInstance().getExternalContext().redirect("dashboard.xhtml");
            } catch (Exception e) {
                log.severe("Token exchange failed: " + e.getMessage());
                // Show error
            }
        }
    }

    public void logout() throws Exception {
        userSession.logout();
        FacesContext.getCurrentInstance().getExternalContext().invalidateSession();
        FacesContext.getCurrentInstance().getExternalContext().redirect("login.xhtml");
    }

    private void exchangeToken(String code) throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        String body = String.format(
                "grant_type=authorization_code&code=%s&redirect_uri=%s&client_id=%s&code_verifier=mock",
                code, REDIRECT_URI, CLIENT_ID);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(IAM_URL + "/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            // Very naive parsing for MVP
            String json = response.body();
            // Cookies are set in response headers, but if backend returns JSON body with
            // success...
            // Wait, OAuth2Resource returns COOKIES, not JSON body token?
            // "{\"status\":\"success\", \"username\":\"...\"}"
            // The cookies are in headers.

            // Extract Cookies from Response Headers
            response.headers().firstValue("Set-Cookie").ifPresent(cookieHeader -> {
                // This gets only ONE cookie if multple? Header map has list.
            });

            // For JSF Client, we need to extract the cookie from the HttpClient response
            // and store it in UserSession so we can use it for API calls.
            // Actually, we plan to use Browser Cookies?
            // If the redirection happens, the Browser never saw the IAM Response directly,
            // the SERVER did.
            // So the Server (WildFly) has the cookies. We need to pass them to the Browser.

            // PROBLEM: Generic OAuth2 client (Server-Side) usually gets JSON Body.
            // Our IAM Service sends HttpOnly Cookies.
            // This works for SPA (Browser calls Token).
            // For Server-Side Web App, HttpClient calls Token. The Cookies end up in the
            // Controller, not the Browser.
            // I need to MANUALLY forward these cookies to the JSF Response.

            response.headers().allValues("Set-Cookie").forEach(cookieVal -> {
                // Parse Name=Value
                String[] parts = cookieVal.split(";");
                String[] nameVal = parts[0].split("=");
                if (nameVal.length >= 2) {
                    if ("access_token".equals(nameVal[0]) || "refresh_token".equals(nameVal[0])) {
                        if ("access_token".equals(nameVal[0]))
                            userSession.setAccessToken(nameVal[1]);
                        // Also set as JSF Cookie?
                        // FacesContext.getCurrentInstance().getExternalContext().addResponseCookie(...)
                    }
                }
            });

            // Also set LoggedIn state
            if (userSession.getAccessToken() != null) {
                if (json.contains("\"username\":\"")) {
                    String u = json.split("\"username\":\"")[1].split("\"")[0];
                    userSession.setUsername(u);
                }
                if (json.contains("\"userId\":\"")) {
                    String id = json.split("\"userId\":\"")[1].split("\"")[0];
                    userSession.setUserId(id);
                }
                if (json.contains("\"mfaEnabled\":true")) {
                    userSession.setMfaEnabled(true);
                }

                // For simplicity in this demo, also parse roles if present in body
                if (json.contains("\"roles\":[")) {
                    String rolesPart = json.split("\"roles\":\\[")[1].split("\\]")[0];
                    String[] roleArr = rolesPart.replace("\"", "").split(",");
                    userSession.setRoles(java.util.Arrays.asList(roleArr));
                }
            }
        }
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
