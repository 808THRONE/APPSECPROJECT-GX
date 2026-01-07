package com.securegate.repositories;

import com.securegate.entities.Grant;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class GrantRepository {

    @PersistenceContext(unitName = "iamPU")
    private EntityManager em;

    @Transactional
    public void save(Grant grant) {
        em.persist(grant);
    }

    @Transactional
    public Optional<Grant> consume(String code) {
        Grant grant = em.find(Grant.class, code);
        if (grant != null) {
            em.remove(grant);
            return Optional.of(grant);
        }
        return Optional.empty();
    }
}
