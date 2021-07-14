FROM openjdk:15-jdk-alpine

RUN addgroup -S spring && adduser -S spring -G spring

USER spring:spring

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} go-session-server.jar

ENTRYPOINT ["java", "-jar", "--enable-preview", "/go-session-server.jar"]
