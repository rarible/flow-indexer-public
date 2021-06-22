FROM bellsoft/liberica-openjdk-alpine:16 as base


FROM base as test
WORKDIR /usr/app
COPY ./ /usr/app
CMD ["./gradlew", "clean", "test", "--no-daemon", "--info"]
CMD ["ls", "/usr/app"]



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