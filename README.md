# SISEXP-UPLA — Microservicios

Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes

Arquitectura de microservicios con 12 contenedores Docker Compose. Proyecto final del curso Arquitectura de Software (VIII Ciclo).

---

## Stack

| Capa | Tecnologia |
|:-----|:-----------|
| Backend | Spring Boot 3.4.1, Java 17, Spring Cloud, JWT |
| Frontend | React 19 SPA + NGINX |
| BD | PostgreSQL 16-alpine (4 instancias: 5433-5436) |
| Mensajeria | RabbitMQ 3-management-alpine |
| Service Discovery | Netflix Eureka |
| API Gateway | Spring Cloud Gateway |
| Contenedores | Docker + Docker Compose (12) |

---

## Arquitectura

```
nginx (:80) → api-gateway (:8080) → eureka (:8761)
                                   ├── auth-service        (:8081) + PostgreSQL (:5433)
                                   ├── presupuesto-service (:8082) + PostgreSQL (:5434)
                                   ├── expediente-service  (:8083) + PostgreSQL (:5435) + RabbitMQ
                                   └── notificacion-service(:8084) + PostgreSQL (:5436) + RabbitMQ
```

---

## Inicio rapido

```bash
# Construir y levantar
docker compose build
docker compose up -d

# Ver estado
docker compose ps

# Logs de un servicio
docker compose logs -f auth-service

# Detener
docker compose down

# Reconstruir frontend
cd frontend && pnpm install && pnpm run build && cd ..
docker compose build nginx && docker compose up -d nginx
```

---

## Acceso

| URL | Que es |
|:----|:-------|
| `http://localhost` | SISEXP-UPLA React SPA (login, dashboard, CRUD) |
| `http://localhost/monitor` | Dashboard de monitoreo (12 nodos en tiempo real) |
| `http://localhost/api/status` | API: estado de los 4 servicios |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:15672` | RabbitMQ Management (sisexp/sisexp) |

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

## Estructura del proyecto

```
├── microservicios/
│   ├── api-gateway/           # Spring Cloud Gateway + JWT filter
│   ├── auth-service/          # Login JWT, usuarios, roles
│   ├── eureka-server/         # Netflix Eureka
│   ├── expediente-service/    # CRUD expedientes + RabbitMQ publisher
│   ├── notificacion-service/  # Notificaciones + RabbitMQ consumer
│   ├── presupuesto-service/   # Techos, POI, PAP, saldos
│   └── sisexp-common/         # DTOs, enums, excepciones compartidas
├── frontend/                  # React 19 SPA + NGINX + dashboard monitor
├── docs/                      # Documentacion y diagramas
├── .opencode/skills/          # 18 skills para AI agents
├── docker-compose.yml         # 12 contenedores
└── pom.xml                    # Parent POM multi-modulo
```

---

## API Endpoints

| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | Admin |
| GET | /api/techos-presupuestales | presupuesto-service | JWT |
| GET | /api/actividades-poi | presupuesto-service | JWT |
| GET | /api/necesidades-pap | presupuesto-service | JWT |
| GET | /api/expedientes | expediente-service | JWT |
| POST | /api/expedientes | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/notificaciones | notificacion-service | JWT |
| GET | /api/status | auth-service | No |

---

## Documentacion

| Archivo | Descripcion |
|:--------|:------------|
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Documentacion completa de arquitectura |
| `docs/INFORME_MICROSERVICIOS_SISEXP.docx` | Version Word |
| `docs/diagramas/microservicios-arquitectura.html` | Diagrama interactivo offline |
| `AGENTS.md` | Guia para AI agents + skills disponibles |
