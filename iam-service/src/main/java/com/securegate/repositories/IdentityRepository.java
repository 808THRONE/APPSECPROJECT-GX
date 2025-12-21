package com.securegate.repositories;

import com.securegate.entities.Identity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class IdentityRepository {
    private final Map<String, Identity> store = new ConcurrentHashMap<>();

    public IdentityRepository() {
        // Mock Data
        Set<String> roles = new HashSet<>();
        roles.add("admin");
        roles.add("user");
        Identity admin = new Identity("1", "admin", "password", roles); // In real app, hash password
        store.put(admin.getUsername(), admin);

        Set<String> userRoles = new HashSet<>();
        userRoles.add("user");
        Identity user = new Identity("2", "user", "password", userRoles);
        store.put(user.getUsername(), user);
    }

    public Optional<Identity> findByUsername(String username) {
        return Optional.ofNullable(store.get(username));
    }

    public boolean verifyCredentials(String username, String password) {
        // Plain text comparison for mock purposes. In PROD use BCrypt.
        Identity identity = store.get(username);
        return identity != null && identity.getPasswordHash().equals(password);
    }
}
