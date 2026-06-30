---
name: arquitectura-microservicios
description: Use when designing or evaluating microservices architecture, applying Clean Architecture, SOLID principles, design patterns (GoF), and architectural patterns (week 9 patterns). Covers the SISEXP microservices migration from monolith to distributed system with 11 Docker containers.
---

# Skill: Arquitectura de Microservicios — SISEXP-UPLA

---

## 1. CLEAN ARCHITECTURE (Robert C. Martin)

Cada microservicio SISEXP aplica 4 capas concéntricas, dependencias hacia adentro:

```
Framework & Drivers  ← Spring Boot, PostgreSQL, RabbitMQ, JWT
    ↑ depends on
Interface Adapters   ← Controllers, DTOs, Repositories (Spring Data JPA)
    ↑ depends on
Application Layer    ← Service (@Service, @Transactional), casos de uso
    ↑ depends on
Domain Entities      ← JPA entities (@Entity), enums, value objects
```

### Ejemplo: expediente-service

| Capa | Archivo | Responsabilidad |
|---|---|---|
| Domain | `Expediente.java`, `SeguimientoLog.java`, `DocumentoAdjunto.java`, `EstadoExpediente.java` | Reglas de negocio puras: estados válidos, transiciones permitidas |
| Application | `ExpedienteService.java`, `BusinessRulesService.java` | Casos de uso: crearExpediente(), cambiarEstado(), validarSaldo() |
| Interface Adapters | `ExpedienteController.java`, `ExpedienteRepository.java`, `CambiarEstadoDTO.java` | Traduce HTTP ↔ dominio; persiste vía JPA abstractions |
| Frameworks | `application.properties`, `SecurityConfig.java`, `RabbitMQConfig.java` | Spring Boot wiring, conexion DB, colas RabbitMQ |

### Regla de dependencia

- Services **nunca** importan `HttpServletRequest`, `HttpServletResponse`
- Entities **nunca** dependen de Spring annotations de controller o config
- Repositories son **interfaces** (Spring Data), no implementaciones concretas
- DTOs son planos (records o POJOs), sin lógica de negocio

---

## 2. PRINCIPIOS SOLID (aplicados a SISEXP)

### S — Single Responsibility Principle

Cada microservicio = **un bounded context**:

| Servicio | Responsabilidad ÚNICA | Puerto |
|---|---|---|
| auth-service | Autenticación, autorización, gestión usuarios/roles | :8081 |
| presupuesto-service | Techos, POI, PAP, disponibilidad presupuestal | :8082 |
| expediente-service | CRUD expedientes, documentos, seguimiento, estados | :8083 |
| notificacion-service | Crear/leer notificaciones, escuchar eventos de cambio de estado | :8084 |
| api-gateway | Ruteo, filtro JWT global, CORS, rate limiting | :8080 |

### O — Open/Closed Principle

**Abierto para extensión**: RabbitMQ permite agregar nuevos consumidores sin modificar productores.
- expediente-service publica `ExpedienteEstadoCambiadoEvent` → cualquier servicio puede suscribirse después
- notificacion-service consume este evento → audit-service futuro podría suscribirse también

**Cerrado para modificación**: Cada servicio se despliega independientemente.
- Cambiar `presupuesto-service` no requiere tocar `expediente-service`

### L — Liskov Substitution Principle

- Interfaces de repositorio extienden `JpaRepository<T, ID>` → Spring Data proxies son sustituibles
- `UserDetailsService.loadUserByUsername()` → `CustomUserDetailsService` sustituye la interfaz de Spring Security
- DTOs heredados mantienen contrato: `ExpedienteDTO extends BaseDTO implements Serializable`

### I — Interface Segregation Principle

Cada servicio expone **solo los endpoints que su bounded context requiere**:

| Servicio | Interfaces expuestas (endpoints REST) |
|---|---|
| auth-service | `/api/auth/login`, `/api/auth/me`, `/api/usuarios/**` |
| presupuesto-service | `/api/techos/**`, `/api/poi/**`, `/api/pap/**`, `/api/presupuesto/disponibilidad/**` |
| expediente-service | `/api/expedientes/**`, `/api/expedientes/{id}/documentos`, `/api/expedientes/{id}/estado` |
| notificacion-service | `/api/notificaciones/**` |

Ningún servicio expone endpoints de otros bounded contexts.

### D — Dependency Inversion Principle

- Services dependen de **interfaces** de repository (no implementaciones): `private final ExpedienteRepository repo;`
- Comunicación inter-servicio: REST vía `RestTemplate`/`WebClient` → depende de interfaz HTTP, no de otro servicio concreto
- RabbitMQ: productores y consumidores dependen de `RabbitTemplate` / `@RabbitListener` (abstracciones de Spring AMQP)
- Gateway rutea por `service-id` (Eureka), no por IP/host concretos

---

## 3. PATRONES ARQUITECTÓNICOS (Semana 9 — 16 patrones)

