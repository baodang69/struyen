# syntax=docker/dockerfile:1

# Stage 1: Dependency resolution and compilation
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /workspace

# Pre-cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Compile and package executable jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Lightweight production JRE runtime
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Install curl for container healthcheck
RUN apk add --no-cache curl

# Security: Create non-root system user and group
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy compiled jar from builder
COPY --from=builder /workspace/target/struyen-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]

