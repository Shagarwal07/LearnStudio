# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# 1. Copy only the pom.xml and download dependencies (cached layer)
COPY backend/pom.xml ./backend/
RUN mvn -f backend/pom.xml dependency:go-offline -B

# 2. Copy the rest of the backend source
COPY backend/src ./backend/src/

# Copy frontend files into the Spring Boot static resources directory
# This allows Spring Boot to serve the UI at the root context (/)
COPY css/ ./backend/src/main/resources/static/css/
COPY js/ ./backend/src/main/resources/static/js/
COPY *.html ./backend/src/main/resources/static/

# Build the JAR file
RUN mvn -f backend/pom.xml clean package -DskipTests

# Stage 2: Create the final production image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy only the built JAR from the previous stage
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=${PORT:8080}", "-jar", "app.jar"]