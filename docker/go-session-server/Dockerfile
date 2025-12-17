FROM eclipse-temurin:21-jre-noble

RUN addgroup --system spring && \
    adduser --system --ingroup spring spring

USER spring:spring

ARG JAR_FILE=target/*.jar

COPY ${JAR_FILE} go-session-server.jar

ENTRYPOINT ["java", "-jar", "/go-session-server.jar"]
