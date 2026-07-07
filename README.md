# SISEXP-UPLA — Microservicios

Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes

Arquitectura de microservicios con 12 contenedores Docker Compose. Proyecto final del curso Arquitectura de Software (VIII Ciclo).

---

## Stack

| Capa | Tecnologia |
|:-----|:-----------|
| Backend | Spring Boot 3.4.1, Java 17, Spring Cloud Gateway, JWT (jjwt 0.12.6) |
| Frontend | React 19 SPA + NGINX (identidad institucional UPLA) |
| BD | PostgreSQL 16-alpine (4 instancias: auth, presupuesto, expediente, notificacion) |
| Mensajeria | RabbitMQ 3-management-alpine |
| Service Discovery | Netflix Eureka |
| Contenedores | Docker + Docker Compose (12) |
| Deploy Frontend | Vercel (gratis) |
| Deploy Backend | Local Docker Compose / Railway (WIP) |

---

## Inicio rapido

```bash
# Construir y levantar (primera vez)
docker compose build
docker compose up -d

# Datos frescos (reinicia DBs)
docker compose down -v presupuesto-db expediente-db auth-db
docker compose up -d presupuesto-db expediente-db auth-db
docker compose up -d auth-service presupuesto-service expediente-service api-gateway

# Reconstruir solo frontend
cd frontend && pnpm install && pnpm run build && cd ..
docker compose build nginx && docker compose up -d --force-recreate nginx

# Ver estado
docker compose ps

# Logs de un servicio
docker compose logs -f presupuesto-service

# Smoke test automatizado
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from tester
```

---

## Acceso

| URL | Que es |
|:----|:-------|
| `http://localhost` | SISEXP-UPLA React SPA (login, dashboard, CRUD) |
| `http://localhost/monitor` | Dashboard de monitoreo (12 nodos en tiempo real) |
| `http://localhost/api/status` | API: estado de los 7 nodos |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:15672` | RabbitMQ Management (sisexp/sisexp) |
| `https://frontend-ivory-nine-43.vercel.app` | Frontend en Vercel (produccion) |

### Credenciales

| Rol | Email | Password |
|:----|:------|:---------|
| Admin | jefe@upla.edu.pe | jefe123 |
| Coord | coord@upla.edu.pe | coord123 |
| Secretaria | secretaria@upla.edu.pe | secretaria123 |
| Director | director@upla.edu.pe | director123 |
| Lab | lab@upla.edu.pe | lab123 |
| Decanato | decanato@upla.edu.pe | decanato123 |

---

## Arquitectura — 12 Contenedores

```
nginx (:80) → api-gateway (:8080) → eureka (:8761)
                                   ├── auth-service        (:8081) + PostgreSQL (:5433)
                                   ├── presupuesto-service (:8082) + PostgreSQL (:5434)
                                   ├── expediente-service  (:8083) + PostgreSQL (:5435) + RabbitMQ
                                   └── notificacion-service(:8084) + PostgreSQL (:5436) + RabbitMQ
```

| Servicio | Puerto | DB Puerto | Responsabilidad |
|:---------|:------:|:---------:|:----------------|
| nginx | 80 | — | React SPA + proxy /api → gateway |
| api-gateway | 8080 | — | JWT global, CORS, ruteo load-balanced |
| eureka-server | 8761 | — | Service Discovery |
| auth-service | 8081 | 5433 | Login JWT, usuarios, roles, dashboard, status |
| presupuesto-service | 8082 | 5434 | Techos, POI, PAP, notas modif., reportes, dashboard |
| expediente-service | 8083 | 5435 | Expedientes, documentos, seguimiento, publica eventos RabbitMQ |
| notificacion-service | 8084 | 5436 | Consume eventos RabbitMQ, notificaciones |
| rabbitmq | 5672/15672 | — | Mensajeria async |

---

## Estructura del proyecto

```
├── microservicios/
│   ├── api-gateway/           # Spring Cloud Gateway + JWT filter + CORS
│   ├── auth-service/          # Login JWT, usuarios, roles, DataInitializer
│   ├── eureka-server/         # Netflix Eureka
│   ├── expediente-service/    # Expedientes, documentos, seguimiento, RabbitMQ publisher
│   ├── notificacion-service/  # Notificaciones, RabbitMQ consumer
│   ├── presupuesto-service/   # Techos, POI, PAP, NotasModif., Dashboard, Reportes, RestTemplate
│   └── sisexp-common/         # Enums compartidos (9), excepciones
├── frontend/                  # React 19 SPA + NGINX + dashboard monitor
├── docs/                      # Documentacion y diagramas
├── .opencode/skills/          # 18 skills para AI agents
├── docker-compose.yml         # 12 contenedores (desarrollo local)
├── docker-compose.test.yml    # Smoke test automatizado (21 endpoints)
└── pom.xml                    # Parent POM multi-modulo
```

---

## API Endpoints (41 total)

| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | Admin |
| GET | /api/techos-presupuestales | presupuesto-service | JWT |
| GET | /api/actividades-poi/techo/{id} | presupuesto-service | JWT |
| GET | /api/necesidades-pap/actividad/{id} | presupuesto-service | JWT |
| GET/POST | /api/notas-modificatorias | presupuesto-service | JWT |
| PUT | /api/notas-modificatorias/{id}/configurar | presupuesto-service | JWT |
| PUT | /api/notas-modificatorias/{id}/rechazar | presupuesto-service | JWT |
| GET | /api/dashboard/alertas | presupuesto-service | JWT |
| GET | /api/dashboard/saldos | presupuesto-service | JWT |
| GET | /api/reportes/anual/{anio} | presupuesto-service | JWT |
| GET | /api/reportes/expedientes | presupuesto-service | JWT |
| GET | /api/reportes/poi | presupuesto-service | JWT |
| GET | /api/reportes/pap | presupuesto-service | JWT |
| GET | /api/reportes/poi/{id} | presupuesto-service | JWT |
| GET | /api/reportes/pap/{id} | presupuesto-service | JWT |
| GET/POST | /api/expedientes | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/expedientes/rastreo/{codigo} | expediente-service | No |
| GET | /api/notificaciones | notificacion-service | JWT |
| GET | /api/status | auth-service | No |

---

## Seed Data

| Servicio | Datos |
|:---------|:------|
| auth | 6 usuarios (1 por rol), passwords BCrypt |
| presupuesto | 5 techos (2022-2026, S/ 1.5M en 2026), 20 actividades POI, 80 necesidades PAP, 4 notas modificatorias |
| expediente | 8 expedientes en 7 estados distintos, 6 seguimiento logs |

---

## Documentacion

| Archivo | Descripcion |
|:--------|:------------|
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Documentacion completa de arquitectura |
| `docs/INFORME_MICROSERVICIOS_SISEXP.docx` | Version Word |
| `AGENTS.md` | Guia para AI agents + skills disponibles |

---

## Deploy (WIP)

| Componente | Plataforma | Estado |
|:-----------|:----------|:------|
| Frontend | Vercel | OK |
| Backend (local) | Docker Compose | OK |
| Backend (cloud) | Railway | Configurado parcialmente (8/12 OK) |

**Nota Railway**: La CLI de Railway v4.30.5 es interactiva y no soporta flags en modo no-TTY. Usar el Dashboard para crear servicios. Evitar hacer muchas llamadas API seguidas (rate limit Cloudflare Error 1015). Espaciar consultas con al menos 2 segundos entre cada una.
