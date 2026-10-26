FROM eclipse-temurin:21-jre
WORKDIR /app
ARG SERVICE
COPY ${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar app.jar
USER 10001:10001
ENTRYPOINT ["java","-XX:MaxRAMPercentage=65","-jar","/app/app.jar"]
