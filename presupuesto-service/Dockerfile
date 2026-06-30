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
RUN mvn dependency:go-offline -pl presupuesto-service -am -q
COPY sisexp-common/src ./sisexp-common/src
COPY presupuesto-service/src ./presupuesto-service/src
RUN mvn clean package -DskipTests -pl presupuesto-service -am -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /build/presupuesto-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
