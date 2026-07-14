---
title: "SISEXP-UPLA — Arquitectura de Microservicios"
subtitle: "Universidad Peruana Los Andes — Arquitectura de Software — VIII Ciclo — 2026"
lang: es
---

# **SISEXP-UPLA — Arquitectura de Microservicios**

## **Universidad Peruana Los Andes**
### **Arquitectura de Software — VIII Ciclo — 2026**

---

# **Tabla de Contenido**

<!-- La TOC se genera automaticamente con --toc -->

---

## **1. Introduccion**

SISEXP-UPLA es un sistema de seguimiento y control de expedientes para la Universidad Peruana Los Andes. Este documento describe la **migracion de arquitectura monolitica a microservicios**, implementando 12 contenedores Docker con comunicacion REST, RabbitMQ y Service Discovery.

| **Dato** | **Valor** |
|:---------|:----------|
| Dominio | Gestion presupuestal: Techo -> POI -> PAP -> Expediente |
| Entidades | 9 (Usuario, TechoPresupuestal, ActividadPOI, NecesidadPAP, Expediente, DocumentoAdjunto, SeguimientoLog, NotaModificatoria, Notificacion) |
| Enums | 10 (RolUsuario, EstadoExpediente, Urgencia, Naturaleza, EstadoActividad, TipoDocumento, TipoNotificacion, TipoNota, EstadoNota, ActivityAction) |
| Roles | 6 (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato) |
| Estados Expediente | 7 (Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) |

---

## **2. Stack Tecnologico**

| **Capa** | **Tecnologia** | **Version** |
|:---------|:---------------|:-----------:|
| Lenguaje | Java | 17 |
| Framework | Spring Boot | 3.4.1 |
| Cloud | Spring Cloud | 2024.0.0 |
| Service Discovery | Netflix Eureka | 2024.0.0 |
| API Gateway | Spring Cloud Gateway | 2024.0.0 |
| Auth | JWT (jjwt) | 0.12.6 |
| Mensajeria | RabbitMQ | 3-management-alpine |
| Bases de datos | PostgreSQL | 16-alpine (4 instancias) |
| Contenedores | Docker + Docker Compose | 29.5 / 5.1 |
| Frontend | React 19 SPA + NGINX | — |
| Monitoreo | Spring Actuator | 3.4.1 |

---

## **3. Arquitectura de Microservicios**

![](diagramas/arquitectura-microservicios.png)

### **3.1 Diagrama de Despliegue**

![Diagrama de Despliegue](diagramas/despliegue.png)

### **3.2 Bounded Contexts**

| **Contexto** | **Servicio** | **Puerto** | **Responsabilidad** |
|:-------------|:-------------|:----------:|:--------------------|
| Autenticacion | auth-service | 8081 | Login JWT, gestion usuarios/roles, validacion tokens |
| Presupuesto | presupuesto-service | 8082 | Techos presupuestales, actividades POI, necesidades PAP, saldos |
| Expedientes | expediente-service | 8083 | CRUD expedientes, documentos, seguimiento de estados, publica eventos |
| Notificaciones | notificacion-service | 8084 | Consume eventos RabbitMQ, crea/consulta notificaciones |
| Ruteo | api-gateway | 8080 | Punto unico de entrada, filtro JWT global, ruteo a servicios |

### **3.3 Comunicacion**

| **Tipo** | **Entre** | **Protocolo** |
|:---------|:----------|:--------------|
| Sincrona | Gateway → Servicios | HTTP REST (JSON) |
| Sincrona | Servicio → Servicio | HTTP REST via Feign |
| Asincrona | expediente-service → notificacion-service | AMQP (RabbitMQ) |
| Registro | Servicios → Eureka | HTTP (Service Discovery) |

---

## **4. Clean Architecture por Servicio**

![](diagramas/componentes.png)

Cada servicio implementa **4 capas concentricas** con dependencias hacia adentro:

| **Capa** | **Clases** | **Regla** |
|:---------|:-----------|:----------|
| Domain | Entidades JPA, Enums, Value Objects | Sin dependencias externas |
| Application | Services (@Service, @Transactional) | Casos de uso, logica de negocio |
| Interface Adapters | Controllers (@RestController), Repositorios (interfaces JPA), DTOs | Traduce HTTP ↔ dominio |
| Frameworks | application.properties, Config classes | Spring Boot, PostgreSQL, RabbitMQ |

### **4.1 Ejemplo: expediente-service**

| **Capa** | **Archivo** |
|:---------|:------------|
| Domain | Expediente.java, SeguimientoLog.java, DocumentoAdjunto.java, EstadoExpediente.java |
| Application | ExpedienteService.java, BusinessRulesService.java |
| Interface Adapters | ApiExpedienteController.java, ExpedienteRepository.java, CambiarEstadoDTO.java |
| Frameworks | application.properties, RabbitMQConfig.java, FeignConfig.java |

---

## **5. Principios SOLID Aplicados**

| **Principio** | **Aplicacion en SISEXP Microservicios** |
|:--------------|:----------------------------------------|
| **S** — Single Responsibility | Cada servicio = 1 bounded context. auth-service solo autentica, presupuesto-service solo gestiona presupuestos |
| **O** — Open/Closed | Abierto a extension via RabbitMQ (nuevos consumidores sin modificar productores). Cerrado a modificacion (despliegue independiente) |
| **L** — Liskov Substitution | Interfaces JpaRepository<T, ID> → Spring Data proxies sustituibles. UserDetailsService → CustomUserDetailsService |
| **I** — Interface Segregation | Cada servicio expone solo los endpoints de su bounded context. Ninguno expone operaciones de otros contextos |
| **D** — Dependency Inversion | Services dependen de interfaces de repositorio (no implementaciones). Feign clients abstraen llamadas HTTP. Gateway rutea por service-id (Eureka) |

---

## **6. Patrones Arquitectonicos**

### **6.1 Estructurales**

| **#** | **Patron** | **Implementacion** |
|:-----:|:-----------|:-------------------|
| 1 | Layers | Controller → Service → Repository → Domain en cada microservicio |
| 2 | Client-Server | React SPA (cliente) → Spring Boot REST (servidor) |
| 3 | Pipe-Filter | Gateway filter chain: JWT validation → Routing |
| 4 | MVC | Spring MVC REST controllers en cada servicio |
| 5 | SOA | Microservicios con contratos REST bien definidos |

### **6.2 Comunicacion y Datos**

| **#** | **Patron** | **Implementacion** |
|:-----:|:-----------|:-------------------|
| 6 | API Gateway | Spring Cloud Gateway (:8080) — ruteo JWT + load-balanced |
| 7 | Service Discovery | Eureka Server (:8761) — 4 servicios registrados |
| 8 | Message Broker | RabbitMQ — eventos de dominio |
| 9 | Publish-Subscribe | expediente-service publica → notificacion-service consume |
| 10 | Database per Service | 4 PostgreSQL independientes (:5433-5436) |
| 11 | Saga (Coreografia) | Eventos RabbitMQ coordinan consistencia eventual |

### **6.3 Resiliencia**

| **#** | **Patron** | **Implementacion** |
|:-----:|:-----------|:-------------------|
| 12 | Circuit Breaker | Resilience4j en llamadas REST inter-servicio (Feign) |
| 13 | Retry | Spring Retry en RestTemplate con backoff |
| 14 | Health Check | Actuator /actuator/health en cada servicio |

---

## **7. Patrones de Diseno GoF**

| **Tipo** | **Patron** | **Donde se usa en SISEXP** |
|:---------|:-----------|:---------------------------|
| Creacional | Singleton | Beans Spring (@Service, @Repository) |
| Creacional | Factory Method | RabbitConnectionFactory, DataSourceBuilder |
| Creacional | Builder | ExpedienteDTO.builder(), JwtTokenProvider.buildToken() |
| Estructural | Adapter | JwtAuthenticationFilter adapta JWT a Spring Security |
| Estructural | Decorator | Resilience4j @CircuitBreaker sobre llamadas Feign |
| Estructural | Facade | ExpedienteService como fachada ante 3 repositorios |
| Estructural | Proxy | Spring AOP: @Transactional, @Cacheable |
| Comportamiento | Observer | @RabbitListener en notificacion-service |
| Comportamiento | Template Method | JdbcTemplate, RestTemplate, RabbitTemplate |
| Comportamiento | Strategy | BusinessRulesService.ejecutarSaldo() por Naturaleza |
| Comportamiento | Chain of Resp. | Filtros en Gateway: JWT → Rate Limit → Routing |
| Comportamiento | Command | CambiarEstadoDTO + cambiarEstado() |

