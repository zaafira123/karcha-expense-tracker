FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY maven-retry.sh /usr/local/bin/maven-retry
RUN sed -i 's/\r$//' /usr/local/bin/maven-retry && sh /usr/local/bin/maven-retry mvn -B -ntp dependency:go-offline
COPY src ./src
RUN sh /usr/local/bin/maven-retry mvn -B -ntp -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app
COPY --from=build --chown=app:app /workspace/target/*.jar /app/app.jar
USER app
EXPOSE 8080
ENV PORT=8080
ENTRYPOINT ["sh", "-c", "exec java -XX:MaxRAMPercentage=75.0 -jar /app/app.jar"]
