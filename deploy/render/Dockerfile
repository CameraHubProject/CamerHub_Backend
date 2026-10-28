FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache su-exec && addgroup -g 10001 app && adduser -D -u 10001 -G app app
WORKDIR /app
COPY --from=build /build/target/camerahub-backend-0.1.0-SNAPSHOT.jar /app/app.jar
COPY deploy/render/entrypoint.sh /app/entrypoint.sh
RUN chmod 755 /app/entrypoint.sh
EXPOSE 10000
ENTRYPOINT ["/app/entrypoint.sh"]
