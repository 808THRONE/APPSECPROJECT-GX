package com.securegate.repositories;

import com.securegate.entities.Tenant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class TenantRepository {

    @PersistenceContext(unitName = "iamPU")
    private EntityManager em;

    // No constructor initialization - data should come from DB

    public Optional<Tenant> findByClientId(String clientId) {
        return Optional.ofNullable(em.find(Tenant.class, clientId));
    }

    public boolean validateRedirectUri(String clientId, String redirectUri) {
        Tenant tenant = em.find(Tenant.class, clientId);
        return tenant != null && tenant.getRedirectUris().contains(redirectUri);
    }

    @Transactional
    public void save(Tenant tenant) {
        em.persist(tenant);
    }
}
