FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre

WORKDIR /app
# Securing the container (TECH-04) by running as a non-root user
RUN addgroup --system app && adduser --system --ingroup app app
USER app

COPY --from=build /app/target/*.jar app.jar

# Render injects the PORT environment variable. Spring Boot is already configured to read it.
EXPOSE $PORT

ENTRYPOINT ["java", "-jar", "app.jar"]
