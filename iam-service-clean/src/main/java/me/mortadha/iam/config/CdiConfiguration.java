package me.mortadha.iam.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Named;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.logging.Logger;

/**
 * CDI Producers for dependency injection
 */
@ApplicationScoped
public class CdiConfiguration {

    @PersistenceContext(unitName = "default")
    private EntityManager entityManager;

    @ConfigProperty(name = "jwt.realm", defaultValue = "urn:mortadha.me:iam")
    private String realm;

    /**
     * Produce EntityManager for injection
     */
    @Produces
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     * Produce realm name for injection
     */
    @Produces
    @Named("realm")
    public String getRealm() {
        return realm;
    }

    /**
     * Produce logger for injection
     */
    @Produces
    @Dependent
    public Logger getLogger(InjectionPoint injectionPoint) {
        return Logger.getLogger(
            injectionPoint.getMember().getDeclaringClass().getName()
        );
    }

    /**
     * Dispose logger
     */
    public void disposeLogger(@Disposes Logger logger) {
        // Cleanup if needed
    }
}
