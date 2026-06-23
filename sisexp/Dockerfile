# Stage 1: Build React frontend
FROM node:18-alpine AS frontend
WORKDIR /app/frontend
COPY sisexp/frontend/package.json sisexp/frontend/package-lock.json ./
RUN npm install
COPY sisexp/frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-17-alpine AS backend
WORKDIR /app
COPY sisexp/pom.xml .
RUN mvn dependency:go-offline -q
COPY sisexp/src ./src
COPY --from=frontend /app/frontend/build ./src/main/resources/static
RUN mvn clean package -DskipTests -q

# Stage 3: Run
FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache postgresql-client
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
