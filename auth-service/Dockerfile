FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY auth-service/build/libs/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
