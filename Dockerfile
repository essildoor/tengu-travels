FROM openjdk:8-jdk-alpine
#RUN mkdir -p /tmp/data
ADD build/libs/tengu-travels-0.0.1-SNAPSHOT.jar tengu-travels.jar
#ADD train_data.zip /tmp/data/data.zip
ENV JAVA_OPTS="-Xmx3g -Xms3g -XX:+UseConcMarkSweepGC -XX:+DisableExplicitGC -server"
EXPOSE 80
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /tengu-travels.jar" ]