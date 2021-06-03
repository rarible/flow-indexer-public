FROM bellsoft/liberica-openjdk-alpine:11
ARG JAR_FILE
ARG APP_PROPS
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
