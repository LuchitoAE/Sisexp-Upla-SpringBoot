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
RUN mvn dependency:go-offline -pl microservicios/notificacion-service -am -q
COPY microservicios/sisexp-common/src ./sisexp-common/src
COPY microservicios/notificacion-service/src ./notificacion-service/src
RUN mvn clean package -DskipTests -pl microservicios/notificacion-service -am -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /build/notificacion-service/target/*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
