FROM openjdk:21-jdk-slim

LABEL maintainer="bitejiuyeke"

ENV TZ=Asia/Shanghai
ENV MAVEN_VERSION=3.9.16
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ---------- 1. 一次性安装所有系统依赖 ----------
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        wget && \
    rm -rf /var/lib/apt/lists/*

# ---------- 2. 安装 Maven (稳定缓存层) ----------
# 清华源只保留当前发行版；旧版本会 404（wget exit 8）。按镜像依次回退。
RUN MAVEN_TGZ=apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
    (wget -q "https://mirrors.tuna.tsinghua.edu.cn/apache/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_TGZ}" -O /tmp/maven.tar.gz \
     || wget -q "https://repo.huaweicloud.com/apache/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_TGZ}" -O /tmp/maven.tar.gz \
     || wget -q "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/${MAVEN_TGZ}" -O /tmp/maven.tar.gz) && \
    tar -xzf /tmp/maven.tar.gz -C /opt && \
    ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven && \
    ln -s /opt/maven/bin/mvn /usr/local/bin/mvn && \
    rm -f /tmp/maven.tar.gz

# ---------- 3. 安装 Node.js (稳定缓存层) ----------
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - && \
    apt-get update && \
    apt-get install -y --no-install-recommends nodejs && \
    rm -rf /var/lib/apt/lists/*

CMD ["bash"]