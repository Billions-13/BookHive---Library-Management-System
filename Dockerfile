# BookHive – container image for Railway (see railway.json) or any Docker host.
#   docker build -t bookhive .
#   docker run -p 8080:8080 -e DB_URL=... -e DB_USERNAME=... -e DB_PASSWORD=... bookhive

# ---- Build stage: package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Run stage: JRE only, non-root user ----
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 1001 --no-create-home bookhive
COPY --from=build /app/target/bookhive-0.0.1-SNAPSHOT.jar app.jar
USER bookhive

# Production settings passed as environment variables, so no source file has to change:
# - prod profile (application-prod.yml)
# - session cookie only sent over HTTPS (Railway serves the site over HTTPS)
# - CSV upload up to 2 MB, the limit CatalogCsvService checks (Spring's default is 1 MB)
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_SERVLET_SESSION_COOKIE_SECURE=true \
    SPRING_SERVLET_MULTIPART_MAX_FILE_SIZE=2MB \
    SPRING_SERVLET_MULTIPART_MAX_REQUEST_SIZE=3MB \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Duser.timezone=Asia/Ho_Chi_Minh -Dfile.encoding=UTF-8"

# Railway sets PORT; application.yml reads ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
