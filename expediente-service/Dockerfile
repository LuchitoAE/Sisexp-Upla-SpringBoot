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
RUN mvn dependency:go-offline -pl expediente-service -am -q
COPY sisexp-common/src ./sisexp-common/src
COPY expediente-service/src ./expediente-service/src
RUN mvn clean package -DskipTests -pl expediente-service -am -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /build/expediente-service/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
