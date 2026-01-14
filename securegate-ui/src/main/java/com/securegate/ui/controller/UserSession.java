package com.securegate.ui.controller;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import lombok.Data;
import java.io.Serializable;

@Named
@SessionScoped
@Data
public class UserSession implements Serializable {
    private String accessToken;
    private String refreshToken;
    private String username;
    private String userId;
    private boolean mfaEnabled;

    // Stego Transient Storage
    private int[] lastStegoBits;
    private int lastStegoLength;
    private java.util.List<String> roles = new java.util.ArrayList<>();

    public boolean isLoggedIn() {
        return accessToken != null;
    }

    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }

    public void logout() {
        this.accessToken = null;
        this.refreshToken = null;
        this.username = null;
    }
}