---

## **8. Despliegue — Docker Compose**

### **8.1 Contenedores (12)**

| **#** | **Nombre** | **Imagen** | **Puerto** | **Health Check** |
|:-----:|:-----------|:-----------|:----------:|:-----------------|
| 1 | sisexp-nginx | nginx:alpine | 80 | wget / |
| 2 | sisexp-api-gateway | build | 8080 | curl /actuator/health |
| 3 | sisexp-eureka | build | 8761 | wget /actuator/health |
| 4 | sisexp-auth-service | build | 8081 | curl /actuator/health |
| 5 | sisexp-presupuesto-service | build | 8082 | curl /actuator/health |
| 6 | sisexp-expediente-service | build | 8083 | curl /actuator/health |
| 7 | sisexp-notificacion-service | build | 8084 | curl /actuator/health |
| 8 | sisexp-auth-db | postgres:16-alpine | 5433 | pg_isready |
| 9 | sisexp-presupuesto-db | postgres:16-alpine | 5434 | pg_isready |
| 10 | sisexp-expediente-db | postgres:16-alpine | 5435 | pg_isready |
| 11 | sisexp-notificacion-db | postgres:16-alpine | 5436 | pg_isready |
| 12 | sisexp-rabbitmq | rabbitmq:3-management-alpine | 5672, 15672 | rabbitmq-diagnostics |

### **8.2 Orden de Arranque**

```
1. Infrastructure: 4 PostgreSQL + RabbitMQ + Eureka
2. Servicios: auth-service, presupuesto-service
3. Servicios (dependen de RabbitMQ): expediente-service, notificacion-service
4. Gateway: api-gateway
5. Frontend: nginx
```

### **8.3 Comandos**

```bash
cd sisexp-microservicios

# Construir todas las imagenes
docker compose build

# Iniciar todos los servicios en background
docker compose up -d

# Ver estado de los contenedores
docker compose ps

# Ver logs de un servicio
docker compose logs -f auth-service

# Detener todo
docker compose down
```

---

## **9. Monitoreo en Tiempo Real**

El dashboard de monitoreo esta en `http://localhost` (servido por NGINX):

- **12 nodos** visualizados con iconos, puertos y tecnologias
- **Estado en vivo**: poll-ea `/api/status` cada 5 segundos
- **Grafica de actividad**: ActivityLogFilter intercepta todas las llamadas, ActivityBuffer 200 eventos, `/api/monitor/activity`
- **Indicadores**: verde = UP, amarillo = STARTING, rojo = DOWN
- **Click en nodo**: muestra componentes (db, rabbit, disk, ping, ssl) con panel detallado
- **Boton Pausar/Reanudar**: detiene el polling
- **Boton Sondear Ahora**: consulta inmediata
- **Header**: UP/DOWN count + latencia en ms
- **Resumen**: "Todo Healthy" / "Problemas Detectados"

---

## **10. Endpoint /api/status**

**GET /api/status** (publico, sin autenticacion)

Respuesta de ejemplo con los 4 servicios UP:

```json
{
  "AUTH-SERVICE": {
    "status": "UP",
    "instances": 1,
    "host": "58e2221c1a43",
    "port": 8081,
    "components": {"db":"UP","discoveryComposite":"UP","diskSpace":"UP","ping":"UP","ssl":"UP"}
  },
  "PRESUPUESTO-SERVICE": {
    "status": "UP",
    "instances": 1,
    "host": "41feeee35265",
    "port": 8082,
    "components": {"db":"UP","discoveryComposite":"UP","diskSpace":"UP","ping":"UP","ssl":"UP"}
  },
  "EXPEDIENTE-SERVICE": {
    "status": "UP",
    "instances": 1,
    "host": "1420ea9ecd60",
    "port": 8083,
    "components": {"db":"UP","rabbit":"UP","discoveryComposite":"UP","diskSpace":"UP","ping":"UP","ssl":"UP"}
  },
  "NOTIFICACION-SERVICE": {
    "status": "UP",
    "instances": 1,
    "host": "a5da0a34207c",
    "port": 8084,
    "components": {"db":"UP","rabbit":"UP","discoveryComposite":"UP","diskSpace":"UP","ping":"UP","ssl":"UP"}
  },
  "summary": {"total":4,"up":4,"down":0,"healthy":true},
  "timestamp": 1782844925013,
  "elapsedMs": 27
}
```

---

## **11. Despliegue en Produccion — Railway + Vercel**

El sistema se despliega en dos plataformas cloud:

| **Componente** | **Plataforma** | **URL** |
|:---------------|:---------------|:--------|
| Frontend React SPA | Vercel | `https://frontend-ivory-nine-43.vercel.app` |
| Backend (API Gateway) | Railway | `https://api-gateway-production-e01a.up.railway.app` |
| Backend (servicios) | Railway | 6 servicios internos (private domain) |

### **11.1 Frontend en Vercel**

```bash
cd frontend && npx vercel deploy --prod --yes && cd ..
```

El `public/config.js` detecta automaticamente el entorno:
```js
API_URL = (function() {
  var host = window.location.hostname;
  if (host === "localhost" || host.endsWith(".local"))
    return "/api"; // Docker local: nginx proxy
  return "https://api-gateway-production-e01a.up.railway.app/api"; // Vercel -> Railway
})();
```

### **11.2 Backend en Railway (12/12 servicios)**

| **Servicio** | **Tipo** | **Estado** |
|:-------------|:---------|:----------:|
| auth-db, presupuesto-db, expediente-db, notificacion-db | PostgreSQL 16-alpine | OK |
| rabbitmq | RabbitMQ 3-management-alpine | OK |
| eureka-server | Netflix Eureka (:8761) | OK |
| api-gateway | Spring Cloud Gateway (:8080) | OK |
| auth-service, presupuesto-service, expediente-service, notificacion-service | Spring Boot 3.4 | OK |

### **11.3 Lecciones Aprendidas (Railway)**

1. **EUREKA_INSTANCE_HOSTNAME es obligatorio**: sin esto los servicios registran su container ID (ej: `1cd254d6997e`), que no es resoluble por otros servicios → `UnknownHostException`
2. **EUREKA_CLIENT_SERVICEURL_DEFAULTZONE** usa el private domain real de Railway (`sisexp-upla-springboot.railway.internal`), no el nombre del servicio
3. **CORS**: `allowCredentials=false` + `allowedOriginPatterns=*` (incompatible con `*` origins con credentials)
4. **Railway CLI v4.30.5**: interactiva, usar flags `--service`, `--variables`, `-y`. Espaciar consultas para evitar rate limit Cloudflare 1015
5. **No ngrok**: free tier muestra pagina interstitial que bloquea CORS preflight. Se migro a Railway con dominio publico

---

## **12. Seed Data**

| **Servicio** | **Cantidad** | **Detalle** |
|:-------------|:-------------|:------------|
| auth-service | 6 usuarios | 1 por cada rol: Admin, Coordinacion, Secretaria, Director, Laboratorio, Decanato |
| presupuesto-service | 5 techos, 20 POI, 80 PAP, 4 notas | Techos 2022-2026. S/1.5M en 2026 (50% usado). Notas modificatorias en estados pendiente/configurada/rechazada |
| expediente-service | 8 expedientes + 6 logs | 7 estados distintos (Borrador a Finalizado), urgencias variadas |

### Credenciales de prueba

| **Rol** | **Email** | **Password** |
|:--------|:----------|:-------------|
| Administrador | jefe@upla.edu.pe | jefe123 |
| Coordinacion | coord@upla.edu.pe | coord123 |
| Secretaria | secretaria@upla.edu.pe | secretaria123 |
| Director | director@upla.edu.pe | director123 |
| Laboratorio | lab@upla.edu.pe | lab123 |
| Decanato | decanato@upla.edu.pe | decanato123 |

---

## **13. Data Types — Post-Auditoria**

| **Entidad** | **Campo** | **Tipo** | **Justificacion** |
|:------------|:----------|:---------|:------------------|
| NotaModificatoria | tipo | TipoNota enum | inclusion_item / inclusion_actividad |
| NotaModificatoria | estado | EstadoNota enum | pendiente / configurada / rechazada |
| NotaModificatoria | nuevoTipo | Naturaleza enum | Bien / Servicio |
| NotaModificatoria | justificacion | TEXT | Sin limite arbitrario (era VARCHAR(2000)) |
| DocumentoAdjunto | tamano | Long | Archivos >2GB (era Integer, max 2.1GB) |
| Usuario | email | length=254 | RFC 5321 |
| Usuario | nombre | length=150 | Acotado |

