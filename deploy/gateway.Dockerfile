# The gateway image also carries the Angular app: the browser talks to one origin (the gateway),
# which serves the SPA, owns the session cookie and routes /api/** to the services.
FROM node:24-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend .
RUN npx ng build --configuration production

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY libs libs
COPY services services
COPY --from=web /web/dist/frontend/browser services/gateway/src/main/resources/static
RUN mvn -B -q -pl services/gateway -am package -DskipTests \
    && cp services/gateway/target/*-SNAPSHOT.jar /app.jar

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S luach && adduser -S luach -G luach
USER luach
COPY --from=build /app.jar /app/app.jar
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
