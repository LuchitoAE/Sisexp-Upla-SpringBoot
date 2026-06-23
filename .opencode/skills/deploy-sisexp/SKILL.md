---
name: deploy-sisexp
description: Use when deploying SISEXP-UPLA to Railway, building Docker images, configuring environment variables, troubleshooting build failures, setting up health checks, or managing production infrastructure. Covers Dockerfile multi-stage builds, railway.toml, Railway CLI, and common deployment issues.
---

# Skill: Deploy SISEXP-UPLA — Railway + Docker + Environment

---

## 1. DEPLOYMENT ARCHITECTURE

```
GitHub (push to master)
    ↓ (auto-deploy trigger)
Railway Build
    ↓ (railway.toml → sisexp/Dockerfile)
Docker Multi-Stage Build
    ├── Stage 1: node:18-alpine → npm install → npm run build (React)
    ├── Stage 2: maven:3.9-eclipse-temurin-17-alpine → mvn package (Spring Boot)
    └── Stage 3: eclipse-temurin:17-jre-alpine → java -jar app.jar
        ↓
Production: https://sisexp-web-production.up.railway.app
```

---

## 2. DOCKERFILE (sisexp/Dockerfile)

3-stage multi-platform build:

```dockerfile
# Stage 1: React frontend
FROM node:18-alpine AS frontend
WORKDIR /app/frontend
COPY sisexp/frontend/package.json sisexp/frontend/package-lock.json ./
RUN npm install
COPY sisexp/frontend/ .
RUN npm run build

# Stage 2: Spring Boot
FROM maven:3.9-eclipse-temurin-17-alpine AS backend
WORKDIR /app
COPY sisexp/pom.xml .
RUN mvn dependency:go-offline -q
COPY sisexp/src ./src
COPY --from=frontend /app/frontend/build ./src/main/resources/static
RUN mvn clean package -DskipTests -q

# Stage 3: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=backend /app/target/*.jar app.jar
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**Key**: paths use `sisexp/` prefix because build context is repo root.

---

## 3. RAILWAY.TOML

```toml
[build]
  builder = "DOCKERFILE"
  dockerfilePath = "sisexp/Dockerfile"

[deploy]
  numReplicas = 1
```

**Build context**: always the repo root (where `railway.toml` lives). Dockerfile paths must be relative to root.

---

## 4. ENVIRONMENT VARIABLES

Railway auto-sets these when PostgreSQL is linked:

| Variable | Value | Set by |
|----------|-------|--------|
| `PORT` | `8080` | Railway |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://...` | Railway (PostgreSQL link) |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Railway (PostgreSQL link) |
| `SPRING_DATASOURCE_PASSWORD` | (auto) | Railway (PostgreSQL link) |
| `SPRING_DATASOURCE_DRIVER` | `org.postgresql.Driver` | application-prod.properties |

**Manually set in Railway dashboard:**
| Variable | Value |
|----------|-------|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `JWT_SECRET` | (strong random string, min 32 chars) |

---

## 5. DEPLOY COMMANDS

```bash
# Deploy from project root
railway up -e production -d

# View logs
railway logs -n 100

# Check status
railway status

# Open in browser
railway open
```

**Always run `railway up` from the repo root** (where `railway.toml` is).

---

## 6. .gitignore AND BUILD CONTEXT

Railway uses `.gitignore` as an implicit `.dockerignore`! Files matched by `.gitignore` are EXCLUDED from the build context.

### Critical: package-lock.json

**DO NOT** put `package-lock.json` in root `.gitignore`. The Docker build needs `sisexp/frontend/package-lock.json`. If `.gitignore` has `package-lock.json`, Railway excludes it from the Docker context, and the build fails with:

```
failed to compute cache key: "/sisexp/frontend/package-lock.json": not found
```

### Safe .gitignore pattern for root:
```gitignore
# Build outputs (safe to ignore)
sisexp/target/
sisexp/frontend/node_modules/
sisexp/frontend/build/

# Node.js root only (not sisexp/)
/node_modules/

# IDE, OS, temp files
.idea/
*.iml
*.log
```

---

## 7. HEALTH CHECKS

Railway hits `GET /health` on port 8080.

### Endpoints that MUST be accessible:
- `GET /health` → 200 JSON `{"status":"UP","timestamp":...}`
- `GET /api/health` → 200 JSON same

### Requirements:
- `/health` in SecurityConfig `permitAll()`
- `/health` in HorarioLaboralFilter `RUTAS_EXENTAS`
- WebConfig SPA fallback must NOT intercept `/health` (already handled: `startsWith("api/")` check)

---

## 8. TROUBLESHOOTING BUILD FAILURES

| Symptom | Cause | Fix |
|---------|-------|-----|
| `package-lock.json: not found` | Root `.gitignore` excludes it | Remove `package-lock.json` from `.gitignore` |
| `COPY failed: file not found` | Wrong path relative to build context | Use `sisexp/` prefix in Dockerfile paths |
| Maven build hangs on dependencies | Network issue in Railway build | Retry; Maven downloads from Central |
| `npm install` fails | package-lock.json outdated vs package.json | Delete `node_modules/` locally, run `npm install`, commit fresh lock |
| `mvn clean package` compilation error | New Java code has syntax errors | Check locally with `mvn compile` before pushing |
| Health check fails after deploy | `/health` blocked by filter or security | Verify in `permitAll()` AND `RUTAS_EXENTAS` |
| App starts but returns HTML errors | `SPRING_PROFILES_ACTIVE` not set | Set env var in Railway dashboard or Dockerfile `ENV` |

---

## 9. POST-DEPLOY VERIFICATION

```powershell
# Health check
Invoke-WebRequest -Uri "https://sisexp-web-production.up.railway.app/api/health"

# Login test
$body = @{email="jefe@upla.edu.pe";password="jefe123"} | ConvertTo-Json
Invoke-WebRequest -Uri "https://sisexp-web-production.up.railway.app/api/auth/login" `
  -Method POST -Body $body -ContentType "application/json"

# Frontend loads
Start-Process "https://sisexp-web-production.up.railway.app/login"
```

Expected login response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "usuario": {
    "id": 1,
    "nombre": "Jefe Administrador",
    "email": "jefe@upla.edu.pe",
    "rol": "Administrador",
    "horarioRestringido": false
  }
}
```

---

## 10. PRODUCTION URLS

| Resource | URL |
|----------|-----|
| App | https://sisexp-web-production.up.railway.app |
| Login | https://sisexp-web-production.up.railway.app/login |
| Dashboard | https://sisexp-web-production.up.railway.app/dashboard |
| API Health | https://sisexp-web-production.up.railway.app/api/health |
| Rastreo | https://sisexp-web-production.up.railway.app/rastreo |
| Railway Project | https://railway.com/project/8f718509-dcb0-41e2-8577-05a789002592 |
