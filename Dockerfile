# Dockerfile für Spring Boot + Gradle Projekt
FROM gradle:8.5-jdk17 AS builder
COPY . /app
WORKDIR /app
RUN gradle bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
