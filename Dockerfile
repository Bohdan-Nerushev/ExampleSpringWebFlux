# Stage 1: Build application JAR
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime container
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/webflux-order-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+AllowRedefinitionToAddDeleteMethods", "-jar", "app.jar"]
