package com.securegate.iam.repository;

import com.securegate.iam.model.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class UserRepository {

    @PersistenceContext(unitName = "iam-pu")
    private EntityManager em;

    public Optional<User> findByUsername(String username) {
        try {
            return Optional.of(em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public java.util.Optional<User> findById(java.util.UUID id) {
        return java.util.Optional.ofNullable(em.find(User.class, id));
    }

    @Transactional
    public void delete(java.util.UUID id) {
        User user = em.find(User.class, id);
        if (user != null) {
            em.remove(user);
        }
    }

    public Optional<User> findByEmail(String email) {
        try {
            return Optional.of(em.createQuery("SELECT u FROM User u WHERE u.email = :email", User.class)
                    .setParameter("email", email)
                    .getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    @Transactional
    public User save(User user) {
        if (user.getUserId() == null) {
            em.persist(user);
            return user;
        } else {
            return em.merge(user);
        }
    }

    public java.util.List<User> findAll() {
        return em.createQuery("SELECT u FROM User u", User.class).getResultList();
    }
}
