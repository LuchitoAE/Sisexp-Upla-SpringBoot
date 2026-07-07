# AGENTS.md — SISEXP-UPLA (Spring Boot Microservicios)

## PROYECTO ACTUAL: SISEXP-UPLA Microservicios

**Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes**

Arquitectura de microservicios con 12 contenedores Docker Compose. Proyecto final del curso Arquitectura de Software (VIII Ciclo).

| Dato | Valor |
|---|---|
| Dominio | Gestion presupuestal de expedientes (Techo -> POI -> PAP -> Expedientes) |
| Entidades | 9 (Usuario, TechoPresupuestal, ActividadPOI, NecesidadPAP, Expediente, DocumentoAdjunto, SeguimientoLog, NotaModificatoria, Notificacion) |
| Enums | 9 (RolUsuario, EstadoExpediente, Urgencia, Naturaleza, EstadoActividad, TipoDocumento, TipoNotificacion, TipoNota, EstadoNota) |
| Roles | 6 (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato) |
| Estados expediente | 7 (Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) |
| Frontend | React 19 SPA CRA + NGINX + react-icons (Heroicons outline) + UPLA institutional login |
| Auth | JWT stateless (jjwt 0.12.6) |
| GitHub | https://github.com/LuchitoAE/Sisexp-Upla-SpringBoot |
| Deploy Frontend | Vercel: https://frontend-ivory-nine-43.vercel.app |
| Deploy Backend | Railway: https://api-gateway-production-e01a.up.railway.app |
| Working Dir | `E:\proyecto\UPLA - Clases\octavo ciclo\arquitectura de software\semana 10\Proyecto-spring boot\` |

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
| Presupuesto | presupuesto-service | 8082 | Techos, POI, PAP, NotasModif, Dashboard, Reportes, RestTemplate |
| Expedientes | expediente-service | 8083 | CRUD expedientes, documentos, seguimiento estados, publica eventos |
| Notificaciones | notificacion-service | 8084 | Consume eventos RabbitMQ, crea/consulta notificaciones |
| Ruteo | api-gateway | 8080 | Punto unico de entrada, JWT global, CORS, ruteo load-balanced |

---

## Acceso al Sistema

| URL | Que es |
|:----|:-------|
| `http://localhost` | SISEXP-UPLA React SPA (login, dashboard, CRUD) |
| `http://localhost/monitor` | Dashboard de monitoreo (12 nodos en tiempo real) |
| `http://localhost/api/status` | API: estado de los 7 nodos |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:15672` | RabbitMQ Management (sisexp/sisexp) |
| `https://frontend-ivory-nine-43.vercel.app` | Frontend en Vercel |

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

# Datos frescos (reinicia DBs)
docker compose down -v presupuesto-db expediente-db auth-db
docker compose up -d presupuesto-db expediente-db auth-db
docker compose up -d auth-service presupuesto-service expediente-service api-gateway

# Detener
docker compose down

# Reconstruir frontend
cd frontend && pnpm install && pnpm run build && cd ..
docker compose build nginx && docker compose up -d --force-recreate nginx

# Smoke test
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from tester
```

---

## Estructura de Paquetes

```
microservicios/
├── sisexp-common/         # 9 enums compartidos (TipoNota, EstadoNota, Naturaleza, etc.)
├── eureka-server/         # Netflix Eureka
├── api-gateway/           # Spring Cloud Gateway + JwtAuthFilter + CorsConfig
├── auth-service/
│   └── model/Usuario, config/DataInitializer, controller/ApiAuth, ApiUsuario, Status
├── presupuesto-service/
│   ├── model/             # TechoPresupuestal, ActividadPOI, NecesidadPAP, NotaModificatoria
│   ├── config/            # DataInitializer (seed techos + POI + PAP + notas), RestTemplateConfig
│   ├── service/           # NotaModificatoriaService
│   └── controller/        # ApiTecho, ApiActividadPOI, ApiNecesidadPAP, Dashboard, Reportes, NotaModificatoria
├── expediente-service/
│   ├── model/             # Expediente, SeguimientoLog, DocumentoAdjunto
│   ├── config/            # DataInitializer (seed 8 expedientes), RabbitMQConfig
│   └── controller/        # ApiExpediente (CRUD, estado, rastreo, documentos)
└── notificacion-service/
    └── model/Notificacion, controller/ApiNotificacion
