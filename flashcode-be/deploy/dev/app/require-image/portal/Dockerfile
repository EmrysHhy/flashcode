FROM openjdk:21-jdk-slim

LABEL maintainer="bitejiuyeke"

ENV TZ=Asia/Shanghai
ENV MAVEN_VERSION=3.9.16
ENV MAVEN_HOME=/opt/maven
ENV PATH=$MAVEN_HOME/bin:$PATH
ENV DEBIAN_FRONTEND=noninteractive
ENV CHROME_BIN=/usr/bin/google-chrome

RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# ---------- 1. 系统工具 + 无头 Chrome（给 Selenium 截图） ----------
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        curl \
        wget \
        gnupg \
        ca-certificates \
        fonts-liberation \
        fonts-noto-cjk \
        libasound2 \
        libatk-bridge2.0-0 \
        libatk1.0-0 \
        libcups2 \
        libdrm2 \
        libgbm1 \
        libgtk-3-0 \
        libnspr4 \
        libnss3 \
        libxcomposite1 \
        libxdamage1 \
        libxfixes3 \
        libxkbcommon0 \
        libxrandr2 \
        xdg-utils && \
    wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb -O /tmp/chrome.deb && \
    apt-get install -y --no-install-recommends /tmp/chrome.deb && \
    rm -f /tmp/chrome.deb && \
    ln -sf /usr/bin/google-chrome-stable /usr/bin/google-chrome && \
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
    rm -rf /var/lib/apt/lists/* && \
    echo "registry=https://registry.npmmirror.com" > /root/.npmrc

CMD ["bash"]