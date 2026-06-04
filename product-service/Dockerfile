FROM gradle:8.10-jdk21 AS builder
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN ./gradlew :product-service:bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/product-service/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]