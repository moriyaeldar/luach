# Builds any backend module of the monorepo: docker build --build-arg MODULE=services/task-service ...
FROM maven:3.9-eclipse-temurin-21 AS build
ARG MODULE
WORKDIR /src
COPY pom.xml .
COPY libs libs
COPY services services
RUN mvn -B -q -pl ${MODULE} -am package -DskipTests \
    && cp ${MODULE}/target/*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S luach && adduser -S luach -G luach
USER luach
COPY --from=build /app.jar /app/app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
