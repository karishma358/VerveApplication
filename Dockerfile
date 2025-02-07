# Use an official OpenJDK base image
FROM openjdk:11-jdk-slim

# Set working directory inside the container
WORKDIR /app

# Copy the built JAR file into the container
COPY target/VerveApplication-1.0-SNAPSHOT.jar app.jar

# Expose the application port
EXPOSE 8080

# Set environment variables for Kafka (optional, can be overridden in docker-compose)
ENV KAFKA_BOOTSTRAP_SERVERS=kafka:9092
ENV KAFKA_TOPIC=verve-unique-ids

# Command to run the Java application
CMD ["java", "-jar", "app.jar"]