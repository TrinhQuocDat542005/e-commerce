FROM jenkins/jenkins:lts-jdk21
USER root

# Cài đặt Docker công cụ dòng lệnh (CLI) vào trong ruột Jenkins
RUN apt-get update && apt-get install -y docker.io

# Giữ quyền root để Jenkins có thể thọc tay điều khiển file socket của máy thật
USER root