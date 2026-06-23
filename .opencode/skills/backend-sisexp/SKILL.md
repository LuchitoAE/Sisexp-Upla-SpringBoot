---
name: backend-sisexp
description: Use when working on Spring Boot backend, Java entities, JPA repositories, services, API controllers, Spring Security, JWT authentication, CORS configuration, data initialization, business rules, enums, or DTOs for SISEXP-UPLA.
---

# Skill: Backend SISEXP-UPLA — Spring Boot + JPA + Security

---

## 1. PACKAGE STRUCTURE

```
com.upla.sisexp/
├── config/
│   ├── SecurityConfig.java         # Spring Security: form login, remember-me, CSRF bypass /api/**, CORS, JWT filter
│   ├── WebConfig.java              # SPA fallback: /** → index.html (excl /api/**)
│   ├── MethodSecurityConfig.java   # @EnableMethodSecurity
│   ├── DataInitializer.java        # Seed data: TRUNCATE CASCADE + roles + users + techos + POI
│   └── DbIndexInitializer.java     # Database index verification
├── security/
│   ├── CustomUserDetails.java      # UserDetails impl wrapping Usuario entity
│   ├── CustomUserDetailsService.java # loads user by email
│   ├── HorarioLaboralFilter.java   # 8am-8pm Peru filter, exempt routes
│   ├── JwtTokenProvider.java       # JJWT token generation/validation (30d)
│   └── JwtAuthenticationFilter.java # Extracts Bearer token, sets SecurityContext
├── model/                          # 11 JPA entities
├── enums/                          # 9 enums
├── dto/                            # ExpedienteFormDTO, CambiarEstadoDTO, DocumentoDTO
├── repository/                     # 10 Spring Data JPA repositories
├── service/                        # 11 services (@Transactional)
├── controller/                     # 10 Thymeleaf MVC controllers
├── api/                            # 11 REST API controllers (@RestController)
└── exception/
    ├── BusinessException.java
    └── GlobalExceptionHandler.java # @ControllerAdvice for Thymeleaf
```

---

## 2. SECURITY CONFIG

### SecurityConfig.java Key Points

```java
// CORS for external clients (Vercel, etc.)
config.setAllowedOrigins(List.of("https://sisexp-upla.vercel.app", "http://localhost:3000"));
config.setAllowCredentials(true);  // Required for cookie-based auth

// CSRF disabled for API routes
.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/rastreo/**"))

// Public routes
"/login","/","/index.html","/rastreo/**","/api/health","/health",
"/api/auth/login","/static/**","/error","/horario-cerrado"

// API auth: JWT filter + form login both supported
/api/** → authenticated (JWT Bearer token OR session cookie)
```

### Filter Chain Order
1. `HorarioLaboralFilter` — checks 8am-8pm, exempts `/api`, `/login`, `/rastreo`, static
2. `JwtAuthenticationFilter` — reads `Authorization: Bearer` header, sets auth if valid
3. `UsernamePasswordAuthenticationFilter` — form login for Thymeleaf

### JWT Tokens
- **Secret**: `jwt.secret` in `application.properties` (env var `JWT_SECRET`)
- **Validity**: `jwt.validity-ms=2592000000` (30 days, same as remember-me)
- **Login response**: `{ token: "eyJ...", usuario: { id, nombre, email, rol, horarioRestringido } }`
- **Auth header**: `Authorization: Bearer <token>`
- **Filter**: skips if no Bearer header, falls through to session auth

---

## 3. ENTITIES (11 JPA entities)

| Entity | Table | Key Fields |
|--------|-------|------------|
| Usuario | usuarios | nombre, email, password (BCrypt), rol (enum), activo, horarioRestringido, createdAt |
| Rol | roles | nombre (enum RolUsuario) |
| TechoPresupuestal | techos_presupuestales | año (unique), montoTotal, activo, poiCerrado, creadoPor |
| ActividadPOI | actividades_poi | codigo (unique), nombre, presupuestoAsignado, fechaLimite, estado (enum), papCerrado, techo (FK) |
| NecesidadPAP | necesidades_pap | nombre, cantidad, precioUnitario, unidad, oficinaLab, tipo, clasificadorGasto, actividad (FK) |
| Expediente | expedientes | codigo (unique), descripcion, urgencia (enum), naturaleza (enum), cantidadSolicitada, costoEstimado, estado (enum), actividad (FK), necesidad (FK), solicitante (FK) |
| DocumentoAdjunto | documentos_adjuntos | nombre, tipo (enum), rutaArchivo, tamaño, expediente (FK), subidoPor (FK) |
| SeguimientoLog | seguimiento_logs | estadoAnterior, estadoNuevo (String, not enum), observacion, expediente (FK), usuario (FK) |
| NotaModificatoria | notas_modificatorias | tipo (enum), nombre, justificacion, costoEstimadoReferencial, estado (enum), actividadOrigen/actividadDestino (FK), solicitante (FK) |
| Notificacion | notificaciones | tipo (enum), mensaje, leida, usuario (FK), expediente/nota (FK nullable) |

