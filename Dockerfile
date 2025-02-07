# Stage 1: Build the application
FROM maven:3.8.5-openjdk-11-slim AS build
WORKDIR /app
COPY . .
RUN mvn clean package

# Add this line to list the contents of the target directory
RUN ls -la /app/target

# Stage 2: Run the application
FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /target/VerveApplication-1.0-SNAPSHOT.jar VerveProject-1.0-SNAPSHOT.jar
CMD ["java", "-jar", "VerveProject-1.0-SNAPSHOT.jar"]