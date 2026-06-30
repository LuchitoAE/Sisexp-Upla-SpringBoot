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
| Dominio | Gestion presupuestal: Techo → POI → PAP → Expediente |
| Entidades | 11 (Usuario, TechoPresupuestal, ActividadPOI, NecesidadPAP, Expediente, DocumentoAdjunto, SeguimientoLog, Notificacion, etc.) |
| Roles | 6 (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato) |
| Estados | 7 (Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) |

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

### **3.1 Diagrama de Despliegue**

```
┌──────────────────────────────────────────────────────────────────┐
│                     Docker Compose (12 contenedores)               │
│                                                                    │
│  ┌──────────┐     ┌──────────────────┐     ┌───────────────────┐  │
│  │  NGINX   │────→│   API Gateway    │←────│  Eureka Server    │  │
│  │  :80     │     │   :8080          │     │  :8761            │  │
│  │ (React)  │     │ (Spring Cloud)   │     │ (Service Discov.) │  │
│  └──────────┘     └───────┬──────────┘     └───────────────────┘  │
│                           │                                        │
│        ┌──────────────────┼───────────────────┐                    │
│        ↓                  ↓                   ↓                    │
│  ┌───────────┐   ┌───────────────┐   ┌───────────────┐            │
│  │ AUTH-SVC  │   │PRESUPUESTO-SVC│   │ EXPEDIENTE-SVC│            │
│  │ :8081     │   │ :8082         │   │ :8083         │            │
│  └─────┬─────┘   └──────┬────────┘   └───┬───────┬───┘            │
│        ↓                ↓                ↓       │                │
│  ┌───────────┐   ┌───────────┐   ┌──────────┐   │                │
│  │  PG Auth  │   │ PG Presup │   │ PG Exped │   │                │
│  │  :5433    │   │ :5434     │   │ :5435    │   │                │
│  └───────────┘   └───────────┘   └──────────┘   │                │
│                                                  ↓                │
│  ┌───────────┐                       ┌────────────────┐          │
│  │ NOTIF-SVC │←──────────────────────│   RabbitMQ     │          │
│  │ :8084     │   consume eventos      │   :5672:15672  │          │
│  └─────┬─────┘                       └────────────────┘          │
│        ↓                                                          │
│  ┌───────────┐                                                     │
│  │ PG Notif  │                                                     │
│  │ :5436     │                                                     │
│  └───────────┘                                                     │
└──────────────────────────────────────────────────────────────────┘
```

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
- **Indicadores**: verde = UP, amarillo = STARTING, rojo = DOWN
- **Click en nodo**: muestra componentes (db, rabbit, disk, ping, ssl)
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

## **11. Acceso Publico — ngrok**

Para exponer el sistema a internet durante la presentacion:

```bash
# Instalar ngrok (una vez)
winget install --id ngrok.ngrok

# Configurar authtoken (una vez)
ngrok config add-authtoken TU_TOKEN

# Exponer localhost:80
ngrok http 80
```

| **Propiedad** | **Valor** |
|:--------------|:----------|
| URL actual | `https://7fbf-179-6-45-108.ngrok-free.app` |
| Cambio por reinicio | La URL cambia cada vez que se reinicia ngrok |
| URL fija | Requiere plan pago |

> **NOTA**: Los visitantes que ingresan por ngrok Free veran una pagina de advertencia — deben hacer click en "Visit Site" para continuar.

---

## **12. API Reference**

| **Metodo** | **Ruta** | **Servicio** | **Auth** |
|:-----------|:---------|:-------------|:--------:|
| POST | /api/auth/login | auth-service | No |
| GET | /api/auth/me | auth-service | JWT |
| GET | /api/usuarios | auth-service | Admin |
| GET | /api/techos-presupuestales | presupuesto-service | JWT |
| POST | /api/techos-presupuestales | presupuesto-service | JWT |
| GET | /api/actividades-poi | presupuesto-service | JWT |
| POST | /api/necesidades-pap | presupuesto-service | JWT |
| GET | /api/expedientes | expediente-service | JWT |
| POST | /api/expedientes | expediente-service | JWT |
| PUT | /api/expedientes/{id}/estado | expediente-service | JWT |
| GET | /api/notificaciones | notificacion-service | JWT |
| GET | /api/status | auth-service | No |

---

## **13. Verificacion**

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

# Dashboard Monitoreo
open http://localhost
```

---

> **Documento generado: 30 de junio de 2026 — SISEXP-UPLA vFinal**
