FROM maven:3.9-eclipse-temurin-21 AS build
ARG MAVEN_OPTS=""
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -DskipTests clean package -B -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java", "-jar", "app.jar"]
