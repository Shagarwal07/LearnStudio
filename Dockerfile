# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17-slim AS build
WORKDIR /app

# Copy the backend source and configuration
COPY backend/pom.xml ./backend/
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
ENTRYPOINT ["java", "-jar", "app.jar"]