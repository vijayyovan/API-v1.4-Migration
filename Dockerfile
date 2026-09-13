# Use Eclipse Temurin JDK 17 as base image
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY .mvn mvn/
COPY mvnw pom.xml ./

# Download dependencies
RUN ./mvnw dependency:go-offline

# Copy source and build
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Runtime image
FROM gcr.io/distroless/java17-debian12:nonroot
WORKDIR /app
COPY --from=builder /app/target/*.jar /app/app.jar

# JVM container/runtime options can be overridden at runtime
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"

EXPOSE 8008
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
