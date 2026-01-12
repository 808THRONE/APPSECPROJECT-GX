package com.securegate.iam.repository;

import com.securegate.iam.model.SystemSetting;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SystemSettingRepository {

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SystemSetting save(SystemSetting setting) {
        if (em.find(SystemSetting.class, setting.getSettingKey()) == null) {
            em.persist(setting);
            return setting;
        } else {
            return em.merge(setting);
        }
    }

    public Optional<SystemSetting> findByKey(String key) {
        return Optional.ofNullable(em.find(SystemSetting.class, key));
    }

    public List<SystemSetting> findAll() {
        return em.createQuery("SELECT s FROM SystemSetting s", SystemSetting.class).getResultList();
    }
}
