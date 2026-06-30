FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY sisexp-common/pom.xml ./sisexp-common/
COPY eureka-server/pom.xml ./eureka-server/
COPY auth-service/pom.xml ./auth-service/
COPY presupuesto-service/pom.xml ./presupuesto-service/
COPY expediente-service/pom.xml ./expediente-service/
COPY notificacion-service/pom.xml ./notificacion-service/
COPY api-gateway/pom.xml ./api-gateway/
RUN mvn dependency:go-offline -pl eureka-server -q
COPY eureka-server/src ./eureka-server/src
RUN mvn clean package -DskipTests -pl eureka-server -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget curl
COPY --from=builder /build/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
