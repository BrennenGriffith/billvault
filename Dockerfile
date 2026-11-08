FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -DskipTests dependency:go-offline
COPY src src
RUN ./mvnw -q -DskipTests package
FROM eclipse-temurin:25-jre
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /app/target/billvault-0.0.1-SNAPSHOT.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=20s --start-period=60s CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
