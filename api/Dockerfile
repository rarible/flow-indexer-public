FROM bellsoft/liberica-openjdk-alpine:11

RUN apk add curl

RUN addgroup -S rarible && adduser -S rarible -G rarible
USER rarible:rarible

WORKDIR /home/rarible
COPY ./target/boot/backend-api.jar application.jar

CMD java $JAVA_OPTIONS $JAVA_MEMORY_OPTIONS -jar application.jar