Todos los montos: BigDecimal precision=12, scale=2. Enums: @Enumerated(EnumType.STRING). FK entre servicios: Long (sin @ManyToOne).

---

## **14. API Reference (44 endpoints)**

### Auth & Usuarios
| **Metodo** | **Ruta** | **Servicio** | **Auth** |
|:-----------|:---------|:-------------|:--------:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | JWT |
| GET | /api/status | auth-service | No |
| GET | /api/health | auth-service | No |

### Presupuesto
| **Metodo** | **Ruta** | **Servicio** | **Auth** |
|:-----------|:---------|:-------------|:--------:|
| GET | /api/techos-presupuestales | presupuesto-service | JWT |
| GET | /api/actividades-poi/techo/{id} | presupuesto-service | JWT |
| GET | /api/necesidades-pap/actividad/{id} | presupuesto-service | JWT |
| GET/POST | /api/notas-modificatorias | presupuesto-service | JWT |
| PUT | /api/notas-modificatorias/{id}/configurar | presupuesto-service | JWT |
| PUT | /api/notas-modificatorias/{id}/rechazar | presupuesto-service | JWT |
| GET | /api/dashboard/alertas?anio= | presupuesto-service | JWT |
| GET | /api/dashboard/saldos?anio= | presupuesto-service | JWT |
| GET | /api/reportes/anual/{anio} | presupuesto-service | JWT |
| GET | /api/reportes/expedientes?anio= | presupuesto-service | JWT |
| GET | /api/reportes/poi?anio= | presupuesto-service | JWT |
| GET | /api/reportes/pap?anio= | presupuesto-service | JWT |
| GET | /api/reportes/poi/{id} | presupuesto-service | JWT |
| GET | /api/reportes/pap/{id} | presupuesto-service | JWT |

### Expedientes
| **Metodo** | **Ruta** | **Servicio** | **Auth** |
|:-----------|:---------|:-------------|:--------:|
| GET/POST | /api/expedientes | expediente-service | JWT |
| GET | /api/expedientes/disponibilidad/{poiId}/{papId}?cantidad= | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/expedientes/rastreo/{codigo} | expediente-service | No |

### Notificaciones
| **Metodo** | **Ruta** | **Servicio** | **Auth** |
|:-----------|:---------|:-------------|:--------:|
| GET | /api/notificaciones?usuarioId= | notificacion-service | JWT |
| GET | /api/notificaciones/count?usuarioId= | notificacion-service | JWT |

### Monitor (publico, sin JWT)
| **Metodo** | **Ruta** | **Servicio** | **Auth** |
|:-----------|:---------|:-------------|:--------:|
| GET | /api/monitor/activity?since=5 | api-gateway | No |
| GET | /api/monitor/activity/service?name=X&since=5 | api-gateway | No |

---

## **15. Verificacion**

### Smoke test automatizado (21 endpoints)

```bash
docker compose up -d
docker compose -f docker-compose.yml -f docker-compose.test.yml run --rm tester
```

### Health Checks manuales

```bash
# Salud de todos los servicios (via Gateway)
curl http://localhost:8080/api/status

# Salud individual de cada microservicio
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health

# Eureka Dashboard
open http://localhost:8761

# RabbitMQ Management
open http://localhost:15672
# Credenciales: sisexp / sisexp

# Monitoreo completo
open http://localhost
```

### Accesos rapidos

| **URL** | **Descripcion** |
|:--------|:----------------|
| `http://localhost` | SISEXP-UPLA React SPA (login, dashboard, CRUD, monitor) |
| `http://localhost/api/monitor/activity?since=5` | API: actividad en tiempo real ultimos N minutos |
| `http://localhost/api/status` | API: estado de los 7 nodos |
| `http://localhost:8761` | Eureka Dashboard |
| `http://localhost:15672` | RabbitMQ Management (sisexp/sisexp) |

---

> **Documento generado: 14 de julio de 2026 — SISEXP-UPLA vFinal**
