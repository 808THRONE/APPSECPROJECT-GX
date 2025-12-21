package com.securegate.repositories;

import com.securegate.entities.Tenant;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class TenantRepository {
    private final Map<String, Tenant> store = new ConcurrentHashMap<>();

    public TenantRepository() {
        // Mock Data
        Set<String> redirectUris = new HashSet<>();
        redirectUris.add("https://oauth.pstmn.io/v1/callback");
        redirectUris.add("http://localhost:5173/callback");

        Tenant tenant = new Tenant("client-id-123", "client-secret-456", redirectUris, "Demo App");
        store.put(tenant.getClientId(), tenant);
        store.put("securegate-pwa", new Tenant("securegate-pwa", "pwa-secret", redirectUris, "SecureGate PWA"));
    }

    public Optional<Tenant> findByClientId(String clientId) {
        return Optional.ofNullable(store.get(clientId));
    }

    public boolean validateRedirectUri(String clientId, String redirectUri) {
        Tenant tenant = store.get(clientId);
        return tenant != null && tenant.getRedirectUris().contains(redirectUri);
    }
}
