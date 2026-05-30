FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN --mount=type=cache,target=/home/gradle/.gradle ./gradlew :order-service:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/order-service/build/libs/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]