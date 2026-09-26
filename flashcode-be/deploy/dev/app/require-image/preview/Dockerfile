# 基础镜像：Debian Slim + JDK 21（与生成应用的运行环境一致）
FROM openjdk:21-jdk-slim

# 非交互模式，避免 tzdata 等包在构建时阻塞
ENV DEBIAN_FRONTEND=noninteractive

# 统一工作目录，前端构建产物将放置于 /workspace
WORKDIR /workspace

# nginx 提供 80 预览；bash 供 update_nginx_location.sh 与 runJar 使用
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        nginx \
        bash && \
    rm -rf /var/lib/apt/lists/*

# 暴露 Web 端口
EXPOSE 80

# nginx.conf 由 compose 挂载到 /etc/nginx/nginx.conf；脚本目录挂载到 /workspace/scripts
CMD ["nginx", "-g", "daemon off;"]
