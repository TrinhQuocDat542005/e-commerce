FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN --mount=type=cache,target=/home/gradle/.gradle ./gradlew :api-gateway:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/api-gateway/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]