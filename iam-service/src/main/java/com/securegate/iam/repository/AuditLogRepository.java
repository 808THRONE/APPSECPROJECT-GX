package com.securegate.iam.repository;

import com.securegate.iam.model.AuditLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class AuditLogRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void save(AuditLog log) {
        em.persist(log);
    }

    public List<AuditLog> findAll() {
        return em.createQuery("SELECT l FROM AuditLog l ORDER BY l.timestamp DESC", AuditLog.class).getResultList();
    }
}
