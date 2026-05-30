FROM jenkins/jenkins:lts-jdk21
USER root

# 1. Cài đặt Docker CLI, git, curl, và các công cụ cần thiết
RUN apt-get update && apt-get install -y \
    docker.io \
    git \
    curl \
    build-essential \
    && rm -rf /var/lib/apt/lists/* \
    && git config --system --add safe.directory '*'

# 2. Tải trực tiếp Docker Compose V2 chính chủ bỏ vào thư mục Plugin của Docker
RUN mkdir -p /usr/local/lib/docker/cli-plugins && \
    curl -SL https://github.com/docker/compose/releases/download/v2.29.2/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose && \
    chmod +x /usr/local/lib/docker/cli-plugins/docker-compose && \
    docker-compose --version

# 3. Cấu hình Jenkins plugins cơ bản (optional, nhưng giúp workflow tốt hơn)
RUN jenkins-plugin-cli --plugins \
    workflow-aggregator:590.v6a_d052e5a_ea_5 \
    pipeline-model-definition:2.2176.v43ed4a_1829b_8 \
    docker-plugin:1.5.1 \
    git:5.2.2

# Giữ quyền root tối cao để Jenkins điều khiển được máy thật
USER root