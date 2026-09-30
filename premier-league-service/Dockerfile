# Build context is the PremierLeague/ root (so pl-core-data is reachable).
# docker build -f premier-league-service/Dockerfile .

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pl-core-data ./pl-core-data
RUN mvn -f pl-core-data/pom.xml -DskipTests install

COPY premier-league-service ./premier-league-service
RUN mvn -f premier-league-service/pom.xml -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /workspace/premier-league-service/target/*.jar app.jar
EXPOSE 4002
ENTRYPOINT ["java", "-jar", "app.jar"]
