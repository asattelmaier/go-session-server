FROM openjdk:21-jdk-slim@sha256:7072053847a8a05d7f3a14ebc778a90b38c50ce7e8f199382128a53385160688

RUN addgroup --system spring && \
    adduser --system --ingroup spring spring

USER spring:spring

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} go-session-server.jar

ENTRYPOINT ["java", "-jar", "/go-session-server.jar"]
