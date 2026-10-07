FROM flashcode/portal:latest
WORKDIR /workspace
MAINTAINER bitejiuyeke
COPY target/*.jar /workspace/app.jar
# Chrome 稳定版号和 Chrome for Testing 构建号不是同一个补丁，按完整版本去下会 404（curl 22）。
# 只对齐主版本，从 npmmirror 上该主版本最新的一份驱动下载。
RUN set -eux; \
    CHROME_VERSION=$(google-chrome --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+'); \
    MAJOR=$(echo "$CHROME_VERSION" | cut -d. -f1); \
    echo "chrome=$CHROME_VERSION major=$MAJOR"; \
    curl -4 -fL --retry 3 --connect-timeout 30 --max-time 60 \
      -o /tmp/cft-index.json \
      "https://registry.npmmirror.com/-/binary/chrome-for-testing/"; \
    DRIVER_VERSION=$(grep -oE "${MAJOR}\.[0-9]+\.[0-9]+\.[0-9]+" /tmp/cft-index.json | sort -uV | tail -n 1); \
    test -n "$DRIVER_VERSION"; \
    echo "chromedriver=$DRIVER_VERSION"; \
    curl -4 -fL --retry 3 --connect-timeout 30 --max-time 180 \
      -o /tmp/chromedriver.zip \
      "https://cdn.npmmirror.com/binaries/chrome-for-testing/${DRIVER_VERSION}/linux64/chromedriver-linux64.zip"; \
    /usr/local/openjdk-21/bin/jar xf /tmp/chromedriver.zip; \
    mv chromedriver-linux64/chromedriver /usr/local/bin/chromedriver; \
    chmod +x /usr/local/bin/chromedriver; \
    rm -rf /tmp/chromedriver.zip chromedriver-linux64 /tmp/cft-index.json
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar