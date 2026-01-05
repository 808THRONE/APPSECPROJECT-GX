package me.mortadha.iam.rest;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

import java.util.logging.Logger;

/**
 * JAX-RS Application configuration
 */
@ApplicationPath("/")
@ApplicationScoped
public class IamApplication extends Application {

    @Inject
    private Logger logger;

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("IAM Service REST API initialized at /");
    }
}
