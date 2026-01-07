FROM quay.io/wildfly/wildfly:30.0.0.Final-jdk21

# Maintainer info
LABEL maintainer="mortadha.me"

# User as in other modules
USER jboss

# Copy the WAR file to deployments directory
COPY target/stego-module.war /opt/jboss/wildfly/standalone/deployments/

# Expose HTTP and management ports
EXPOSE 8080 9990

# Start WildFly in standalone mode
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
