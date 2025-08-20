# Multi-stage build
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copiar arquivos do projeto
COPY pom.xml .
COPY config-service-api ./config-service-api
COPY config-client-starter ./config-client-starter

RUN mvn dependency:go-offline -B

RUN mvn clean package -DskipTests -pl config-service-api

RUN ls -la config-service-api/target/

FROM eclipse-temurin:17-jre

WORKDIR /app


RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring


COPY --from=build /app/config-service-api/target/spring-config-service-api-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080


ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]