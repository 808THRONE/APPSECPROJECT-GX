FROM quay.io/wildfly/wildfly:30.0.1.Final-jdk17

USER root

USER jboss

# Deploy
COPY target/api-gateway.war /opt/jboss/wildfly/standalone/deployments/api.war

# Expose ports (internally 8080)
EXPOSE 8080 9990

# Command to run
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
