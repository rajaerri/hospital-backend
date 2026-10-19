# Multi-stage build for Spring Boot with Java 21
# Stage 1: Build the JAR using Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and source files
COPY pom.xml .
COPY src ./src

# Package the application (skip tests for speed and reliability)
RUN mvn clean package -DskipTests

# Stage 2: Lightweight runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy built artifact from the build stage
COPY --from=build /app/target/*.jar app.jar

# Render injects PORT dynamically (defaults to 8080 if not set)
ENV PORT=8080
EXPOSE 8080

# Run Spring Boot with JVM flags tailored for Render Free Tier (512MB RAM)
ENTRYPOINT ["java", "-XX:+UseSerialGC", "-Xss512k", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
