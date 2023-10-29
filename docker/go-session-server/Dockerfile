FROM openjdk:16-jdk-alpine@sha256:49d822f4fa4deb5f9d0201ffeec9f4d113bcb4e7e49bd6bc063d3ba93aacbcae

RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} go-session-server.jar

ENTRYPOINT ["java", "-jar", "--enable-preview", "/go-session-server.jar"]
