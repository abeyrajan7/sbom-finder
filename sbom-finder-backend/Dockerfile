# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set working directory
WORKDIR /app

# Copy Maven build files
COPY target/*.jar app.jar

# Expose port 8080
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
