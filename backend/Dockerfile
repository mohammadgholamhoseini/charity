# Build the application with Maven and Java 21.
FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# Run the packaged application on a smaller Java image.
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring \
    && mkdir -p /app/uploads \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /app/target/*.jar app.jar

USER spring:spring

EXPOSE 8082

ENV FILE_UPLOAD_DIR=/app/uploads

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
