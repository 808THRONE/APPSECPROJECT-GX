package com.securegate.repositories;

import com.securegate.entities.Identity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class IdentityRepository {

    @PersistenceContext(unitName = "iamPU")
    private EntityManager em;

    public Optional<Identity> findByUsername(String username) {
        return em.createQuery("SELECT i FROM Identity i WHERE i.username = :username", Identity.class)
                .setParameter("username", username)
                .getResultStream()
                .findFirst();
    }

    public boolean verifyCredentials(String username, String password) {
        // Plain text comparison for mock purposes.
        Optional<Identity> identity = findByUsername(username);
        return identity.isPresent() && identity.get().getPasswordHash().equals(password);
    }

    @Transactional
    public void save(Identity identity) {
        em.persist(identity);
    }
}
