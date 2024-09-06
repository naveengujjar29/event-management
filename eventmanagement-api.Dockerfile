# Use the official Maven image to build the application
FROM maven:3.9.9-amazoncorretto-17 AS build
WORKDIR /app

# Copy the project files into the container
COPY . .

# Package the application
RUN mvn clean package -DskipTests

# Use the official OpenJDK image to run the Spring Boot app
FROM amazoncorretto:17-alpine3.20

# Set the working directory inside the image
WORKDIR /app

# Copy the built JAR file from the build stage
COPY --from=build /app/target/event-management-0.0.1-SNAPSHOT.jar event-management-0.0.1-SNAPSHOT.jar

# Expose the port the Spring Boot app will run on
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "event-management-0.0.1-SNAPSHOT.jar"]
