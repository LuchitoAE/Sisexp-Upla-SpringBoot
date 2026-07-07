ARG SERVICE

FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY microservicios/sisexp-common/pom.xml ./microservicios/sisexp-common/
COPY microservicios/eureka-server/pom.xml ./microservicios/eureka-server/
COPY microservicios/auth-service/pom.xml ./microservicios/auth-service/
COPY microservicios/presupuesto-service/pom.xml ./microservicios/presupuesto-service/
COPY microservicios/expediente-service/pom.xml ./microservicios/expediente-service/
COPY microservicios/notificacion-service/pom.xml ./microservicios/notificacion-service/
COPY microservicios/api-gateway/pom.xml ./microservicios/api-gateway/
RUN mvn dependency:go-offline -pl microservicios/${SERVICE} -am -q 2>/dev/null || true
COPY microservicios/sisexp-common/src ./microservicios/sisexp-common/src
COPY microservicios/${SERVICE}/src ./microservicios/${SERVICE}/src
RUN mvn clean package -DskipTests -pl microservicios/${SERVICE} -am -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=builder /build/microservicios/${SERVICE}/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
