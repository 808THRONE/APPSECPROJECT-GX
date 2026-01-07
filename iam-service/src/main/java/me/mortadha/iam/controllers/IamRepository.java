package me.mortadha.iam.controllers;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import me.mortadha.iam.entities.Grant;
import me.mortadha.iam.entities.Identity;
import me.mortadha.iam.entities.Tenant;

import java.util.Optional;
import java.util.logging.Logger;

/**
 * Repository for IAM operations
 */
@Singleton
public class IamRepository {

    private static final Logger LOGGER = Logger.getLogger(IamRepository.class.getName());

    @Inject
    private EntityManager entityManager;

    /**
     * Find tenant by tenant ID (client_id)
     */
    public Optional<Tenant> findTenantById(String tenantId) {
        try {
            Tenant tenant = entityManager
                .createQuery("SELECT t FROM Tenant t WHERE t.tenantId = :id", Tenant.class)
                .setParameter("id", tenantId)
                .getSingleResult();
            return Optional.of(tenant);
        } catch (NoResultException e) {
            LOGGER.fine("Tenant not found: " + tenantId);
            return Optional.empty();
        }
    }

    /**
     * Find identity by username
     */
    public Optional<Identity> findIdentityByUsername(String username) {
        try {
            Identity identity = entityManager
                .createQuery("SELECT i FROM Identity i WHERE i.username = :username", Identity.class)
                .setParameter("username", username)
                .getSingleResult();
            return Optional.of(identity);
        } catch (NoResultException e) {
            LOGGER.fine("Identity not found: " + username);
            return Optional.empty();
        }
    }

    /**
     * Find grant (user consent) for tenant and identity
     */
    public Optional<Grant> findGrant(Short tenantId, Long identityId) {
        try {
            Grant grant = entityManager
                .createQuery("SELECT g FROM Grant g WHERE g.id.tenantId = :tid AND g.id.identityId = :iid", Grant.class)
                .setParameter("tid", tenantId)
                .setParameter("iid", identityId)
                .getSingleResult();
            return Optional.of(grant);
        } catch (NoResultException e) {
            LOGGER.fine("Grant not found for tenant=" + tenantId + ", identity=" + identityId);
            return Optional.empty();
        }
    }

    /**
     * Save a new grant (user consent)
     */
    public void saveGrant(Grant grant) {
        entityManager.persist(grant);
        LOGGER.info("Grant saved: tenant=" + grant.getId().getTenantId() + 
                   ", identity=" + grant.getId().getIdentityId());
    }

    /**
     * Get roles for a user as string array
     */
    public String[] getRolesForIdentity(String username) {
        try {
            Long rolesMask = entityManager
                .createQuery("SELECT i.roles FROM Identity i WHERE i.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
            
            return RoleMapper.rolesToStringArray(rolesMask);
            
        } catch (NoResultException e) {
            LOGGER.warning("Identity not found for roles: " + username);
            return new String[0];
        }
    }

    /**
     * Role bit mapping utility
     */
    public static class RoleMapper {
        public static final long SURFER = 1L;       // 001
        public static final long MODERATOR = 2L;    // 010
        public static final long ADMINISTRATOR = 4L; // 100

        public static String[] rolesToStringArray(Long rolesMask) {
            if (rolesMask == null) {
                return new String[0];
            }

            java.util.List<String> roles = new java.util.ArrayList<>();
            
            if ((rolesMask & SURFER) != 0) {
                roles.add("Surfer");
            }
            if ((rolesMask & MODERATOR) != 0) {
                roles.add("Moderator");
            }
            if ((rolesMask & ADMINISTRATOR) != 0) {
                roles.add("Administrator");
            }
            
            return roles.toArray(new String[0]);
        }

        public static Long stringArrayToRoles(String[] roles) {
            long mask = 0L;
            for (String role : roles) {
                switch (role) {
                    case "Surfer" -> mask |= SURFER;
                    case "Moderator" -> mask |= MODERATOR;
                    case "Administrator" -> mask |= ADMINISTRATOR;
                }
            }
            return mask;
        }
    }
}
