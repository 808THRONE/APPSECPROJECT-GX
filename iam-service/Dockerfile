FROM quay.io/wildfly/wildfly:38.0.1.Final-jdk21

# Create admin user for management
RUN /opt/jboss/wildfly/bin/add-user.sh admin Admin#123 --silent

# Deploy artifact
COPY target/iam-service.war /opt/jboss/wildfly/standalone/deployments/

# Set environment variables (Defaults, can be overridden by docker-compose)
ENV PASETO_SECRET_KEY=ReplaceThisWithA32CharRandomKeyInProduction123
ENV POSTGRES_URL=jdbc:postgresql://postgres:5432/securegate
ENV POSTGRES_USER=securegate
ENV POSTGRES_PASSWORD=securegate_password

# Expose ports
EXPOSE 8080 9990
