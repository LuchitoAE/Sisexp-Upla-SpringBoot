# AGENTS.md — SISEXP-UPLA (Spring Boot Microservicios) — vFinal

## PROYECTO ACTUAL: SISEXP-UPLA Microservicios

**Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes**

Arquitectura de microservicios con 12 contenedores Docker Compose. Proyecto final del curso Arquitectura de Software (VIII Ciclo).

| Dato | Valor |
|---|---|
| Dominio | Gestion presupuestal de expedientes (Techo -> POI -> PAP -> Expedientes) |
| Entidades | 9 (Usuario, TechoPresupuestal, ActividadPOI, NecesidadPAP, Expediente, DocumentoAdjunto, SeguimientoLog, NotaModificatoria, Notificacion) |
| Enums | 10 (RolUsuario, EstadoExpediente, Urgencia, Naturaleza, EstadoActividad, TipoDocumento, TipoNotificacion, TipoNota, EstadoNota, ActivityAction) |
| Roles | 6 (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato) |
| Estados expediente | 7 (Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) |
| Frontend | React 19 SPA CRA + NGINX + react-icons 5.7.0 (Heroicons outline) + UPLA login |
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
| 2 | sisexp-api-gateway | 8080 | Spring Cloud Gateway + JwtAuthFilter + ActivityLogFilter + MonitorController |
| 3 | sisexp-eureka | 8761 | Netflix Eureka (Service Discovery) |
| 4 | sisexp-auth-service | 8081 | Spring Boot + PostgreSQL |
| 5 | sisexp-presupuesto-service | 8082 | Spring Boot + PostgreSQL + RestTemplate |
| 6 | sisexp-expediente-service | 8083 | Spring Boot + PostgreSQL + RabbitMQ |
| 7 | sisexp-notificacion-service | 8084 | Spring Boot + PostgreSQL + RabbitMQ |
| 8-11 | 4 PostgreSQL | 5433-5436 | postgres:16-alpine |
| 12 | sisexp-rabbitmq | 5672 | rabbitmq:3-management-alpine |

---

## Bounded Contexts

| Contexto | Servicio | Puerto | Responsabilidad |
|:---------|:---------|:------:|:----------------|
| Autenticacion | auth-service | 8081 | Login JWT, gestion usuarios/roles, validacion tokens, StatusController |
| Presupuesto | presupuesto-service | 8082 | Techos, POI, PAP, NotasModif, Dashboard, Reportes, RestTemplate a expediente-service |
| Expedientes | expediente-service | 8083 | CRUD expedientes, documentos, seguimiento estados, publica eventos RabbitMQ |
| Notificaciones | notificacion-service | 8084 | Consume eventos RabbitMQ, crea/consulta notificaciones |
| Monitoreo | api-gateway | 8080 | ActivityLogFilter intercepta todas las llamadas, MonitorController sirve feed de actividad |
| Ruteo | api-gateway | 8080 | Punto unico de entrada, JWT global, CORS, ruteo load-balanced |

---

## Acceso al Sistema

| URL | Que es |
|:----|:-------|
| `http://localhost` | SISEXP-UPLA React SPA (login, dashboard, CRUD, monitor) |
| `http://localhost/api/monitor/activity?since=5` | API: actividad en tiempo real ultimos N minutos |
| `http://localhost/api/status` | API: estado de los 7 nodos |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:15672` | RabbitMQ Management (sisexp/sisexp) |
| `https://frontend-ivory-nine-43.vercel.app` | Frontend en Vercel |
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

## Comandos Rapidos

```bash
# Construir y levantar todo
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

# Reconstruir solo frontend (el cambio mas frecuente)
cd frontend && pnpm install && pnpm run build && cd ..
docker compose build nginx && docker compose up -d --force-recreate nginx

# Reconstruir api-gateway (cambios de backend)
docker compose build api-gateway && docker compose up -d --force-recreate api-gateway

# Smoke test automatizado
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from tester

# Deploy Vercel (forzar)
cd frontend && npx vercel deploy --prod --yes && cd ..

# Railway (1 solo servicio a la vez, CLI interactiva)
railway service redeploy --service api-gateway -y
railway variables set KEY="VALUE" --service api-gateway
railway add --image postgres:16-alpine --service {name} --variables "K=V"
railway domain --service api-gateway --port 8080
```

---

## Estructura de Paquetes

