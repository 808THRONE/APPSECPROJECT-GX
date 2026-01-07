FROM quay.io/wildfly/wildfly:30.0.1.Final-jdk17

USER root

# Install postgresql driver module later if needed, but for now just deployment
# Create customization dir
RUN mkdir -p /opt/jboss/wildfly/customization

USER jboss

# Download Postgres Driver
USER root
RUN curl -L -o /tmp/postgresql-42.6.0.jar https://jdbc.postgresql.org/download/postgresql-42.6.0.jar

USER jboss

# Configure WildFly
COPY commands.cli /tmp/commands.cli
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/tmp/commands.cli && \
    rm -rf /opt/jboss/wildfly/standalone/configuration/standalone_xml_history

# Deploy
COPY target/iam-service.war /opt/jboss/wildfly/standalone/deployments/iam.war

# Expose ports
EXPOSE 8080 9990

# Command to run (standard)
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
