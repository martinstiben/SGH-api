FROM eclipse-temurin:17-jdk-alpine

# Instalar curl para health checks
RUN apk add --no-cache curl

VOLUME /tmp
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw clean package -DskipTests
RUN ls -la target/
RUN cp target/*.jar app.jar
RUN ls -la app.jar
ENTRYPOINT ["java","-jar","app.jar"]
