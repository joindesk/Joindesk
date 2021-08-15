FROM openjdk:11-jre-slim-buster
EXPOSE 9000
ARG JAR_FILE=target/joindesk-*.jar
ADD ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]