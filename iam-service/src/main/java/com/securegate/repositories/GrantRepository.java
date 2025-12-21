package com.securegate.repositories;

import com.securegate.entities.Grant;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GrantRepository {
    private final Map<String, Grant> codeStore = new ConcurrentHashMap<>();

    public void save(Grant grant) {
        codeStore.put(grant.getCode(), grant);
    }

    public Optional<Grant> consume(String code) {
        return Optional.ofNullable(codeStore.remove(code));
    }
}
