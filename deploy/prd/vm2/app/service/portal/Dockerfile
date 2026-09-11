FROM openjdk:21
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /workspace
MAINTAINER bitejiuyeke
COPY ./*.jar /workspace/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar