FROM flashcode/portal:latest
WORKDIR /workspace
MAINTAINER bitejiuyeke
COPY target/*.jar /workspace/app.jar
# 与镜像内 Chrome 同版本的 ChromeDriver。容器访问不了 googlechromelabs，不能让 Selenium 运行时再下载。
RUN set -eux; \
    CHROME_VERSION=$(google-chrome --version | grep -oE '[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+'); \
    curl -4 -fL --retry 3 --connect-timeout 30 --max-time 180 \
      -o /tmp/chromedriver.zip \
      "https://cdn.npmmirror.com/binaries/chrome-for-testing/${CHROME_VERSION}/linux64/chromedriver-linux64.zip"; \
    /usr/local/openjdk-21/bin/jar xf /tmp/chromedriver.zip; \
    mv chromedriver-linux64/chromedriver /usr/local/bin/chromedriver; \
    chmod +x /usr/local/bin/chromedriver; \
    rm -rf /tmp/chromedriver.zip chromedriver-linux64
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar