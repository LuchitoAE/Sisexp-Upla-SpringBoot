# Stage 1: Build React frontend
FROM node:18-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm install
COPY frontend/ .
RUN npm run build

# Stage 2: Build Spring Boot backend
FROM maven:3.9-eclipse-temurin-17-alpine AS backend
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
COPY --from=frontend /app/frontend/build ./src/main/resources/static
RUN mvn clean package -DskipTests -q

# Stage 3: Run
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
