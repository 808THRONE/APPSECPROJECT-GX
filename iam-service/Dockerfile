# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Download Postgres Driver
RUN mvn dependency:get -DgroupId=org.postgresql -DartifactId=postgresql -Dversion=42.7.1 -Dtransitive=false -Ddest=postgresql-42.7.1.jar

RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM quay.io/wildfly/wildfly:38.0.0.Final-jdk21

# Create admin user for management
RUN /opt/jboss/wildfly/bin/add-user.sh admin Admin#001 --silent

USER root
# Create module directory
RUN mkdir -p /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main

# Copy driver and module.xml
COPY --from=build /app/postgresql-42.7.1.jar /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/
COPY docker-scripts/module.xml /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main/
RUN chown -R jboss:root /opt/jboss/wildfly/modules/system/layers/base/org/postgresql/main

USER jboss

# Configure Datasource
COPY docker-scripts/setup-postgres.cli /opt/jboss/wildfly/standalone/configuration/
RUN /opt/jboss/wildfly/bin/jboss-cli.sh --file=/opt/jboss/wildfly/standalone/configuration/setup-postgres.cli

# Copy WAR from build stage
COPY --from=build /app/target/iam-service.war /opt/jboss/wildfly/standalone/deployments/

# Expose ports
EXPOSE 8080 9990

# Command to run (standalone is default)
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
