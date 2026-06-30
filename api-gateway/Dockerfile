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
RUN mvn dependency:go-offline -pl api-gateway -q
COPY api-gateway/src ./api-gateway/src
RUN mvn clean package -DskipTests -pl api-gateway -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /build/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
