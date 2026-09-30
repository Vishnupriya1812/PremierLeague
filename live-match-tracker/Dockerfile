# Build context is the PremierLeague/ root (so pl-core-data is reachable).
# docker build -f live-match-tracker/Dockerfile .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pl-core-data ./pl-core-data
RUN mvn -f pl-core-data/pom.xml -DskipTests install

COPY live-match-tracker ./live-match-tracker
RUN mvn -f live-match-tracker/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/live-match-tracker/target/*.jar app.jar
EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]
