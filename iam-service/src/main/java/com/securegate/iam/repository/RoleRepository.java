package com.securegate.iam.repository;

import com.securegate.iam.model.Role;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.Optional;

@ApplicationScoped
public class RoleRepository {

    @PersistenceContext
    private EntityManager em;

    public Optional<Role> findByName(String name) {
        return em.createQuery("SELECT r FROM Role r WHERE r.roleName = :name", Role.class)
                .setParameter("name", name)
                .getResultStream()
                .findFirst();
    }

    @Transactional
    public Role save(Role role) {
        if (role.getRoleId() == null) {
            em.persist(role);
            return role;
        } else {
            return em.merge(role);
        }
    }
}
