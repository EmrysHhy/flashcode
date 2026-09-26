FROM flashcode/portal:latest
WORKDIR /workspace
MAINTAINER bitejiuyeke
COPY target/*.jar /workspace/app.jar
ENTRYPOINT exec java $JAVA_OPTS -jar app.jar