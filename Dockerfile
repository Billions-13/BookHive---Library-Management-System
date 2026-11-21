# BookHive – container image used by Railway (railway.json) or any Docker host.
#   docker build -t bookhive .
#   docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... bookhive

# ---- Build stage: compile and package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Tests run locally / before merging (mvn verify, H2); the image build only packages.
RUN mvn -B -q -DskipTests package

# ---- Run stage: small JRE image, non-root user ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 --no-create-home bookhive
COPY --from=build /app/target/bookhive-0.0.1-SNAPSHOT.jar app.jar
USER bookhive

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Ho_Chi_Minh -Dfile.encoding=UTF-8"
# Railway injects PORT; application.yml reads ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
