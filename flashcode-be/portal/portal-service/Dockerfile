FROM flashcode/portal:latest
WORKDIR /workspace
MAINTAINER bitejiuyeke
COPY target/*.jar /workspace/app.jar
# chromedriver 不在构建时安装，运行时由 Selenium Manager 自动下载匹配版本（见 SeleniumUtil#openChrome 兜底）
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar