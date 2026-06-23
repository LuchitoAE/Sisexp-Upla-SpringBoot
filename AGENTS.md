# AGENTS.md — SISEXP-UPLA (Spring Boot)

## PROYECTO ACTUAL: SISEXP-UPLA

**Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes**

Migración de Express/React a Spring Boot + HTML/JS vanilla.

| Dato | Valor |
|---|---|
| Dominio | Gestión presupuestal de expedientes (Techo → POI → PAP → Expedientes) |
| Entidades | 11 (Usuario, TechoPresupuestal, ActividadPOI, NecesidadPAP, Expediente, DocumentoAdjunto, SeguimientoLog, NotaModificatoria, Notificacion, Rol) |
| Roles | 6 (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato) |
| Estados expediente | 7 (Borrador, En revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) |
| Frontend | **Thymeleaf** (MVC server-side, templates + fragments) + Bootstrap 5.3 |
| Auth | Spring Security form login + Remember Me 30 días (equivalente al JWT 30d) |
| Horario laboral | 8am-8pm Perú, bypass para Admin |
| Documentación | `docs/referencia/DOMINIO_SISEXP.md` — dominio completo extraído del código original |
| Docs originales | `docs/referencia/doc/` — ERS, SDD, trazabilidad, inconsistencias |
| Plan detallado | `PLAN.md` — fases, tareas y orden de implementación |

**Stack objetivo:** Spring Boot 3.4.1 + Java 17 + Spring Security + Thymeleaf + Spring Data JPA + PostgreSQL + Bootstrap 5.3

---

## ¿Qué es este archivo?
Es la memoria del proyecto. Se actualiza en cada fase completada. Al iniciar una nueva sesión, se lee primero. Contiene plantillas genéricas de Spring Boot + frontend HTML/JS reutilizables entre proyectos.

---

## 1. METODOLOGÍA ICONIX

5 fases con trazabilidad completa de requisitos a código:

| Fase | Documento | Diagramas |
|------|----------|-----------|
| 1. ERS | ISO 29148, actores, RF, CU | 1 flowchart CU + 1 SSD por CU (14) |
| 2. Análisis | Modelo de Dominio conceptual | 1 classDiagram (17 clases) |
| 3. Robustez | Boundary-Control-Entity | 6 diagramas secuencia BCE |
| 4. Secuencias | Controller → Service → Repo → DB | 4 diagramas detallados |
| 5. Código | Implementación | Spring Boot + Java |

**Documentos**: `.md` fuente → `pandoc` → `.docx` entrega.

---

## 2. MAPEO MERMAID → STARUML

| Mermaid | StarUML | ¿Funciona? |
|---------|---------|:---:|
| `flowchart TD/LR` | FCFlowchartDiagram | ✅ |
| `sequenceDiagram` | UMLSequenceDiagram | ✅ |
| `classDiagram` | UMLClassDiagram | ✅ |
| `usecaseDiagram` | — | ❌ (usar flowchart) |
| `%%{init}...` | — | ❌ (rechazado) |
| `erDiagram` | — | no probado |

**Reglas**: No usar `usecaseDiagram` ni `%%{init}`. Usar `TD` vertical, `LR` horizontal. Después de generar: Auto Layout en StarUML. Los nombres genéricos se renombran manualmente.

---

## Progress

### Done (Session 2026-06-23)
- **`@Transactional(readOnly = true)` eliminado de 4 controllers**: `UsuarioController`, `ActividadPOIController`, `NotificacionController`, `RastreoController` — causaba "cannot execute INSERT in a read-only transaction" en PostgreSQL para cualquier POST/PUT. Se movió a solo los métodos `@GetMapping`.
- **Error 500 en crear Actividad POI**: fix con `required` en `<select name="techoId">` + `@RequestParam(required = false) Long techoId` + validación en service.
- **Error 500 en crear Usuario**: mismo fix del `@Transactional(readOnly = true)`.
- **Error 500 en detalle expediente** (sesión anterior): `log.estadoNuevo.name()` sobre `String` → cambiado a `log.estadoNuevo`.
- **Error 500 en crear Necesidad PAP** (sesión anterior): `@Transactional` del controller + default option en select.
- **Login page rediseñada**: login-card con sombra, logo, gradiente, alerts con `d-flex align-items-center`.
- **Badges de urgencia**: agregados `.badge-No_tan_urgente` (amarillo) y `.badge-Puede_esperar` (gris) en `sisexp.css`.
- **Chart.js eliminado**: canvas + script removido de reportes; `chart.umd.min.js` quitado de `scripts.html`.
- **HorarioLaboralFilter**: agregado `/vendor` a `RUTAS_EXENTAS` — Bootstrap no se cargaba fuera del horario laboral.
- **Seed data limpiado**: base de datos reseteada (TRUNCATE CASCADE via JdbcTemplate). Datos mínimos:
  - 6 roles, 6 usuarios seed
  - 2 techos: 2025 completado (45k/45k), 2026 abierto (115k/0 usado)
  - 9 POI: 4 históricos (2025), 5 vigentes (2026)
  - 0 necesidades, 0 expedientes, 0 logs, 0 notificaciones — sistema en blanco
- **Techo nuevo**: auto-sugiere `new Date().getFullYear() + 1`.
- **Rastreo**: quitado del sidebar (sigue público desde `/login`).
- **Bootstrap local fix**: vendor files ahora cargan correctamente gracias a `/vendor` en rutas exentas.

### Done (Session 2026-06-23, after deploy fix)
- **Docker build local exitoso**: `docker build -t sisexp:latest .` compila sin errores, imagen lista para deploy
- **Contenedor local corre**: `docker run -p 8080:8080 sisexp:latest` inicia Spring Boot + seed data sin errores
- **Fix ambigüedad mappings**: `NotificacionController` tenía `@GetMapping("/api/notificaciones/count")` que chocaba con `ApiNotificacionController`. Eliminado del controller Thymeleaf.
- **Fix Hibernate lazy proxy serialization**: agregado `@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})` en 9 entidades JPA + `spring.jackson.serialization.fail-on-empty-beans=false` en `application.properties`
- **Fix password expuesto en JSON**: `@JsonProperty(access = WRITE_ONLY)` en `Usuario.password` para que no aparezca en respuestas API (ni en listado usuarios ni en nested `creadoPor`)
- **Fix missing `@GetMapping("/{id}")`**: agregados endpoints `obtener()` en `ApiTechoPresupuestalController`, `ApiNecesidadPAPController`, `ApiUsuarioController`, `ApiNotaModificatoriaController`
- **Verificación endpoints funcionales**: login, /me, expedientes, techos, POI, dashboard, notificaciones, usuarios — todos responden 200/404 correctamente con H2 local
- **CSRF config**: `/api/**` exento de CSRF via `ignoringRequestMatchers("/api/**")` en SecurityConfig (se añadió en sesión anterior para React SPA)

### Done (Session 2026-06-23, documentation)
- **GitHub repo**: `https://github.com/LuchitoAE/Sisexp-Upla-SpringBoot` — código completo migrado
- **Railway deploy**: exitoso con PostgreSQL, seed data cargada, endpoints funcionales
- **3 skills de diagramación** creados en `.opencode/skills/`:
  - `diagramas-ssd` — System Sequence Diagrams (ICONIX Fase 4, ISO/IEC 19505)
  - `diagramas-ers` — ERS (ISO 29148, ICONIX Fase 1)
  - `diagramas-bce` — Robustez Boundary-Control-Entity (ICONIX Fase 3)
- **14 SSD** generados (CU01-CU14) en `docs/diagramas/SSD/`
- **ERS completo** en `docs/diagramas/ERS/` (14 RF, 8 RNF, 14 CU, ISO 29148)
- **14 BCE** generados (CU01-CU14) en `docs/diagramas/BCE/`
- **Documento consolidado**: `docs/SISEXP_DIAGRAMAS_COMPLETO.md` (163KB) + `.docx` (73KB)

## Critical Context

| Dato | Valor |
|---|---|
| URL producción | `https://sisexp-web-production.up.railway.app/login` |
| Credenciales seed | jefe@upla.edu.pe/jefe123 (Admin bypass 24/7), coord/coord123, secretaria/secretaria123, director/director123, lab/lab123, decanato/decanato123 |
| Seed data actual | 6 roles + 6 usuarios + 2 techos (2025 completo, 2026 abierto) + 9 POI. **Sin necesidades, expedientes, logs ni notificaciones** |
| Auth | Spring Security form login + remember-me 30d (no JWT) |
| Horario laboral | 8am-8pm `America/Lima`; rutas exentas: `/login`, `/rastreo`, `/api/health`, `/error`, `/css`, `/js`, `/vendor`, `/favicon.ico` |
| Package manager | **pnpm** (NO npm) — para instalar dependencias frontend y herramientas CLI |
| Package base | `com.upla.sisexp` |
| BD | PostgreSQL en Railway (prod), H2 en local (dev) |
| DB reset | `DataInitializer` limpia datos existentes con `TRUNCATE ... CASCADE` via `JdbcTemplate` y re-siembra en cada deploy (mientras haya datos previos) |

## Relevant Files
- `sisexp/src/main/resources/static/css/sisexp.css` — CSS con design system completo (badges, alerts, timeline, cards, responsive)
- `sisexp/src/main/resources/templates/login.html` — login rediseñado con card, gradiente, alerts con border-left
- `sisexp/src/main/resources/templates/expedientes/lista.html` — badges de urgencia con `.name().replace('_',' ')`
- `sisexp/src/main/resources/templates/expedientes/detalle.html` — fix de `log.estadoNuevo` (String, no enum)
- `sisexp/src/main/resources/templates/poi/lista.html` — `<select>` con `required` para techoId
- `sisexp/src/main/resources/templates/techos/lista.html` — auto-sugiere año actual+1 al crear
- `sisexp/src/main/resources/templates/fragments/sidebar.html` — Rastreo quitado del menú
- `sisexp/src/main/resources/templates/fragments/scripts.html` — Chart.js eliminado
- `sisexp/src/main/java/com/upla/sisexp/security/HorarioLaboralFilter.java` — `/vendor` agregado a RUTAS_EXENTAS
- `sisexp/src/main/java/com/upla/sisexp/config/DataInitializer.java` — seed limpio (9 POI, sin PAP/expedientes) + TRUNCATE CASCADE con JdbcTemplate
- `sisexp/src/main/java/com/upla/sisexp/controller/UsuarioController.java` — sin `@Transactional(readOnly = true)` a nivel clase
- `sisexp/src/main/java/com/upla/sisexp/controller/ActividadPOIController.java` — sin class-level `@Transactional`, `techoId` opcional
- `sisexp/src/main/java/com/upla/sisexp/controller/NotificacionController.java` — sin class-level `@Transactional`
- `sisexp/src/main/java/com/upla/sisexp/controller/RastreoController.java` — sin class-level `@Transactional`
- `sisexp/src/main/java/com/upla/sisexp/service/ActividadPOIService.java` — null check para `techoId`

## Lecciones Aprendidas (SISEXP-specific)

### Transacciones
1. **No usar `@Transactional(readOnly = true)` en controllers Thymeleaf**: los controllers con métodos POST causan "cannot execute INSERT in a read-only transaction" en PostgreSQL. Poner `@Transactional(readOnly = true)` solo en los `@GetMapping`, y dejar los `@PostMapping` sin anotación (el service maneja su propia transacción).
2. **Controladores afectados**: `UsuarioController`, `ActividadPOIController`, `NecesidadPAPController`, `NotificacionController`, `RastreoController` — todos tenían el mismo patrón incorrecto.

### Errores 500 comunes
3. **`.name()` sobre String**: En Thymeleaf templates, `${obj.name()}` solo funciona si `obj` es un enum. Si es String, lanza `SpelEvaluationException`. Usar `${obj}` directamente.
4. **`@RequestParam Long` con valor vacío**: Si un `<select>` sin `required` envía `""`, Spring no puede convertirlo a `Long`. Usar `@RequestParam(required = false) Long` + validación manual en service.
5. **`@Lob String` en PostgreSQL**: Mapea a OID (large objects) que falla en auto-commit. Usar `@Column(columnDefinition = "TEXT")`.

### Seed data
6. **Reset automático**: `DataInitializer` usa `JdbcTemplate.execute("TRUNCATE TABLE ... RESTART IDENTITY CASCADE")` para limpiar datos en cada deploy. Las tablas se truncan en orden inverso a las FKs.
7. **Seed mínimo**: Mantener solo roles + usuarios + techos + POI en seed. Necesidades y expedientes los crea el usuario desde la UI — menos data obsoleta.

### Frontend
8. **Rutas exentas en `HorarioLaboralFilter`**: Incluir `/vendor` (Bootstrap local), no solo `/css` y `/js`. Si falta, el login se ve sin estilos fuera del horario laboral.
9. **Badges con guiones bajos**: Los enums como `No_tan_urgente` se muestran con `replace('_',' ')` en templates. Las clases CSS deben coincidir exactamente con `enum.name()` (con guiones bajos).
10. **Chart.js innecesario**: Si solo se usa en reportes y el usuario no lo pide, es más simple quitarlo que mantenerlo. Pesar 45KB + script loading en todas las páginas.

### Seguridad
11. **Rastreo público**: Sin `sec:authorize` ni autenticación. Si se deja en sidebar, cualquier rol lo ve. Mejor quitarlo del sidebar y dejarlo solo accesible desde login (público).

---

## 3. ARQUITECTURA SPRING BOOT (SISEXP)

### Estructura de paquetes
```
com.upla.sisexp/
├── config/           # SecurityConfig, DataInitializer, DbIndexInitializer, MethodSecurityConfig, WebConfig
├── security/         # CustomUserDetails, HorarioLaboralFilter
├── model/            # 11 entidades JPA
├── enums/            # RolUsuario, EstadoExpediente, Urgencia, Naturaleza, EstadoActividad, TipoDocumento, TipoNotificacion, TipoNota
├── repository/       # 10 repositorios Spring Data JPA
├── dto/              # ExpedienteFormDTO, CambiarEstadoDTO
├── service/          # 7 servicios (@Transactional)
├── controller/       # 7 controllers MVC (@Controller, Thymeleaf)
└── exception/        # BusinessException + GlobalExceptionHandler
```

### Jerarquía de llamadas
```
Controller → DTO (validación @Valid)
    → Service (@Transactional, lógica de negocio)
        → Repository (Spring Data JPA)
            → PostgreSQL
```

---

## 4. PLANTILLAS DE CÓDIGO (SISEXP)

### SecurityConfig.java (Spring Security form login)
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/health", "/rastreo/**", "/login", "/logout"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/rastreo/**", "/api/health",
                    "/css/**", "/js/**", "/vendor/**", "/favicon.ico",
                    "/error", "/horario-cerrado").permitAll()
                .requestMatchers("/usuarios/**").hasRole("Administrador")
                .requestMatchers("/techos/**").hasAnyRole("Administrador","Coordinacion","Secretaria","Director")
                .requestMatchers("/reportes/**").hasAnyRole("Administrador","Coordinacion","Director","Decanato")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login").permitAll()
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error")
            )
            .rememberMe(remember -> remember
                .key("SisexpRememberMeKey2026!")
                .tokenValiditySeconds(2592000)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "remember-me")
            );
        return http.build();
    }
}
```

### HorarioLaboralFilter.java
```java
@Component
public class HorarioLaboralFilter extends OncePerRequestFilter {
    private static final ZoneId ZONA_PERU = ZoneId.of("America/Lima");
    private static final int HORA_INICIO = 8, HORA_FIN = 20;
    private static final Set<String> RUTAS_EXENTAS = Set.of(
            "/login", "/rastreo", "/api/health", "/error",
            "/css", "/js", "/vendor", "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        for (String exenta : RUTAS_EXENTAS)
            if (path.startsWith(exenta)) { chain.doFilter(request, response); return; }

        int hora = ZonedDateTime.now(ZONA_PERU).getHour();
        if (hora >= HORA_INICIO && hora < HORA_FIN) { chain.doFilter(request, response); return; }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user)
            if (!user.isHorarioRestringido()) { chain.doFilter(request, response); return; }

        if (auth != null) response.sendRedirect("/horario-cerrado");
        else response.sendRedirect("/login?horario");
    }
}
```

### DataInitializer.java (TRUNCATE CASCADE + seed mínimo)
```java
@Component @Order(2)
public class DataInitializer implements CommandLineRunner {
    // ... repositorios + JdbcTemplate + PasswordEncoder

    @Override @Transactional
    public void run(String... args) {
        if (usuarioRepo.count() > 0) {
            log.info("Limpiando datos existentes para nuevo seed...");
            jdbc.execute("TRUNCATE TABLE notificaciones RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE seguimiento_logs RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE documentos_adjuntos RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE expedientes RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE necesidades_pap RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE actividades_poi RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE techos_presupuestales RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE usuarios RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE roles RESTART IDENTITY CASCADE");
        }
        // seedRoles() + seedUsuarios() + seedTechos() + seedActividades()
        // NO seedNecesidades, seedExpedientes, seedSeguimientoLogs, seedNotificaciones
    }
}
```

### GlobalExceptionHandler.java (Thymeleaf — devuelve vista error)
```java
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, RedirectAttributes redirect, HttpServletRequest request) {
        redirect.addFlashAttribute("error", ex.getMessage());
        return "redirect:" + (request.getHeader("Referer") != null ?
                request.getHeader("Referer") : "/dashboard");
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Error interno", ex);
        model.addAttribute("status", 500);
        return "error";
    }
}
```

---

## 5. FLUJO DE GESTIÓN DE EXPEDIENTES

```
Techo Presupuestal (año, monto total)  → ADMIN/COORD
  └── Actividad POI (código, nombre, presupuesto)  → ADMIN/COORD
        └── Necesidad PAP (bien/servicio, cantidad, precio)  → ADMIN/COORD
              └── Expediente (solicitante, urgencia, estado)  → LAB/SECRETARIA
                    │
                    ├── Estados: Borrador → En_revision → Aprobado/Rechazado/Observado
                    │                    → Finalizado → Derivado
                    │
                    ├── Urgencia: Urgente / No tan urgente / Puede esperar
                    │
                    ├── Documentos adjuntos (metadatos, sin binario)
                    ├── SeguimientoLog (historial de cambios de estado)
                    └── Notificaciones (por cambio de estado)
```

### Roles y permisos
| Rol | Acceso |
|-----|--------|
| Administrador | TODO, bypass horario 24/7 |
| Coordinacion | Gestiona techos, POI, PAP, ve todos los expedientes |
| Secretaria | Crea expedientes, cambia estados |
| Director | Aprueba/rechaza expedientes |
| Laboratorio | Crea expedientes (solo ve los suyos) |
| Decanato | Reportes y lectura |

---

## 6. DOCKERFILE (multi-stage, carpeta `sisexp/`)

```dockerfile
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 7. RAILWAY.TOML

```toml
[build]
  builder = "DOCKERFILE"
  dockerfilePath = "sisexp/Dockerfile"

[deploy]
  numReplicas = 1
```

---

## 8. DEPLOY EN RAILWAY (desde GitHub)

1. Ir a [railway.app](https://railway.app) → Dashboard → **New Project**
2. **Deploy from GitHub repo** → seleccionar `Sisexp-Upla-SpringBoot`
3. Railway detecta `Dockerfile` + `railway.toml` automáticamente
4. **+ New → Database → PostgreSQL** (se linkea automáticamente)
5. Agregar variable: `SPRING_DATASOURCE_URL=jdbc:postgresql://...(autogenerado)...`
6. **Deploy** (cada push a main redeploy automático)
7. Domain: `https://sisexp-web-production.up.railway.app`

---

## 9. DEPENDENCIAS MAVEN (SISEXP)

```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.1</version>
</parent>
<dependencies>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-thymeleaf</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
    <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
    <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
    <dependency><groupId>org.thymeleaf.extras</groupId><artifactId>thymeleaf-extras-springsecurity6</artifactId></dependency>
</dependencies>
```

---

## 10. EXTRAS ÚTILES (SISEXP)

### Generar DOCX desde MD
```bash
pandoc documento.md -o documento.docx
```

### Railway CLI — ver logs
```bash
railway logs --service 4eeffaa2-2701-4b29-bf6b-f1f58e488b4a -n 100
```

### Railway CLI — deploy manual
```bash
railway up -e production -d
```