frontend/
├── src/
│   ├── api/client.js      # HTTP client con JWT + cache
│   ├── components/Auth/Login.js   # Login con identidad UPLA + seeds
│   ├── components/Layout/         # Sidebar (Heroicons), Header (notif bell)
│   ├── pages/                     # Dashboard, Expediente, POI, PAP, Techos, Reportes, Notas, Usuarios
│   ├── contexts/AuthContext.js    # Auth state + JWT token
│   └── utils/config.js            # Roles, permisos, modulos
├── public/config.js       # API_URL para local (/api) o Vercel
├── monitor/               # Dashboard monitoreo 12 nodos
└── nginx.conf             # Proxy / -> SPA, /api -> gateway, /monitor -> monitor
```

---

## Endpoints API (41 total)

| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | JWT |
| GET | /api/techos-presupuestales | presupuesto-service | JWT |
| GET | /api/actividades-poi/techo/{id} | presupuesto-service | JWT |
| GET | /api/necesidades-pap/actividad/{id} | presupuesto-service | JWT |
| GET/POST | /api/notas-modificatorias | presupuesto-service | JWT |
| PUT | /api/notas-modificatorias/{id}/configurar | presupuesto-service | JWT |
| PUT | /api/notas-modificatorias/{id}/rechazar | presupuesto-service | JWT |
| GET | /api/dashboard/alertas | presupuesto-service | JWT |
| GET | /api/dashboard/saldos | presupuesto-service | JWT |
| GET | /api/reportes/anual/{anio} | presupuesto-service | JWT |
| GET | /api/reportes/expedientes?anio= | presupuesto-service | JWT |
| GET | /api/reportes/poi?anio= | presupuesto-service | JWT |
| GET | /api/reportes/pap?anio= | presupuesto-service | JWT |
| GET | /api/reportes/poi/{id} | presupuesto-service | JWT |
| GET | /api/reportes/pap/{id} | presupuesto-service | JWT |
| GET/POST | /api/expedientes | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/expedientes/rastreo/{codigo} | expediente-service | No |
| GET | /api/notificaciones | notificacion-service | JWT |
| GET | /api/status | auth-service | No |

---

## Seed Data

| Servicio | Cantidad | Detalle |
|:---------|:---------|:--------|
| auth-service | 6 usuarios | 1 por cada rol |
| presupuesto-service | 5 techos, 20 POI, 80 PAP, 4 notas | 2022-2026, S/1.5M en 2026 (50% usado) |
| expediente-service | 8 expedientes + 6 logs | 7 estados distintos, urgencias variadas |

---

## Data Types (post-auditoria)

| Entidad | Campo | Tipo | Justificacion |
|:--------|:------|:-----|:--------------|
| NotaModificatoria | tipo | TipoNota enum | inclusion_item / inclusion_actividad |
| NotaModificatoria | estado | EstadoNota enum | pendiente / configurada / rechazada |
| NotaModificatoria | nuevoTipo | Naturaleza enum | Bien / Servicio |
| NotaModificatoria | justificacion | TEXT | Sin limite arbitrario (era length=2000) |
| DocumentoAdjunto | tamano | Long | Archivos >2GB (era Integer, max 2.1GB) |
| Usuario | email | length=254 | RFC 5321 |
| Usuario | nombre | length=150 | Acotado |

Todos los montos: BigDecimal precision=12 scale=2. Enums: @Enumerated(EnumType.STRING). FK entre servicios: Long (sin @ManyToOne).

---

## Railway Deployment

| Item | Estado |
|:-----|:------|
| TODOS (12/12) | SUCCESS |
| auth-db, presupuesto-db, expediente-db, notificacion-db | SUCCESS |
| rabbitmq, eureka-server, api-gateway | SUCCESS |
| auth-service, presupuesto-service | SUCCESS |
| expediente-service, notificacion-service | SUCCESS |
| nginx | No necesario (frontend en Vercel) |

**URL publica API Gateway:** `https://api-gateway-production-e01a.up.railway.app`

**Railway CLI v4.30.5**: interactiva, usar flags `--service`, `--variables`, `--image`, `-y`.

**Fixes aplicados:**
- `EUREKA_INSTANCE_HOSTNAME` = `RAILWAY_PRIVATE_DOMAIN` en cada servicio (sin esto, Eureka registra el container ID que no es resoluble)
- `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` usa el private domain real del eureka-server (`sisexp-upla-springboot.railway.internal`, no `eureka-server.railway.internal`)
- `railway add --image postgres:16-alpine --service {name} --variables "..."` para crear DBs
- `railway domain --service api-gateway --port 8080` para exponer el gateway publicamente
- `railway service redeploy --service {name} -y` para redesplegar

Proyecto ID: `38350e4a-d078-4836-bf40-290719260fde`

---

## Frontend Design

- **Libreria de iconos**: react-icons 5.7.0 (Heroicons outline style)
- **Sidebar**: HiOutlineChartBar, HiOutlineFolderOpen, HiOutlineCurrencyDollar, etc.
- **Header**: HiOutlineBell (notificaciones), HiOutlineLogout
- **Login**: UPLA logo animado + fondos institucionales + seeds rapidos
- **Estilo**: Intranet ejecutiva UPLA (Propuesta A)

---

## Skills Disponibles

### Backend & Arquitectura
- `arquitectura-microservicios` — Clean Architecture, SOLID, patrones GoF
- `backend-sisexp` — Spring Boot, JPA, servicios, repos, enums, data types
- `deploy-sisexp` — Docker, Railway (monolito legacy, no microservicios)

### Documentacion
- `docs-sisexp` — MD -> DOCX profesional con reference.docx + pandoc

### Frontend & UX/UI
- `frontend-sisexp` — React SPA, login, componentes, auth flow
- `ux-ui-design` — Thymeleaf + Bootstrap (legacy del monolito)
- `frontend-accessibility-inclusive-design` — Accesibilidad WCAG
- `frontend-ui-visual-composition` — Jerarquia visual, tipografia, color
- `frontend-ux-usability-foundations` — Affordances, feedback, prevencion errores
- `frontend-interaction-patterns-components` — Patrones de interaccion
- `frontend-forms-inputs-checkout` — Formularios, validacion
- `frontend-information-architecture-navigation` — Navegacion, breadcrumbs
- `frontend-ux-writing-content-design` — Microcopy, CTAs, empty states
- `frontend-design-systems-frontend-architecture` — Design tokens, componentes

---

## Documentos

| Archivo | Descripcion |
|:--------|:------------|
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Documentacion completa de arquitectura |
| `docs/INFORME_MICROSERVICIOS_SISEXP.docx` | Version Word |
| `README.md` | Readme del repo con setup, endpoints, seed data |
| `docker-compose.test.yml` | Smoke test 21 endpoints |
