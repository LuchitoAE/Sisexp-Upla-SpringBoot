# SISEXP-UPLA — Microservicios

Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes

Arquitectura de microservicios con 12 contenedores Docker Compose. Proyecto final del curso Arquitectura de Software (VIII Ciclo).

---

## Stack

| Capa | Tecnologia |
|:-----|:-----------|
| Backend | Spring Boot 3.4.1, Java 17, Spring Cloud Gateway, JWT (jjwt 0.12.6) |
| Frontend | React 19 SPA CRA + NGINX + react-icons 5.7.0 (Heroicons outline) |
| BD | PostgreSQL 16-alpine (4 instancias: auth, presupuesto, expediente, notificacion) |
| Mensajeria | RabbitMQ 3-management-alpine |
| Service Discovery | Netflix Eureka |
| Contenedores | Docker + Docker Compose (12) |
| Deploy Frontend | Vercel: https://frontend-ivory-nine-43.vercel.app |
| Deploy Backend | Railway: https://api-gateway-production-e01a.up.railway.app |

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

# Reconstruir api-gateway
docker compose build api-gateway && docker compose up -d --force-recreate api-gateway

# Reconstruir expediente-service
docker compose build expediente-service && docker compose up -d --force-recreate expediente-service

# Ver estado
docker compose ps

# Smoke test automatizado (21 endpoints)
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from tester
```

---

## Acceso

| URL | Que es |
|:----|:-------|
| `http://localhost` | SISEXP-UPLA React SPA (login, dashboard, CRUD, monitor) |
| `http://localhost/api/monitor/activity?since=5` | API: actividad en tiempo real ultimos N minutos |
| `http://localhost/api/status` | API: estado de los 7 nodos |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:15672` | RabbitMQ Management (sisexp/sisexp) |
| `https://frontend-ivory-nine-43.vercel.app` | Frontend en Vercel (produccion) |
| `https://api-gateway-production-e01a.up.railway.app` | API Gateway publico en Railway |

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
| api-gateway | 8080 | — | JWT, CORS, ruteo, ActivityLogFilter, MonitorController |
| eureka-server | 8761 | — | Service Discovery |
| auth-service | 8081 | 5433 | Login JWT, usuarios, roles, status |
| presupuesto-service | 8082 | 5434 | Techos, POI, PAP, notas modif., dashboard, reportes, RestTemplate |
| expediente-service | 8083 | 5435 | Expedientes, documentos, seguimiento, disponibilidad, RabbitMQ publisher |
| notificacion-service | 8084 | 5436 | Consume eventos RabbitMQ, notificaciones |
| rabbitmq | 5672/15672 | — | Mensajeria async |

---

## Estructura del proyecto

```
microservicios/
├── sisexp-common/         # 10 enums compartidos + excepciones
├── eureka-server/         # Netflix Eureka (+ railway.toml)
├── api-gateway/           # Spring Cloud Gateway + JwtAuthFilter + CorsConfig
│   ├── ActivityLogFilter.java    # GlobalFilter: intercepta todo, traduce a descripcion humana
│   ├── ActivityBuffer.java       # Buffer circular 200 eventos thread-safe
│   ├── ActivityEvent.java        # POJO: timestamp, service, action, description, path, status, userEmail
│   └── MonitorController.java    # GET /api/monitor/activity?since=N, /service?name=X&since=N
├── auth-service/
│   └── model/, config/, controller/ (ApiAuth, ApiUsuario, Status)
├── presupuesto-service/
│   ├── model/             # TechoPresupuestal, ActividadPOI, NecesidadPAP, NotaModificatoria
│   ├── config/            # DataInitializer, RestTemplateConfig
│   ├── service/           # BusinessRulesService, NotaModificatoriaService
│   └── controller/        # ApiTecho, ApiActividadPOI, ApiNecesidadPAP, Dashboard, Reportes, NotaModificatoria
├── expediente-service/
│   ├── model/             # Expediente, SeguimientoLog, DocumentoAdjunto
│   ├── config/            # DataInitializer (8 seed), RabbitMQConfig, RestTemplateConfig
│   ├── service/           # ExpedienteService (crear, estado, docs, disponibilidad, generarNumero)
│   └── controller/        # ApiExpediente (CRUD, estado, rastreo, documentos, disponibilidad)
└── notificacion-service/
    ├── model/             # Notificacion
    ├── consumer/          # ExpedienteEventConsumer (RabbitMQ)
    └── controller/        # ApiNotificacion

frontend/
├── src/
│   ├── api/client.js          # HTTP client con JWT + cache + interceptor grabacion
│   ├── components/Auth/Login.js       # Login UPLA + seeds rapidos
│   ├── components/Layout/
│   │   ├── Sidebar.js         # Heroicons outline, item Monitor (todos roles), colapsable
│   │   └── Header.js          # Botones Monitor + Grabar, campana notif, avatar
│   ├── pages/
│   │   ├── Dashboard.js       # KPI Cards por año con selector
│   │   ├── ExpedientePage.js  # CRUD + docs + estado + disponibilidad
│   │   ├── ActividadPOIPage.js, NecesidadPAPPage.js, TechoPresupuestalPage.js
│   │   ├── ReportesPage.js, NotaModificatoriaPage.js, UsuariosPage.js
│   │   └── MonitorPage.js     # Pantalla completa: canvas 12 nodos + edges + feed actividad + grabaciones
│   ├── contexts/
│   │   ├── AuthContext.js      # Auth state + JWT token
│   │   └── RecorderContext.js  # Estado grabacion (start/stop/buffer/localStorage)
│   └── utils/config.js         # Roles, NAV_MODULES, NAV_PERMISSIONS (incluye monitor)
├── public/config.js            # API_URL runtime: detecta local (/api) vs Vercel (Railway URL)
└── nginx.conf                  # Proxy / → SPA, /api → gateway
```

---

## API Endpoints (43 total)

### Auth & Usuarios
| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | JWT |
| GET | /api/status | auth-service | No |
| GET | /api/health | auth-service | No |

### Presupuesto
| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
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

### Expedientes
| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| GET/POST | /api/expedientes | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/expedientes/rastreo/{codigo} | expediente-service | No |
| GET | /api/expedientes/disponibilidad/{poiId}/{papId}?cantidad= | expediente-service | JWT |
| POST | /api/expedientes/{id}/documentos | expediente-service | JWT |

### Notificaciones
| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| GET | /api/notificaciones?usuarioId= | notificacion-service | JWT |
| GET | /api/notificaciones/count?usuarioId= | notificacion-service | JWT |

### Monitor (publico)
| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| GET | /api/monitor/activity?since=5 | api-gateway | No |
| GET | /api/monitor/activity/service?name=X&since=5 | api-gateway | No |

---

## Seed Data

| Servicio | Datos |
|:---------|:------|
| auth | 6 usuarios (1 por rol), passwords BCrypt |
| presupuesto | 5 techos (2022-2026, S/ 1.5M en 2026), 20 actividades POI, 80 necesidades PAP, 4 notas modificatorias |
| expediente | 8 expedientes en 7 estados distintos, 6 seguimiento logs |

---

## Frontend Design

### Sistema de Monitor
- **Actividad en tiempo real**: ActivityLogFilter en gateway intercepta todas las llamadas, ActivityBuffer almacena 200 eventos, MonitorController expone `/api/monitor/activity`
- **Canvas de 12 nodos**: arrastrables (drag & drop con persistencia en localStorage), 16 edges animados
- **Panel de detalle**: click en nodo muestra status, host, acciones recientes, componentes, boton "Ir al modulo →"
- **Activity Feed**: timeline scrollable con dots verdes/rojos, polling cada 5s

### Sistema de Grabacion/Replay
- Boton **Grabar** en Header (toggle rojo pulsante con timer)
- `client.js` intercepta todas las llamadas API y guarda `{ts, method, path, status, bodySnapshot}`
- Grabaciones guardadas en `localStorage.sisexp_recordings`
- Panel **Grabaciones** en Monitor: lista con boton ▶ Reproducir
- Modo **Reproduccion**: ejecuta acciones secuencialmente (500ms delay), ilumina nodos, velocidad 1x/2x/4x

### Navegacion
- SPA con `useState('active')` (no React Router)
- Lazy loading con `React.lazy()` + `Suspense`
- 9 modulos: dashboard, expedientes, techos, poi, pap, reportes, notas, usuarios, monitor
- Monitor: pantalla completa sin sidebar ni header, boton "← Volver"
- Sidebar con Heroicons outline, colapsable, item Monitor visible para todos los roles

### Config.js — Deteccion de entorno
```js
window.__SISEXP_CONFIG__ = {
  API_URL: (function() {
    var host = window.location.hostname;
    if (host === "localhost" || host === "127.0.0.1" || host.endsWith(".local")) {
      return "/api";  // Docker local: nginx proxy a api-gateway:8080
    }
    return "https://api-gateway-production-e01a.up.railway.app/api";  // Vercel → Railway
  })()
};
```

---

## Deploy

| Componente | Plataforma | Estado |
|:-----------|:----------|:------|
| Frontend | Vercel | OK |
| Backend (local) | Docker Compose | OK |
| Backend (cloud) | Railway | OK (12/12) |

### Railway — Lecciones aprendidas

1. **EUREKA_INSTANCE_HOSTNAME es obligatorio**: sin esto los servicios registran su container ID (ej: `1cd254d6997e`), que no es resoluble → `UnknownHostException`
2. **EUREKA_CLIENT_SERVICEURL_DEFAULTZONE** usa el private domain real de Railway (`sisexp-upla-springboot.railway.internal`), no el nombre del servicio
3. **CORS**: `allowCredentials=false` + `allowedOriginPatterns=*` (incompatible con `*` origins con credentials)
4. **Railway CLI v4.30.5**: interactiva, usar flags `--service`, `--variables`, `--image`, `-y`. Espaciar consultas para evitar rate limit Cloudflare 1015
5. **No ngrok**: free tier muestra pagina interstitial que bloquea CORS preflight

---

## Documentacion

| Archivo | Descripcion |
|:--------|:------------|
| `AGENTS.md` | Guia completa para AI agents (contexto, skills, comandos, lecciones) |
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Documentacion completa de arquitectura |
| `docs/INFORME_MICROSERVICIOS_SISEXP.docx` | Version Word |
| `docker-compose.yml` | Stack local 12 contenedores |
| `docker-compose.test.yml` | Smoke test automatizado 21 endpoints |
| `microservicios/*/railway.toml` | Config-as-code Railway (6 archivos) |
| `frontend/vercel.json` | Config deploy Vercel |

---

## Skills (para AI agents)

| Area | Skills |
|:-----|:-------|
| Backend | `arquitectura-microservicios`, `backend-sisexp`, `deploy-sisexp` |
| Frontend | `frontend-sisexp`, `ux-ui-design` |
| Documentacion | `docs-sisexp` |
