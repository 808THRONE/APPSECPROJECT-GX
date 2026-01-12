# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM quay.io/wildfly/wildfly:38.0.0.Final-jdk21

RUN /opt/jboss/wildfly/bin/add-user.sh admin Admin#001 --silent

COPY --from=build /app/target/api-gateway.war /opt/jboss/wildfly/standalone/deployments/

EXPOSE 8080 9990

CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]
