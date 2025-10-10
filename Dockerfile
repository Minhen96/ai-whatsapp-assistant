# --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies first (for caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copy jar from builder stage
COPY --from=build /app/target/*.jar app.jar

# Force production profile
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
