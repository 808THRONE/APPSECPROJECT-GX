package com.securegate.iam.repository;

import com.securegate.iam.model.Policy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PolicyRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Policy save(Policy policy) {
        if (policy.getPolicyId() == null) {
            em.persist(policy);
            return policy;
        } else {
            return em.merge(policy);
        }
    }

    public Optional<Policy> findById(UUID id) {
        return Optional.ofNullable(em.find(Policy.class, id));
    }

    public List<Policy> findAll() {
        return em.createQuery("SELECT p FROM Policy p", Policy.class).getResultList();
    }

    @Transactional
    public void delete(UUID id) {
        findById(id).ifPresent(em::remove);
    }
}
