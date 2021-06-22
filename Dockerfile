FROM bellsoft/liberica-openjdk-alpine:16 as base
WORKDIR /usr/app
COPY ./ /usr/app

FROM base as test
RUN ["./gradlew", "clean", "test", "--no-daemon", "--info"]
RUN ["ls", "/usr/app"]

FROM base as builder
RUN ["./gradlew", "build", "-xtest", "--no-daemon", "--info"]
RUN ["ls", "/usr/app"]

FROM base as backend-api
RUN apk add curl
RUN addgroup -S rarible && adduser -S rarible -G rarible
USER rarible:rarible
WORKDIR /usr/app
RUN ["ls", "-alh"]
COPY --from=builder /usr/app/backend-api/build/libs/backend-api.jar application.jar
CMD java $JAVA_OPTIONS -jar application.jar


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