### Key Annotations
- `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` on all entities (prevents lazy proxy serialization errors)
- `@JsonProperty(access = WRITE_ONLY)` on `Usuario.password` (prevents password exposure in JSON)
- `@Column(columnDefinition = "TEXT")` for String LOBs (PostgreSQL compatibility, avoids OID issues)
- `@Enumerated(EnumType.STRING)` on all enum fields

---

## 4. ENUMS

| Enum | Values |
|------|--------|
| RolUsuario | Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato |
| EstadoExpediente | Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado |
| Urgencia | Urgente, No_tan_urgente, Puede_esperar |
| Naturaleza | Bien, Servicio |
| EstadoActividad | Pendiente, En_proceso, Finalizada, Extemporanea |
| TipoDocumento | TDR, Especificaciones_Tecnicas, Cotizacion, Informe_Tecnico |
| TipoNotificacion | observacion, rechazo, aprobacion, alerta_fecha, nota_aprobada, nota_rechazada, info |
| TipoNota | inclusion_item, inclusion_actividad |
| EstadoNota | Pendiente, Configurada, Rechazada |

---

## 5. API ENDPOINTS (~37 routes)

### Auth
- `POST /api/auth/login` — login, returns `{ token, usuario }` (JWT)
- `GET /api/auth/me` — current user from session/JWT
- `POST /api/auth/logout` — invalidate session

### CRUD patterns (all under `/api/`):
- Techos: `GET/POST /api/techos-presupuestales`, `PUT/PATCH /api/techos-presupuestales/{id}`
- POI: `GET /api/actividades-poi/techo/{techoId}`, `POST/PUT/DELETE`
- PAP: `GET/POST /api/necesidades-pap/actividad/{actividadId}`, `PUT/DELETE`
- Expedientes: `GET/POST`, `PUT/{id}/estado`, `POST/{id}/documentos`
- Usuarios: `GET/POST`, `PUT/{id}`, `PATCH/{id}/toggle-activo`
- Dashboard: `GET /api/dashboard/alertas`, `GET /api/dashboard/saldos`
- Reportes: `GET /api/reportes/anual/{anio}`, `/poi`, `/pap`, `/expedientes`
- Notificaciones: `GET /api/notificaciones`, `/count`, `PUT /read-all`, `/{id}/read`
- Notas: `GET/POST /api/notas-modificatorias`, `PUT/{id}/configurar`, `/{id}/rechazar`
- Health: `GET /api/health`, `GET /health`

---

## 6. DATA INITIALIZER

`DataInitializer.java` (Order=2, runs after DbIndexInitializer):

1. **TRUNCATE CASCADE** via `JdbcTemplate.execute()` in reverse FK order:
   notificaciones → seguimiento_logs → documentos_adjuntos → expedientes → necesidades_pap → actividades_poi → techos_presupuestales → usuarios → roles
2. **Seed data**:
   - 6 roles (all RolUsuario values)
   - 6 users (one per role, BCrypt encoded)
   - 2 techos: 2025 (completado 45k/45k), 2026 (abierto 115k)
   - 9 POI: 4 historicos (2025), 5 vigentes (2026)
   - NO necesidades, NO expedientes, NO logs, NO notificaciones (sistema en blanco)

---

## 7. COMMON ERRORS & FIXES

| Error | Cause | Fix |
|-------|-------|-----|
| "cannot execute INSERT in a read-only transaction" | `@Transactional(readOnly = true)` on controller class | Remove from class, put only on `@GetMapping` methods |
| Hibernate lazy proxy serialization error | Jackson tries to serialize Hibernate proxy | Add `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` on entity |
| Password appears in JSON responses | `password` field not protected | Add `@JsonProperty(access = WRITE_ONLY)` on password field |
| Ambiguous mapping error | Two controllers map same path | Check for duplicate `@GetMapping` across API and Thymeleaf controllers |
| 403 on `/api/auth/login` POST | CSRF protection on API routes | Ensure `csrf.ignoringRequestMatchers("/api/**")` |
| User redirected to /login inside loop | `HorarioLaboralFilter` blocks static resources | Add `/vendor`, `/css`, `/js` to `RUTAS_EXENTAS` |
| PostgreSQL "OID" error for TEXT columns | `@Lob String` maps to OID in PostgreSQL | Use `@Column(columnDefinition = "TEXT")` |
| Railway health check fails | `/health` endpoint not registered or blocked | Ensure in `permitAll()` AND `RUTAS_EXENTAS` |
| JWT token rejected | Secret mismatch or expired | Check `jwt.secret` and `jwt.validity-ms` properties |