```
microservicios/
├── sisexp-common/         # 10 enums compartidos (TipoNota, EstadoNota, Naturaleza, etc.)
├── eureka-server/         # Netflix Eureka (+ railway.toml)
├── api-gateway/           # Spring Cloud Gateway + JwtAuthFilter + CorsConfig + ActivityLogFilter + MonitorController
│   ├── JwtAuthFilter.java     # JWT validation, exime /api/auth/login, /api/health, /api/rastreo, /api/status, /api/monitor
│   ├── CorsConfig.java        # allowCredentials=false, allowedOriginPatterns=*
│   ├── ActivityLogFilter.java # GlobalFilter orden 10: intercepta todo, guarda eventos en ActivityBuffer
│   ├── ActivityBuffer.java    # Buffer circular 200 eventos thread-safe, queries por tiempo/servicio
│   ├── ActivityEvent.java     # POJO: timestamp, service, action, description, path, status, userEmail
│   └── MonitorController.java # GET /api/monitor/activity?since=N, GET /api/monitor/activity/service?name=X&since=N
├── auth-service/
│   └── model/Usuario, config/DataInitializer, controller/ApiAuth, ApiUsuario, Status
├── presupuesto-service/
│   ├── model/             # TechoPresupuestal, ActividadPOI, NecesidadPAP, NotaModificatoria
│   ├── config/            # DataInitializer (seed techos + POI + PAP + notas), RestTemplateConfig
│   ├── service/           # NotaModificatoriaService
│   └── controller/        # ApiTecho, ApiActividadPOI, ApiNecesidadPAP, Dashboard, Reportes, NotaModificatoria
├── expediente-service/
│   ├── model/             # Expediente, SeguimientoLog, DocumentoAdjunto
│   ├── config/            # DataInitializer (seed 8 expedientes), RabbitMQConfig, RestTemplateConfig
│   ├── service/           # ExpedienteService (crear, estado, docs, disponibilidad, generarNumero)
│   └── controller/        # ApiExpediente (CRUD + estado + rastreo + documentos + disponibilidad)
└── notificacion-service/
    └── model/Notificacion, controller/ApiNotificacion, consumer/ExpedienteEventConsumer

frontend/
├── src/
│   ├── api/client.js          # HTTP client con JWT + cache + interceptor de grabacion (recording)
│   ├── components/Auth/Login.js       # Login identidad UPLA + seeds rapidos
│   ├── components/Layout/
│   │   ├── Sidebar.js         # Heroicons outline, item Monitor (todos los roles), colapsable
│   │   └── Header.js          # Botones Monitor + Grabar, campana notificaciones, avatar
│   ├── pages/
│   │   ├── Dashboard.js       # KPI Cards por año (selector, por defecto ultimo año activo)
│   │   ├── ExpedientePage.js  # CRUD expedientes + documentos + cambio de estado
│   │   ├── ActividadPOIPage.js
│   │   ├── NecesidadPAPPage.js
│   │   ├── TechoPresupuestalPage.js
│   │   ├── ReportesPage.js
│   │   ├── NotaModificatoriaPage.js
│   │   ├── UsuariosPage.js
│   │   └── MonitorPage.js     # Pantalla completa: canvas 12 nodos + edges + feed actividad + grabaciones/replay
│   ├── contexts/
│   │   ├── AuthContext.js      # Auth state + JWT token
│   │   └── RecorderContext.js  # Estado grabacion (start/stop/buffer/localStorage)
│   └── utils/config.js         # Roles, NAV_MODULES, NAV_PERMISSIONS (incluye monitor)
├── public/config.js            # API_URL: detecta localhost vs Vercel, setea URL correcta
└── nginx.conf                  # Proxy / -> SPA, /api -> gateway (sin bloque /monitor)
```

---

## Endpoints API (44 total)

| Metodo | Ruta | Servicio | Auth |
|:-------|:-----|:---------|:----:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | JWT |
| GET | /api/status | auth-service | No |
| GET | /api/health | auth-service | No |
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
| GET | /api/expedientes/disponibilidad/{poiId}/{papId}?cantidad= | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/expedientes/rastreo/{codigo} | expediente-service | No |
| GET | /api/notificaciones?usuarioId= | notificacion-service | JWT |
| GET | /api/notificaciones/count?usuarioId= | notificacion-service | JWT |
| GET | /api/monitor/activity?since=5 | api-gateway (local) | No |
| GET | /api/monitor/activity/service?name=X&since=5 | api-gateway (local) | No |

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

## Railway Deployment — 12/12 SUCCESS

| Item | Estado | Nota |
|:-----|:------|:-----|
| auth-db | SUCCESS | postgres:16-alpine |
| presupuesto-db | SUCCESS | postgres:16-alpine |
| expediente-db | SUCCESS | postgres:16-alpine, recreado via `railway add` |
| notificacion-db | SUCCESS | postgres:16-alpine, recreado via `railway add` |
| rabbitmq | SUCCESS | rabbitmq:3-management-alpine |
| eureka-server | SUCCESS | Puerto 8761, private domain configurado |
| api-gateway | SUCCESS | Dominio publico: api-gateway-production-e01a.up.railway.app |
| auth-service | SUCCESS | + JWT_EXPIRATION, conecta a auth-db |
| presupuesto-service | SUCCESS | + RestTemplate, conecta a presupuesto-db |
| expediente-service | SUCCESS | + RabbitMQ, conecta a expediente-db |
| notificacion-service | SUCCESS | + RabbitMQ, conecta a notificacion-db |
| nginx | No necesario | Frontend en Vercel, no se necesita nginx en Railway |

**Proyecto ID:** `38350e4a-d078-4836-bf40-290719260fde`

### Variables criticas por servicio

| Servicio | Variable | Valor |
|:---------|:---------|:------|
| TODOS los servicios | EUREKA_CLIENT_SERVICEURL_DEFAULTZONE | `http://sisexp-upla-springboot.railway.internal:8761/eureka` |
| auth-service | EUREKA_INSTANCE_HOSTNAME | `auth-service.railway.internal` |
| presupuesto-service | EUREKA_INSTANCE_HOSTNAME | `presupuesto-service.railway.internal` |
| expediente-service | EUREKA_INSTANCE_HOSTNAME | `heartfelt-wonder.railway.internal` |
| notificacion-service | EUREKA_INSTANCE_HOSTNAME | `notificacion-service.railway.internal` |
| TODOS | JWT_SECRET | `SisexpJwtSecret2026MicroservicesKey!` |
| auth-service | JWT_EXPIRATION | `86400000` |
| auth-service | SPRING_DATASOURCE_URL | `jdbc:postgresql://auth-db.railway.internal:5432/auth_db` |
| presupuesto-service | SPRING_DATASOURCE_URL | `jdbc:postgresql://presupuesto-db.railway.internal:5432/presupuesto_db` |
| expediente-service | SPRING_DATASOURCE_URL | `jdbc:postgresql://expediente-db.railway.internal:5432/expediente_db` |
| notificacion-service | SPRING_DATASOURCE_URL | `jdbc:postgresql://notificacion-db.railway.internal:5432/notific_db` |
| expediente-service | SPRING_RABBITMQ_HOST | `rabbitmq.railway.internal` |
| expediente-service | PRESUPUESTO_SERVICE_URL | `http://presupuesto-service:8082` (Docker) / `http://presupuesto-service.railway.internal:8082` (Railway) |
| notificacion-service | SPRING_RABBITMQ_HOST | `rabbitmq.railway.internal` |

### Railway CLI — Patrones seguros

```bash
# v4.30.5 es interactiva. Usar flags, NO argumentos posicionales.
# NO abrir multiples ventanas de Railway (rate limit 1015 Cloudflare)

# Crear DBs
railway add --image "postgres:16-alpine" --service "expediente-db" --variables "POSTGRES_DB=expediente_db" --variables "POSTGRES_PASSWORD=sisexp"

# Redesplegar
railway service redeploy --service api-gateway -y

# Variables
railway variables set KEY="value" --service api-gateway
railway variables --service api-gateway  # listar

# Status
railway service status --all

# Dominio publico
railway domain --service api-gateway --port 8080
```

### Railway — Lecciones aprendidas

1. **EUREKA_INSTANCE_HOSTNAME es obligatorio**: sin esto, los servicios registran su container ID en Eureka (ej: `1cd254d6997e`), que NO es resoluble por otros servicios. Causa `UnknownHostException`.
2. **EUREKA_CLIENT_SERVICEURL_DEFAULTZONE usa el private domain real**: `sisexp-upla-springboot.railway.internal` NO `eureka-server.railway.internal`. El RAILWAY_PRIVATE_DOMAIN del eureka-server puede ser diferente al nombre del servicio.
3. **CORS en Gateway usa `allowCredentials=false`**: incompatible con `*` origins. Se configuro `addAllowedOriginPattern("*")` con `setAllowCredentials(false)`.
4. **Railway CLI v4.30.5**: no soporta flags en modo no-TTY en algunos comandos. `railway add` y `railway service redeploy` si aceptan `--service` flag.
5. **No ngrok**: el free tier muestra pagina interstitial que bloquea CORS preflight. Se migro a Railway con dominio publico.

