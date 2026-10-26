# Build directly from source: Render does not have the local Maven target directories.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY microservices/ microservices/
RUN --mount=type=cache,target=/root/.m2 \
    mvn -f microservices/pom.xml --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache su-exec && addgroup -g 10001 app && adduser -D -u 10001 -G app app
WORKDIR /app
ARG SERVICE=api-gateway
# Normalize both an omitted argument and an explicitly empty Render variable.
ENV CAMERAHUB_SERVICE=${SERVICE:-api-gateway}
COPY --from=build /build/microservices/${CAMERAHUB_SERVICE}/target/${CAMERAHUB_SERVICE}-0.1.0-SNAPSHOT.jar /app/app.jar
COPY deploy/render/entrypoint.sh /app/entrypoint.sh
RUN chmod 755 /app/entrypoint.sh
ENTRYPOINT ["/app/entrypoint.sh"]
