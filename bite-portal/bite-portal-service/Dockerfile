FROM flashcode/portal:latest
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 生成链路里的 npm install 走国内镜像，避免 registry.npmjs.org 长时间卡住后 ECONNRESET
RUN echo "registry=https://registry.npmmirror.com" > /root/.npmrc
WORKDIR /workspace
MAINTAINER bitejiuyeke
COPY target/*.jar /workspace/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar