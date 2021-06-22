FROM bellsoft/liberica-openjdk-alpine:16 as base

WORKDIR /usr/app

COPY ./ /usr/app

CMD ["./gradlew", "test", "--no-daemon", "--info"]




#FROM bellsoft/liberica-openjdk-alpine:11
#
#RUN apk add curl
#
#RUN addgroup -S rarible && adduser -S rarible -G rarible
#USER rarible:rarible
#
#WORKDIR /usr/app
#COPY ./build/libs/scanner.jar application.jar
#
#CMD java $JAVA_OPTIONS -jar application.jar