# AGENTS.md — SISEXP-UPLA (Spring Boot Microservicios) — vFinal

## PROYECTO ACTUAL: SISEXP-UPLA Microservicios

**Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes**

Arquitectura de microservicios con 12 contenedores Docker Compose. Proyecto final del curso Arquitectura de Software (VIII Ciclo).

| Dato | Valor |
|---|---|
| Dominio | Gestión presupuestal de expedientes (Techo → POI → PAP → Expedientes) |
| Entidades | 11 (Usuario, TechoPresupuestal, ActividadPOI, NecesidadPAP, Expediente, DocumentoAdjunto, SeguimientoLog, NotaModificatoria, Notificacion, Rol) |
| Roles | 6 (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato) |
| Estados expediente | 7 (Borrador, En revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) |
| Frontend | React 19 SPA + NGINX |
| Auth | JWT stateless (jjwt 0.12.6) |
| GitHub | https://github.com/LuchitoAE/sisexp-microservicios |

---

## Arquitectura — 12 Contenedores

| # | Contenedor | Puerto | Tecnologia |
|:--|:-----------|:------:|:-----------|
| 1 | sisexp-nginx | 80 | nginx:alpine (React SPA + proxy /api) |
| 2 | sisexp-api-gateway | 8080 | Spring Cloud Gateway + JWT filter |
| 3 | sisexp-eureka | 8761 | Netflix Eureka (Service Discovery) |
| 4 | sisexp-auth-service | 8081 | Spring Boot + PostgreSQL |
| 5 | sisexp-presupuesto-service | 8082 | Spring Boot + PostgreSQL |
| 6 | sisexp-expediente-service | 8083 | Spring Boot + PostgreSQL + RabbitMQ |
| 7 | sisexp-notificacion-service | 8084 | Spring Boot + PostgreSQL + RabbitMQ |
| 8-11 | 4 PostgreSQL | 5433-5436 | postgres:16-alpine |
| 12 | sisexp-rabbitmq | 5672 | rabbitmq:3-management-alpine |

---

## Bounded Contexts

| Contexto | Servicio | Puerto | Responsabilidad |
|:---------|:---------|:------:|:----------------|
| Autenticacion | auth-service | 8081 | Login JWT, gestion usuarios/roles, validacion tokens |
| Presupuesto | presupuesto-service | 8082 | Techos presupuestales, POI, necesidades PAP, saldos |
| Expedientes | expediente-service | 8083 | CRUD expedientes, documentos, seguimiento estados, publica eventos |
| Notificaciones | notificacion-service | 8084 | Consume eventos RabbitMQ, crea/consulta notificaciones |
| Ruteo | api-gateway | 8080 | Punto unico de entrada, JWT global, ruteo load-balanced |

---

## Acceso al Sistema

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

## Comandos Rapidos

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

## Estructura de Paquetes (por servicio)

```
auth-service/src/main/java/com/upla/sisexp/auth/
├── config/           # SecurityConfig, DataInitializer
├── security/         # JwtTokenProvider, JwtAuthFilter
├── model/            # Usuario (JPA entity)
├── enums/            # RolUsuario
├── repository/       # UsuarioRepository
├── service/          # AuthService
└── controller/       # ApiAuthController, ApiUsuarioController, StatusController

presupuesto-service/src/main/java/com/upla/sisexp/presupuesto/
├── config/
├── model/            # TechoPresupuestal, ActividadPOI, NecesidadPAP
├── repository/
├── service/
└── controller/       # ApiTecho, ApiActividadPOI, ApiNecesidadPAP, ApiSaldoInterno

expediente-service/src/main/java/com/upla/sisexp/expediente/
├── config/           # RabbitMQConfig
├── model/            # Expediente, SeguimientoLog, DocumentoAdjunto
├── repository/
├── service/          # Publica eventos a RabbitMQ
└── controller/       # ApiExpedienteController

notificacion-service/src/main/java/com/upla/sisexp/notificacion/
├── config/           # RabbitMQ consumer config
├── model/            # Notificacion
├── repository/
├── service/          # Consumer de eventos RabbitMQ
└── controller/       # ApiNotificacionController
```

---

## Skills Disponibles

### Backend & Arquitectura
- `arquitectura-microservicios` — Clean Architecture, SOLID, patrones GoF
- `backend-sisexp` — Spring Boot, JPA, servicios, repos
- `deploy-sisexp` — Docker, Railway, ngrok

### Documentacion
- `docs-sisexp` — MD → DOCX profesional con reference.docx + pandoc
- `diagramas-ssd` — System Sequence Diagrams
- `diagramas-ers` — ERS (ISO 29148)
- `diagramas-bce` — Boundary-Control-Entity

### Frontend & UX/UI
- `frontend-accessibility-inclusive-design` — Accesibilidad WCAG, screen readers
- `frontend-design-systems-frontend-architecture` — Design tokens, componentes reusables
- `frontend-forms-inputs-checkout` — Formularios, validacion, errores
- `frontend-information-architecture-navigation` — Navegacion, breadcrumbs
- `frontend-interaction-patterns-components` — Patrones de interaccion, modales
- `frontend-ui-visual-composition` — Jerarquia visual, tipografia, color
- `frontend-ux-usability-foundations` — Affordances, feedback, prevencion errores
- `frontend-ux-writing-content-design` — Microcopy, CTAs, empty states
- `ux-ui-design` — Thymeleaf + Bootstrap (legacy del monolito)

---

## Endpoints API

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
| GET | /api/expedientes/rastreo/{codigo} | expediente-service | No |

---

## Documentos

| Archivo | Descripcion |
|:--------|:------------|
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Documentacion completa de arquitectura |
| `docs/INFORME_MICROSERVICIOS_SISEXP.docx` | Version Word (reference.docx template) |
| `docs/reference.docx` | Plantilla de estilos para pandoc → DOCX |
| `docs/diagramas/microservicios-arquitectura.html` | Diagrama interactivo offline |