### 3.1 Patrones estructurales

| # | Patrón | Implementación en SISEXP |
|---|---|---|
| 1 | **Layers (Capas)** | Controller → Service → Repository → Domain en cada microservicio |
| 2 | **Client-Server** | React SPA (client) → Spring Boot REST API (server) via HTTP/JSON |
| 3 | **Pipe-Filter** | Spring Cloud Gateway filter chain: JWT validation → Rate Limiter → Routing |
| 4 | **MVC** | Spring MVC: @RestController → Model (@Entity) → Thymeleaf View (monolith) |
| 5 | **SOA (Service-Oriented)** | Microservicios REST que exponen contratos bien definidos (DTOs, OpenAPI) |

### 3.2 Patrones de comunicación

| # | Patrón | Implementación en SISEXP |
|---|---|---|
| 6 | **API Gateway** | Spring Cloud Gateway (:8080) — rutea /api/auth/**, /api/expedientes/**, etc. |
| 7 | **Service Discovery** | Eureka Server (:8761) — auth, presupuesto, expediente, notificacion se registran |
| 8 | **Message Broker** | RabbitMQ como bus central — eventos de dominio via exchanges/topics |
| 9 | **Publish-Subscribe** | Fanout/Direct exchanges en RabbitMQ — expediente publica, notificacion+audit consumen |
| 10 | **Event-Driven** | Cambio de estado de expediente dispara evento → notificacion-service reacciona asincrónicamente |

### 3.3 Patrones de datos

| # | Patrón | Implementación en SISEXP |
|---|---|---|
| 11 | **Database per Service** | Cada servicio tiene su propia PostgreSQL (o schema): auth-db, presupuesto-db, expediente-db, notificacion-db |
| 12 | **Saga (Coreografía)** | Eventos RabbitMQ coordinan consistencia eventual: expediente aprobado → evento → presupuesto descuenta saldo → notificación creada |
| 13 | **CQRS (básico)** | Lectura: GET /api/expedientes (cacheable, sin joins pesados). Escritura: POST /api/expedientes (transaccional, con validaciones de negocio) |

### 3.4 Patrones de resiliencia

| # | Patrón | Implementación en SISEXP |
|---|---|---|
| 14 | **Circuit Breaker** | Resilience4j en llamadas REST inter-servicio. Si presupuesto-service falla 5 veces en 10 seg, circuito abre 30 seg. |
| 15 | **Retry** | Spring Retry en RestTemplate: 3 reintentos con backoff exponencial (1s, 2s, 4s) |
| 16 | **Health Check** | Spring Actuator `/actuator/health` en cada servicio. Gateway agrega `/status` con el estado compuesto de todos los servicios. |

---

## 4. PATRONES DE DISEÑO GOF (Catálogo clásico aplicado)

### Creacionales

| Patrón | Dónde | Por qué |
|---|---|---|
| **Singleton** | Beans Spring (@Service, @Repository, @Component) | Spring container gestiona una instancia por bean (scope singleton por defecto) |
| **Factory Method** | `RabbitConnectionFactory`, `DataSourceBuilder` | Creación configurable de conexiones según perfil (dev=H2, prod=PostgreSQL) |
| **Builder** | `ExpedienteDTO.builder()`, `JwtTokenProvider.buildToken()` | Construcción paso a paso de objetos complejos (Lombok @Builder) |
| **Prototype** | `RestTemplate` (scope prototype para evitar shared state) | Cada llamada HTTP usa una instancia limpia |

### Estructurales

| Patrón | Dónde | Por qué |
|---|---|---|
| **Adapter** | `JwtAuthenticationFilter` adapta JWT al `SecurityContextHolder` de Spring | Convierte interfaz JWT (token string) a la interfaz que Spring Security espera (Authentication) |
| **Decorator** | Resilience4j `@CircuitBreaker` decora llamadas a servicios externos | Agrega comportamiento (circuit breaker) sin modificar el código de la llamada |
| **Facade** | `ExpedienteService` como fachada ante `ExpedienteRepository`, `DocumentoRepository`, `SeguimientoLogRepository` | Simplifica la interfaz: el controller solo llama al service |
| **Proxy** | Spring AOP: `@Transactional`, `@Cacheable` | Controla acceso al objeto real (transacción, caché) sin modificar su código |
| **Composite** | `SecurityFilterChain` con múltiples filtros encadenados | Trata filtros individuales y composiciones de manera uniforme |

### Comportamiento

| Patrón | Dónde | Por qué |
|---|---|---|
| **Observer** | `@RabbitListener` en notificacion-service | Reacciona a eventos publicados por expediente-service sin acoplamiento directo |
| **Template Method** | `JdbcTemplate`, `RestTemplate`, `RabbitTemplate` | Define el esqueleto del algoritmo, delega pasos específicos a callbacks |
| **Strategy** | `BusinessRulesService.ejecutarSaldo()` — diferentes estrategias según `Naturaleza` (BIEN/SERVICIO) | Encapsula algoritmos de cálculo intercambiables |
| **Chain of Responsibility** | Filtros en SecurityConfig: `JwtFilter → RateLimitFilter → RoutingFilter` | Cada filtro procesa o pasa al siguiente |
| **Command** | `CambiarEstadoDTO` + `ExpedienteService.cambiarEstado()` | Encapsula una solicitud como objeto (comando), permitiendo logging/undo/queue |

---

## 5. ARQUITECTURA DE DESPLIEGUE

```
┌─────────────────────────────────────────────────────────────────┐
│                    Docker Compose (local)                        │
│                                                                 │
│  ┌──────────┐     ┌─────────────────┐     ┌────────────────┐   │
│  │  NGINX   │────→│  API Gateway    │────→│  Eureka Server │   │
│  │  :80     │     │  :8080          │     │  :8761         │   │
│  │ (React)  │     │  (Spring Cloud) │     │  (Discovery)   │   │
│  └──────────┘     └───────┬─────────┘     └────────────────┘   │
│                           │                                      │
│              ┌────────────┼────────────┬────────────┐           │
│              ↓            ↓            ↓            ↓           │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  │ auth-service │ │presupuesto-svc│ │expediente-svc│ │notificacion-sv│
│  │ :8081        │ │ :8082        │ │ :8083        │ │ :8084        │
│  └──────┬───────┘ └──────┬───────┘ └──┬──────┬────┘ └──────┬───────┘
│         ↓                ↓            ↓      │             ↓
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  │ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │ │ PostgreSQL   │
│  │ auth-db:5432 │ │ pres-db:5433 │ │ exp-db:5434  │ │ noti-db:5435 │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘
│                                                     │             │
│                                              ┌──────┴─────┐       │
│                                              │  RabbitMQ  │       │
│                                              │  :5672     │       │
│                                              │  :15672    │       │
│                                              └────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 6. FLUJO DE EVENTOS (SAGA — Coreografía)

```
1. POST /api/expedientes/{id}/estado  →  expediente-service
   │
2. ExpedienteService.cambiarEstado()
   ├── Validar transición permitida (Borrador→En_revision→Aprobado/Rechazado/...)
   ├── Guardar nuevo estado en DB expediente
   ├── Registrar SeguimientoLog
   └── Publicar evento RabbitMQ: "expediente.estado.cambiado"
        │
3. RabbitMQ entrega a:
   ├── notificacion-service  →  crea Notificacion
   ├── presupuesto-service   →  si estado=APROBADO, descuenta saldo
   └── (futuro) audit-service → registra para compliance
```

---

## 7. CONFIGURACIÓN POR MICROSERVICIO

### Health Check (Spring Actuator)

```yaml
# application.properties de cada servicio
management.endpoints.web.exposure.include=health,info
management.endpoint.health.show-details=always
management.health.rabbit.enabled=true
management.health.db.enabled=true
```

### Circuit Breaker (Resilience4j) — cuando un servicio llama a otro

```java
@Service
public class PresupuestoService {

    @CircuitBreaker(name = "presupuesto", fallbackMethod = "fallbackDisponibilidad")
    public DisponibilidadDTO getDisponibilidad(Long id) {
        return restTemplate.getForObject(
            "http://presupuesto-service/api/presupuesto/disponibilidad/" + id,
            DisponibilidadDTO.class
        );
    }

    public DisponibilidadDTO fallbackDisponibilidad(Long id, Exception e) {
        return new DisponibilidadDTO(id, null, "Servicio no disponible", 0, 0);
    }
}
```

---

## 8. ENDPOINT DE MONITOREO (API Gateway)

```java
@RestController
public class StatusController {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<String> services = List.of("AUTH-SERVICE", "PRESUPUESTO-SERVICE",
            "EXPEDIENTE-SERVICE", "NOTIFICACION-SERVICE");

        for (String svc : services) {
            try {
                var instances = discoveryClient.getInstances(svc);
                if (instances.isEmpty()) {
                    result.put(svc, Map.of("status", "DOWN", "instances", 0));
                    continue;
                }
                var instance = instances.get(0);
                String url = instance.getUri() + "/actuator/health";
                var health = restTemplate.getForObject(url, Map.class);
                result.put(svc, Map.of(
                    "status", health != null ? health.get("status") : "UNKNOWN",
                    "instances", instances.size(),
                    "host", instance.getHost() + ":" + instance.getPort()
                ));
            } catch (Exception e) {
                result.put(svc, Map.of("status", "DOWN", "error", e.getMessage()));
            }
        }
        result.put("rabbitmq", checkRabbitMQ());
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
```

---

## 9. VERIFICACIÓN

```bash
# Salud de todos los servicios
curl http://localhost:8080/api/status

# Salud individual
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
curl http://localhost:8084/actuator/health

# Eureka Dashboard
open http://localhost:8761

# RabbitMQ Management
open http://localhost:15672
```
