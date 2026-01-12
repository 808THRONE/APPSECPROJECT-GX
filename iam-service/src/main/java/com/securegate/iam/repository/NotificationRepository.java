package com.securegate.iam.repository;

import com.securegate.iam.model.Notification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NotificationRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public Notification save(Notification notification) {
        if (notification.getNotificationId() == null) {
            em.persist(notification);
            return notification;
        } else {
            return em.merge(notification);
        }
    }

    public List<Notification> findAll() {
        return em.createQuery("SELECT n FROM Notification n ORDER BY n.createdAt DESC", Notification.class)
                .getResultList();
    }

    @Transactional
    public void markAsRead(UUID id) {
        Optional.ofNullable(em.find(Notification.class, id)).ifPresent(n -> n.setRead(true));
    }

    public Optional<Notification> findById(UUID id) {
        return Optional.ofNullable(em.find(Notification.class, id));
    }

    @Transactional
    public void delete(UUID id) {
        findById(id).ifPresent(n -> em.remove(n));
    }
}
