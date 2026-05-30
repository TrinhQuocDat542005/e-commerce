FROM jenkins/jenkins:lts-jdk21
USER root

# 1. Cài đặt Docker CLI cơ bản, curl và git
RUN apt-get update && apt-get install -y docker.io curl git && \
    git config --system --add safe.directory '*'

# 2. Tải trực tiếp Docker Compose V2 bỏ vào thư mục Plugin chính quy
RUN mkdir -p /usr/local/lib/docker/cli-plugins && \
    curl -SL https://github.com/docker/compose/releases/download/v2.29.2/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# 3. [Chiêu vá chốt hạ] Tạo lối tắt để gõ hệ có dấu gạch hay không dấu gạch đều nhận diện được
RUN ln -f -s /usr/local/lib/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose

# 4. Chạy lệnh nghiệm thu nội bộ xem hệ thống đã nhận súng đạn chưa
RUN docker compose version
RUN docker-compose --version

USER root