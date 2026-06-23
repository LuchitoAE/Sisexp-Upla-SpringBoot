FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY sisexp/pom.xml .
COPY sisexp/src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
