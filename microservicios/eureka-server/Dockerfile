FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY microservicios/sisexp-common/pom.xml ./sisexp-common/
COPY microservicios/eureka-server/pom.xml ./eureka-server/
COPY microservicios/auth-service/pom.xml ./auth-service/
COPY microservicios/presupuesto-service/pom.xml ./presupuesto-service/
COPY microservicios/expediente-service/pom.xml ./expediente-service/
COPY microservicios/notificacion-service/pom.xml ./notificacion-service/
COPY microservicios/api-gateway/pom.xml ./api-gateway/
RUN mvn dependency:go-offline -pl microservicios/eureka-server -q
COPY microservicios/eureka-server/src ./eureka-server/src
RUN mvn clean package -DskipTests -pl microservicios/eureka-server -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache wget curl
COPY --from=builder /build/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