---

## Frontend Design

### Paleta y estilo
- **Sidebar**: `#0f172a` fondo, items con Heroicons outline, indicador activo `#3b82f6`
- **Header**: fondo blanco, botones Monitor (gris) y Grabar (toggle rojo pulsante), campana notificaciones
- **Dashboard**: KPI cards blancas con bordes grises, selector de año dropdown
- **Monitor**: dark theme `#0a0f14`, canvas con edges animados, nodos arrastrables
- **Login**: logo UPLA animado + fondos institucionales

### Iconos (react-icons 5.7.0)
- Sidebar: `HiOutlineChartBar`, `HiOutlineFolderOpen`, `HiOutlineCurrencyDollar`, `HiOutlineClipboardList`, `HiOutlineArchive`, `HiOutlineChartPie`, `HiOutlineUsers`, `HiOutlinePencilAlt`, `HiOutlineDesktopComputer`
- Header: `HiOutlineBell`, `HiOutlineDesktopComputer`, `HiOutlineVideoCamera`, `HiOutlineStop`, `HiOutlineLogout`

### Navegacion
- SPA con `useState('active')` (no React Router)
- Lazy loading con `React.lazy()` + `Suspense`
- 9 modulos: dashboard, expedientes, techos, poi, pap, reportes, notas, usuarios, monitor
- Monitor: vista pantalla completa (sin sidebar ni header), boton "← Volver"

### Sistema de Grabacion/Replay
- **RecorderContext.js**: estado global `{ isRecording, elapsed, recordings }`
- Boton **Grabar** en Header: toggle `HiOutlineVideoCamera` ↔ `HiOutlineStop` con dot rojo pulsante
- Al grabar: `client.js` intercepta todas las llamadas API y guarda `{ ts, method, path, status, bodySnapshot }`
- Al detener: se guarda en `localStorage.sisexp_recordings` con nombre y timestamp
- Panel **Grabaciones** en Monitor: lista de sesiones guardadas con boton ▶ Reproducir
- Modo **Reproduccion**: ejecuta acciones secuencialmente con delay 500ms, ilumina nodos afectados, velocidad 1x/2x/4x

### Activity Feed (Monitor)
- **ActivityLogFilter.java** en api-gateway: GlobalFilter (orden 10) que intercepta TODAS las peticiones
- Traduce path + metodo → descripcion humana (ej: "jefe@upla.edu.pe creo un nuevo expediente")
- Almacena en **ActivityBuffer** (buffer circular 200 eventos thread-safe)
- **MonitorController**: `GET /api/monitor/activity?since=5` (publico, sin JWT)
- **MonitorPage**: polling cada 5s, timeline scrollable con dots verdes/rojos, filtrado por servicio al clickear nodo

### Monitor — Canvas de 12 nodos
- 12 nodos arrastrables (drag & drop, posiciones guardadas en localStorage)
- 16 edges animados (solid=gateway routing, dashed=DB, dotted=Eureka/RabbitMQ)
- Click en nodo → panel lateral con status, host, acciones recientes, componentes
- Click en espacio vacio → cierra panel
- Top bar: contador UP/DOWN, latencia, boton Pausar/Sondear
- Nodos de servicio tienen boton "Ir al modulo →" que navega a la pagina correspondiente

### Config.js — Deteccion de entorno
```js
window.__SISEXP_CONFIG__ = {
  API_URL: (function() {
    var host = window.location.hostname;
    if (host === "localhost" || host === "127.0.0.1" || host.endsWith(".local")) {
      return "/api";  // Docker local: nginx proxy a api-gateway:8080
    }
    return "https://api-gateway-production-e01a.up.railway.app/api";  // Vercel: directo a Railway
  })()
};
```

---

## Documentos

| Archivo | Descripcion |
|:--------|:------------|
| `AGENTS.md` | Este archivo — contexto completo del proyecto |
| `README.md` | Readme del repo con setup, endpoints, seed data |
| `docker-compose.yml` | Stack local 12 contenedores |
| `docker-compose.test.yml` | Smoke test automatizado 21 endpoints |
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Documentacion completa de arquitectura |
| `docs/INFORME_MICROSERVICIOS_SISEXP.docx` | Version Word |
| `frontend/vercel.json` | Config de deploy Vercel (CRA, build command) |
| `microservicios/*/railway.toml` | Config-as-code Railway por servicio (6 archivos) |
