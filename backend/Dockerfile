# syntax=docker/dockerfile:1.7
ARG VCS_REF=unknown

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN --mount=type=cache,id=picture-management-maven-v1,target=/root/.m2 \
    set -eu; \
    for attempt in 1 2 3; do \
      mvn -B -ntp -DskipTests -Dmaven.wagon.http.retryHandler.count=5 package && exit 0; \
      echo "Maven build attempt ${attempt} failed" >&2; \
    done; \
    exit 1

FROM eclipse-temurin:21-jre-alpine
ARG VCS_REF
LABEL org.opencontainers.image.revision=$VCS_REF
WORKDIR /app
COPY --from=build /workspace/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
