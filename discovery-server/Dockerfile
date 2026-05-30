# ==========================================
# STAGE 1: Môi trường build code sạch ngầm
# ==========================================
FROM gradle:8.10-jdk21 AS builder
WORKDIR /app

# Copy toàn bộ sườn dự án Multi-module vào để Gradle nhận diện cấu hình chung
COPY --chown=gradle:gradle . .

# Chỉ ra lệnh compile đúng module discovery-server, bỏ qua chạy test để tiết kiệm thời gian
RUN --mount=type=cache,target=/home/gradle/.gradle ./gradlew :discovery-server:bootJar -x test
# ==========================================
# STAGE 2: Môi trường chạy thực tế tối giản
# ==========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Bốc duy nhất file .jar thành phẩm từ Stage 1 mang sang Stage 2
COPY --from=builder /app/discovery-server/build/libs/*.jar app.jar

# Mở cổng 8761 để các service khác có thể gọi vào đăng ký danh bạ
EXPOSE 8761

# Lệnh nổ máy khởi chạy dịch vụ khi container khởi động
ENTRYPOINT ["java", "-jar", "app.jar"]