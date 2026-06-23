# PLAN.md — SISEXP-UPLA con Spring Boot + Thymeleaf

> **Sistema de Seguimiento y Control de Expedientes — Universidad Peruana Los Andes**
> Migración de Express/React a Spring Boot 3.4.1 + Thymeleaf + Bootstrap 5.3
> Arquitectura MVC tradicional: el servidor renderiza vistas. No SPA, no REST separada.

---

## Estructura del proyecto

```
Proyecto-spring boot/
├── sisexp/                              ← Proyecto Spring Boot Maven (TODO el código)
│   ├── pom.xml
│   ├── src/main/java/com/upla/sisexp/
│   │   ├── SisexpApplication.java      ← Main
│   │   ├── config/
│   │   │   ├── SecurityConfig.java
│   │   │   ├── DataInitializer.java    ← Seed
│   │   │   ├── DbIndexInitializer.java ← Indices
│   │   │   └── WebConfig.java
│   │   ├── security/
│   │   │   ├── CustomUserDetails.java
│   │   │   ├── CustomUserDetailsService.java
│   │   │   └── HorarioLaboralFilter.java
│   │   ├── model/                       ← 11 entidades JPA
│   │   ├── enums/                       ← 9 ENUMs
│   │   ├── repository/                  ← 11 repositorios
│   │   ├── dto/                         ← DTOs para formularios
│   │   ├── service/                     ← servicios + reglas de negocio
│   │   ├── controller/                  ← @Controller (Thymeleaf) + @RestController (AJAX)
│   │   └── exception/
│   │       ├── BusinessException.java
│   │       └── GlobalExceptionHandler.java
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   ├── templates/                   ← Vistas Thymeleaf
│   │   │   ├── layout.html
│   │   │   ├── login.html
│   │   │   ├── dashboard.html
│   │   │   ├── error.html
│   │   │   ├── rastreo.html
│   │   │   ├── fragments/
│   │   │   │   ├── sidebar.html
│   │   │   │   ├── header.html
│   │   │   │   └── scripts.html
│   │   │   ├── expedientes/
│   │   │   │   ├── lista.html
│   │   │   │   ├── formulario.html
│   │   │   │   └── detalle.html
│   │   │   ├── techos/lista.html
│   │   │   ├── poi/lista.html
│   │   │   ├── pap/lista.html
│   │   │   ├── notas/lista.html
│   │   │   ├── reportes/index.html
│   │   │   ├── usuarios/lista.html
│   │   │   └── notificaciones/lista.html
│   │   └── static/
│   │       ├── css/sisexp.css           ← Estilos propios
│   │       └── js/sisexp.js             ← JS auxiliar
│   └── Dockerfile
├── docker-compose.yml                   ← Spring Boot + PostgreSQL local
├── railway.toml
├── AGENTS.md                            ← Memoria del proyecto
├── PLAN.md                              ← Este archivo
└── docs/
    ├── referencia/
    │   ├── DOMINIO_SISEXP.md            ← Dominio completo extraído del código original
    │   └── doc/                         ← Documentación original (ERS, SDD, informes)
    └── diagramas/                       ← Para StarUML
```

> **Nota:** No hay carpeta `frontend/` separada. Thymeleaf vive dentro de `sisexp/src/main/resources/templates/`. Es un solo proyecto Maven monolítico.

---

## Dependencias entre fases

```
FASE 1 (Setup) ─────────────────────────────────────────────────────────────────┐
FASE 2 (Layouts) ← depende de F1 ──────────────────────────────────────────────┤
FASE 3 (Seguridad) ← depende de F2 ────────────────────────────────────────────┤
FASE 4 (Entidades + Repos) ← depende de F1 ────────────────────────────────────┤
FASE 5 (Seed Data) ← depende de F4 ────────────────────────────────────────────┤
FASE 6 (Dashboard) ← depende de F2, F3, F4, F5 ───────────────────────────────┤
FASE 7 (Expedientes) ← depende de F6 ─────┐                                     │
FASE 8 (Techo) ← depende de F6 ───────────┤                                     │
FASE 9 (Actividades POI) ← depende de F6 ─┤ Todos estos dependen de F6          │
FASE 10 (Necesidades PAP) ← depende de F6─┤ (Dashboard ya tiene sidebar,        │
FASE 11 (Notas) ← depende de F6 ──────────┤  auth, layout, y seed funcionando)  │
FASE 12 (Reportes) ← depende de F6 ───────┤                                     │
FASE 13 (Usuarios) ← depende de F6 ───────┤                                     │
FASE 14 (Rastreo) ← depende de F4 ────────┘ (solo necesita expedienteRepo)      │
FASE 15 (Notificaciones) ← depende de F6 ───────────────────────────────────────┤
FASE 16 (Business Rules) ← integradas en F7-F15 ────────────────────────────────┤
FASE 17 (Docker/Deploy) ← depende de todo ──────────────────────────────────────┘
```

---

## FASE 1: Instalación y configuración base

**Objetivo:** Proyecto Spring Boot compilable con Maven, PostgreSQL funcionando, estructura de paquetes creada.

**Dependencias:** Ninguna (es el inicio).

### Tareas

- [x] **1.1** Crear proyecto Maven `sisexp/`
  - Archivo: `sisexp/pom.xml`
  - Dependencias Maven exactas: spring-boot-starter-web, spring-boot-starter-data-jpa, spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-thymeleaf, thymeleaf-extras-springsecurity6, postgresql, h2 (dev), jjwt-api 0.12.6, jjwt-impl 0.12.6, jjwt-jackson 0.12.6, springdoc-openapi-starter-webmvc-ui 2.7.0
  - **Ref:** `AGENTS.md` sección 10 (Dependencias Maven)

- [x] **1.2** Crear clase main `SisexpApplication.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/SisexpApplication.java`
  - Package base: `com.upla.sisexp`

- [x] **1.3** Crear `application.properties`
  - Archivo: `sisexp/src/main/resources/application.properties`
  - Variables: `${SPRING_DATASOURCE_URL}`, pool HikariCP=20, batch_size=30, ddl-auto=update, thymeleaf cache=false (dev)
  - **Ref:** `AGENTS.md` sección 4 (application.properties)

- [x] **1.4** Crear carpetas de paquetes vacías
  - Crear: `config/`, `security/`, `model/`, `enums/`, `repository/`, `dto/`, `service/`, `controller/`, `exception/`
  - Crear: `templates/`, `templates/fragments/`, `templates/expedientes/`, `templates/techos/`, `templates/poi/`, `templates/pap/`, `templates/notas/`, `templates/reportes/`, `templates/usuarios/`, `templates/notificaciones/`
  - Crear: `static/css/`, `static/js/`

- [x] **1.5** Crear `docker-compose.yml` (raíz del proyecto)
  - Archivo: `docker-compose.yml`
  - Servicios: PostgreSQL 15 en puerto 5432 + sisexp-app en puerto 8080
  - **Ref:** `AGENTS.md` sección 6 (Dockerfile) y sección 7 (railway.toml)

- [x] **1.6** Crear `DbIndexInitializer.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/config/DbIndexInitializer.java`
  - Índices SISEXP: expedientes(estado, codigo, solicitante_id, actividad_poi_id, fecha_limite), actividades_poi(techo_presupuestal_id), necesidades_pap(actividad_poi_id), seguimiento_logs(expediente_id), documentos_adjuntos(expediente_id), notificaciones(usuario_id, leida)
  - **Ref:** `AGENTS.md` sección 4 (DbIndexInitializer)

- [x] **1.7** Crear `BusinessException.java` y `GlobalExceptionHandler.java`
  - Archivos: `exception/BusinessException.java`, `exception/GlobalExceptionHandler.java`
  - **Ref:** `AGENTS.md` sección 4 (GlobalExceptionHandler)

---

## FASE 2: Plantillas base y layouts Thymeleaf

**Objetivo:** Tener layout base con sidebar + header funcionando. CSS responsive. Sin datos reales aún (placeholder).

**Dependencias:** FASE 1 (proyecto compilable, propiedades, carpetas creadas).

### Tareas

- [x] **2.1** Crear `layout.html` — layout base decofrated
  - Archivo: `sisexp/src/main/resources/templates/layout.html`
  - Usa `th:fragment="layout"`, incluye sidebar + header + content placeholder
  - Usa `xmlns:th="http://www.thymeleaf.org"` y `xmlns:sec="http://www.thymeleaf.org/extras/spring-security"`

- [x] **2.2** Crear `fragments/sidebar.html` — navegación con control de roles
  - Archivo: `sisexp/src/main/resources/templates/fragments/sidebar.html`
  - Módulos por rol (6 variantes) usando `sec:authorize="hasRole('Administrador')"` etc.
  - Navegación: Dashboard, Expedientes, Techo Presupuestal, Actividades POI, PAP, Notas Modificatorias, Reportes, Usuarios, Notificaciones
  - **Ref:** `docs/referencia/DOMINIO_SISEXP.md` sección 3.4 (Navegación por rol)

- [x] **2.3** Crear `fragments/header.html` — barra superior
  - Archivo: `sisexp/src/main/resources/templates/fragments/header.html`
  - Muestra: nombre de usuario (`sec:authentication="name"`), rol, badge notificaciones, botón logout
  - Logout: `POST /logout` con CSRF token

- [x] **2.4** Crear `fragments/scripts.html` — CSS + JS común
  - Archivo: `sisexp/src/main/resources/templates/fragments/scripts.html`
  - Bootstrap 5.3 CSS/JS CDN, Bootstrap Icons CDN, Chart.js CDN
  - CSS sidebar responsive (mismo patrón FarmaGest: --sidebar-w, colapso en móvil, z-index)
  - **Ref:** `AGENTS.md` sección 5 (CSS Responsive sidebar)

- [x] **2.5** Crear `static/css/sisexp.css` — estilos propios
  - Archivo: `sisexp/src/main/resources/static/css/sisexp.css`
  - Variables CSS, sidebar, tarjetas KPI, barras de progreso, timeline, badges de estado

- [x] **2.6** Crear `static/js/sisexp.js` — JS auxiliar (mínimo)
  - Archivo: `sisexp/src/main/resources/static/js/sisexp.js`
  - Funciones: `toggleSidebar()`, confirmación de eliminación, previsualización de PDF, auto-close alerts

- [x] **2.7** Crear `WebConfig.java` — configurar Thymeleaf y recursos estáticos
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/config/WebConfig.java`

---

## FASE 3: Seguridad (Spring Security + sesiones + horario + RBAC)

**Objetivo:** Login funcional, remember-me 30 días, horario laboral, control de acceso por rol en páginas.

**Dependencias:** FASE 2 (layout con sidebar usando `sec:authorize` ya escrito).

### Tareas

- [x] **3.1** Crear `CustomUserDetails.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/security/CustomUserDetails.java`
  - Implementa `UserDetails`. Campos: id, nombre, email, password, rol, horarioRestringido, activo
  - `getAuthorities()`: `ROLE_Administrador`, `ROLE_Coordinacion`, etc.
  - **Ref:** `docs/referencia/DOMINIO_SISEXP.md` sección 2 (Usuario)

- [x] **3.2** Crear `CustomUserDetailsService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/security/CustomUserDetailsService.java`
  - Implementa `UserDetailsService`. Busca por email, valida activo, intentos, bloqueo
  - Usa `UsuarioRepository` (se crea en FASE 4, referenciar la interfaz)
  - **Ref:** `docs/referencia/DOMINIO_SISEXP.md` sección 6 (#3 Login con límite de intentos)

- [x] **3.3** Crear `SecurityConfig.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/config/SecurityConfig.java`
  - Configuración: formLogin (loginPage="/login", defaultSuccessUrl="/dashboard"), rememberMe (tokenValiditySeconds=2592000 → 30 días), logout (logoutSuccessUrl="/login?logout")
  - Páginas públicas: `/login`, `/rastreo/**`, `/css/**`, `/js/**`, `/api/health`, `/error`
  - BCryptPasswordEncoder bean
  - **Ref:** `AGENTS.md` sección 4 (SecurityConfig — adaptado a formLogin en vez de JWT stateless)
  - **ATENCIÓN:** El JwtAuthFilter de AGENTS.md NO se usa. Se usa formLogin + rememberMe.

- [x] **3.4** Crear `HorarioLaboralFilter.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/security/HorarioLaboralFilter.java`
  - Extiende `OncePerRequestFilter`. Hora Perú (`America/Lima`), 8am-8pm.
  - Bypass: usuarios con `horarioRestringido=false` (Admin)
  - Rutas exentas: `/login`, `/css/**`, `/js/**`, `/rastreo/**`, `/api/health`, `/error`
  - Si está fuera de horario y no tiene bypass: redirect a `/login?horario`
  - **Ref:** `docs/referencia/DOMINIO_SISEXP.md` sección 7

- [x] **3.5** Crear `login.html` — página de login
  - Archivo: `sisexp/src/main/resources/templates/login.html`
  - Formulario: email + password + remember-me checkbox
  - Mensajes: error de credenciales, cuenta bloqueada, fuera de horario, sesión expirada, logout exitoso
  - Botones de acceso rápido demo (6 roles) con JavaScript para rellenar credenciales
  - Mismo estilo que el SISEXP original (fondo gris, tarjeta centrada, logo UPLA)
  - **Ref:** `docs/referencia/DOMINIO_SISEXP.md` sección 10 (Seed — credenciales)

---

## FASE 4: Modelo de dominio — 11 entidades JPA + 9 ENUMs + 11 repositorios

**Objetivo:** Todas las entidades mapeadas, relaciones definidas, repositorios con queries custom.

**Dependencias:** FASE 1 (proyecto compilable, application.properties con JPA config).

**Archivos de referencia para TODOS los campos y tipos:**
- **Ref principal:** `docs/referencia/DOMINIO_SISEXP.md` sección 2 (especificación detallada de cada entidad)
- **Ref secundaria:** código original en `semana 5/mvcAS/SISEXP-UPLA/backend-as/src/models/`

### Tareas

- [x] **4.1** Crear paquete `enums/` — 9 archivos
  - Archivos:
    - `RolUsuario.java` — Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato
    - `EstadoActividad.java` — Pendiente, En_Ejecucion, Ejecutado, Cerrado
    - `EstadoExpediente.java` — Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado
    - `Urgencia.java` — Urgente, No_tan_urgente, Puede_esperar
    - `Naturaleza.java` — Bien, Servicio
    - `TipoDocumento.java` — TDR, Especificaciones_Tecnicas, Cotizacion, Informe_Tecnico
    - `TipoNota.java` — inclusion_item, inclusion_actividad
    - `EstadoNota.java` — pendiente, rechazada, configurada
    - `TipoNotificacion.java` — observacion, rechazo, aprobacion, alerta_fecha, nota_aprobada, nota_rechazada, info
  - **Ref:** `DOMINIO_SISEXP.md` sección 2.3

- [x] **4.2** Crear 10 entidades en `model/`
  - Archivos (crear en este orden por dependencias FK):
    1. `Usuario.java` — email UK, password, rol ENUM, activo, horarioRestringido, intentosFallidos, bloqueadoHasta
    2. `TechoPresupuestal.java` — año UK, montoTotal, montoUtilizado, creadoPor, activo, planificado
    3. `ActividadPOI.java` — @ManyToOne Techo, codigo, nombre, presupuestoAsignado, saldoComprometido, saldoEjecutado, fechaLimite, estado ENUM, planificado
    4. `NecesidadPAP.java` — @ManyToOne ActividadPOI, nombre, cantidad, precioEstimado, unidad, oficinaLaboratorio, tipo ENUM, clasificadorGasto, cantidadDisponible, montoDisponible, cantidadEjecutada, montoEjecutado
    5. `Expediente.java` — codigo UK, @ManyToOne ActividadPOI, @ManyToOne NecesidadPAP, @ManyToOne Usuario (solicitante), urgencia ENUM, naturaleza ENUM, descripcion, estado ENUM, observacion, fechaLimite, cantidadSolicitada, costoEstimado, @ManyToOne Usuario (aprobadoPor)
    6. `DocumentoAdjunto.java` — @ManyToOne Expediente, tipo ENUM, nombreOriginal, nombreArchivo, mimeType, tamaño
    7. `SeguimientoLog.java` — @ManyToOne Expediente, estadoAnterior, estadoNuevo, @ManyToOne Usuario, observacion, metadata, createdAt (solo create, sin update)
    8. `NotaModificatoria.java` — codigo UK, @ManyToOne Usuario (solicitante), origen, tipo ENUM, @ManyToOne ActividadPOI (existente), nuevoNombre, justificacion, costoEstimadoReferencial, nombreArchivo, nombreOriginalArchivo, @ManyToOne ActividadPOI (origen), montoTransferir, nuevoClasificadorGasto, nuevoTipo ENUM, estado ENUM, observacionAdmin, @ManyToOne Usuario (aprobadoPor)
    9. `Notificacion.java` — @ManyToOne Usuario, mensaje, tipo ENUM, @ManyToOne Expediente, leida, createdAt
    10. `Rol.java` — codigo UK, nombre, descripcion
  - **Ref:** `DOMINIO_SISEXP.md` sección 2.1 (diagrama ER completo) y 2.2 (especificación detallada)

- [x] **4.3** Crear 10 repositorios en `repository/`
  - Archivos:
    1. `UsuarioRepository.java` — findByEmail, findByRol
    2. `TechoPresupuestalRepository.java` — findByAño, findByActivoTrue
    3. `ActividadPOIRepository.java` — findByTechoPresupuestalId, findByEstado, count
    4. `NecesidadPAPRepository.java` — findByActividadPoiId, findByActividadPoiIdAndCantidadDisponibleGreaterThan
    5. `ExpedienteRepository.java` — findByCodigo, findTopByCodigoStartingWithOrderByCodigoDesc, countByEstado, findByEstado, countByFechaLimiteBeforeAndEstadoNotIn
    6. `DocumentoAdjuntoRepository.java` — findByExpedienteId
    7. `SeguimientoLogRepository.java` — findByExpedienteIdOrderByCreatedAtDesc
    8. `NotaModificatoriaRepository.java` — findByEstado, findBySolicitanteId
    9. `NotificacionRepository.java` — findByUsuarioIdAndLeidaFalse, countByUsuarioIdAndLeidaFalse
    10. `RolRepository.java` — findByCodigo

---

## FASE 5: Seed data (DataInitializer)

**Objetivo:** Al iniciar la app, insertar datos de prueba completos (6 usuarios, 20 POI, 13 PAP, 5 expedientes con logs).

**Dependencias:** FASE 4 (entidades + repositorios creados).

**Ref exacta:** `docs/referencia/DOMINIO_SISEXP.md` sección 10 (Seed data de referencia)

### Tareas

- [x] **5.1** Crear `DataInitializer.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/config/DataInitializer.java`
  - Implementa `CommandLineRunner`. Solo ejecuta si la BD está vacía (verificar count de usuarios).
  - Inserta en orden (respetando FKs):
    1. 6 Roles (ADMIN, COORD, SEC, DIR, LAB, DEC)
    2. 6 Usuarios con BCrypt (jefe@upla.edu.pe/jefe123, coord@upla.edu.pe/coord123, secretaria@upla.edu.pe/secretaria123, director@upla.edu.pe/director123, lab@upla.edu.pe/lab123, decanato@upla.edu.pe/decanato123)
    3. 2 Techos Presupuestales (2025 S/45,000 cerrado, 2026 S/115,000 activo)
    4. 20 Actividades POI (4 de 2025 cerradas, 16 de 2026 distribuyendo S/115,000)
    5. 13 Necesidades PAP (en 7 actividades)
    6. 5 Expedientes con estados variados
    7. 13 registros de SeguimientoLog (cronología de cada expediente)
    8. 5 Notificaciones de ejemplo
  - **ATENCIÓN:** Asegurar que los datos de seed coinciden exactamente con los del proyecto original (montos, ids, cantidades)

---

## FASE 6: Dashboard (primera página funcional post-login)

**Objetivo:** Página principal con KPIs, barras de progreso y alertas.

**Dependencias:** FASE 2 (layout), FASE 3 (seguridad), FASE 4 (entidades), FASE 5 (seed data).

### Tareas

- [x] **6.1** Crear `DashboardService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/DashboardService.java`
  - Métodos:
    - `getKPIs()` — total expedientes, count por cada estado, count vencidos (fechaLimite < hoy AND estado NOT IN Finalizado, Rechazado)
    - `getSaldos()` — JOIN actividades_poi + techos_presupuestales, calcular % ejecución
    - `getAlertas()` — expedientes próximos a vencer (7 días) y vencidos
  - **Ref:** `DOMINIO_SISEXP.md` sección 5.3 (Diagrama Dashboard)

- [x] **6.2** Crear `DashboardController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/DashboardController.java`
  - `@Controller`, `@GetMapping("/dashboard")`, carga KPIs + saldos + alertas en el Model
  - `@GetMapping("/")` → redirect a `/dashboard`

- [x] **6.3** Crear `templates/dashboard.html`
  - Archivo: `sisexp/src/main/resources/templates/dashboard.html`
  - Usa `layout.html` como decorator (`th:replace="layout :: layout"`)
  - 3 secciones:
    - Tarjetas KPIs (Bootstrap cards con colores por estado: total azul, aprobado verde, en revisión amarillo, rechazado rojo, vencidos rojo oscuro)
    - Barras de progreso presupuestal por actividad POI (Bootstrap progress-bar: ejecutado verde, comprometido naranja, disponible azul)
    - Lista de alertas (íconos de warning rojo/naranja + texto + fecha)

---

## FASE 7: Expedientes (módulo central)

**Objetivo:** CRUD completo de expedientes con ciclo de 7 estados, subida de documentos y timeline.

**Dependencias:** FASE 6 (dashboard funcional, sidebar navegable).

### Tareas

- [x] **7.1** Crear DTOs para formularios
  - Archivos: `dto/ExpedienteFormDTO.java`, `dto/CambiarEstadoDTO.java`, `dto/DocumentoDTO.java`

- [x] **7.2** Crear `ExpedienteService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/ExpedienteService.java`
  - CRUD: listar (filtrado por rol), crear, obtenerConLogs, actualizarEstado, adjuntarDocumento, eliminarDocumento
  - Integra llamadas a `BusinessRulesService` y `BusinessValidationsService` (se crean en FASE 16)
  - **Ref:** `DOMINIO_SISEXP.md` sección 5 y 6

- [x] **7.3** Crear `ExpedienteController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/ExpedienteController.java`
  - `@Controller` para vistas Thymeleaf:
    - `GET /expedientes` → lista
    - `GET /expedientes/nuevo` → formulario
    - `POST /expedientes` → guardar nuevo
    - `GET /expedientes/{id}` → detalle
    - `POST /expedientes/{id}/editar` → actualizar
    - `POST /expedientes/{id}/estado` → cambiar estado
    - `POST /expedientes/{id}/documentos` → subir archivo
    - `GET /expedientes/{id}/derivacion` → hoja imprimible
  - `@RestController` para AJAX:
    - `GET /api/necesidades/por-actividad/{actividadId}` → cargar selector PAP dinámico
    - `GET /api/expedientes/disponibilidad/{actividadId}/{necesidadId}` → validación en tiempo real

- [x] **7.4** Crear `templates/expedientes/lista.html`
  - Tabla con columnas: código, actividad POI, ítem PAP, solicitante, urgencia, estado (badge de color), fecha límite, acciones
  - Filtros: búsqueda por código/descripción, filtro por estado
  - Badge de color por estado: Borrador=gris, En revisión=azul, Aprobado=verde, Rechazado=rojo, Finalizado=verde oscuro, Observado=amarillo, Derivado=naranja
  - Botón "Nuevo expediente" (solo si rol tiene permiso EXP_CREAR)
  - Enlace a detalle al hacer click en código

- [x] **7.5** Crear `templates/expedientes/formulario.html`
  - Formulario con:
    - Selector de Actividad POI (carga inicial con @ModelAttribute)
    - Selector de Necesidad PAP (carga dinámica AJAX al seleccionar actividad, vía `/api/necesidades/por-actividad/{id}`)
    - Selector urgencia (Urgente / No tan urgente / Puede esperar)
    - Selector naturaleza (Bien / Servicio) — se autocompleta según PAP
    - Campo descripción (textarea)
    - Campo cantidad solicitada (number, default 1)
    - Campo fecha límite sugerida (date)
    - Costo estimado calculado automáticamente (cantidad × precio unitario PAP)
    - Validación en tiempo real: llamada AJAX a `/api/expedientes/disponibilidad/{actId}/{necId}?cantidad=X`
  - Validaciones sever-side: `@Valid` con mensajes de error

- [x] **7.6** Crear `templates/expedientes/detalle.html`
  - Secciones:
    1. **Datos del expediente:** tabla con todos los campos, badge de estado
    2. **Panel de clasificación:** selector dropdown con 7 estados + campo observación + botón "Actualizar estado"
       - Solo visible para Admin/Coordinación (`sec:authorize`)
       - Validación de transiciones permitidas según estado actual
    3. **Timeline de seguimiento:** lista vertical cronológica de SeguimientoLog
       - Cada entrada: estado anterior → estado nuevo, usuario, fecha, observación
       - Estilo: línea vertical con círculos de color por estado
    4. **Documentos adjuntos:** tabla + botón subir (modal con input file + selector tipo)
       - Tipos: TDR, Especificaciones Técnicas, Cotización, Informe Técnico
       - Solo PDF. Máximo 15 MB.
       - Botones: descargar, eliminar (solo Admin puede eliminar)
    5. **Botón Hoja de Derivación:** abre HTML imprimible en nueva pestaña
       - Solo visible si estado = Aprobado

---

## FASE 8: Techo Presupuestal

**Objetivo:** CRUD de techos presupuestales anuales.

**Dependencias:** FASE 6 (dashboard funcional).

### Tareas

- [x] **8.1** Crear `TechoPresupuestalService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/TechoPresupuestalService.java`

- [x] **8.2** Crear `TechoPresupuestalController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/TechoPresupuestalController.java`
  - Rutas: `GET /techos`, `POST /techos`, `POST /techos/{id}/editar`, `POST /techos/{id}/toggle`
  - `@PreAuthorize("hasRole('Administrador')")` para crear/editar

- [x] **8.3** Crear `templates/techos/lista.html`
  - Tabla: año, monto total (S/), monto utilizado (S/), % utilizado (barra progreso), activo, acciones
  - Modal crear/editar: año (number), monto total (decimal)

---

## FASE 9: Actividades POI

**Objetivo:** CRUD de actividades POI vinculadas a techos presupuestales.

**Dependencias:** FASE 6 (dashboard funcional), FASE 8 (techos creados).

### Tareas

- [x] **9.1** Crear `ActividadPOIService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/ActividadPOIService.java`

- [x] **9.2** Crear `ActividadPOIController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/ActividadPOIController.java`
  - `@PreAuthorize("hasRole('Administrador')")` para crear/editar
  - Ver: todos los roles (`POI_VER`)

- [x] **9.3** Crear `templates/poi/lista.html`
  - Tabla: código, nombre, techo (año), presupuesto, saldo comprometido, saldo ejecutado, disponible, % ejecutado (barra), estado (badge), fecha límite, acciones
  - Filtro por techo presupuestal (año)

---

## FASE 10: Necesidades PAP

**Objetivo:** CRUD de necesidades PAP vinculadas a actividades POI.

**Dependencias:** FASE 6 (dashboard), FASE 9 (actividades POI creadas).

### Tareas

- [x] **10.1** Crear `NecesidadPAPService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/NecesidadPAPService.java`

- [x] **10.2** Crear `NecesidadPAPController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/NecesidadPAPController.java`
  - `@PreAuthorize` para crear/editar (Admin, Coordinación)

- [x] **10.3** Crear `templates/pap/lista.html`
  - Tabla: nombre, actividad POI, cantidad planificada, precio unitario, tipo (badge Bien/Servicio), clasificador, cantidad disponible, cantidad ejecutada, monto disponible, acciones
  - Filtro por actividad POI

---

## FASE 11: Notas Modificatorias

**Objetivo:** Flujo completo de solicitudes de modificación presupuestal.

**Dependencias:** FASE 6 (dashboard), FASE 9 (actividades POI).

### Tareas

- [x] **11.1** Crear `NotaModificatoriaService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/NotaModificatoriaService.java`

- [x] **11.2** Crear `NotaModificatoriaController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/NotaModificatoriaController.java`

- [x] **11.3** Crear `templates/notas/lista.html`
  - Tabla: código, solicitante, tipo (inclusión item/actividad), actividad existente, nuevo nombre, costo referencial, estado (badge: pendiente=amarillo, configurada=verde, rechazada=rojo), acciones
  - Modal crear nota (solicitante): tipo, actividad existente, nuevo nombre, justificación, costo estimado, archivo sustento PDF
  - Modal configurar/aprobar (Admin): actividad origen, monto a transferir, clasificador, tipo (Bien/Servicio), observación

---

## FASE 12: Reportes

**Objetivo:** 4 tipos de reportes con tablas agregadas y exportación CSV.

**Dependencias:** FASE 6 (dashboard).

### Tareas

- [x] **12.1** Crear `ReporteService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/ReporteService.java`
  - Métodos: `expedientes()`, `poiGeneral()`, `poiEspecifico(actividadId)`, `papGeneral()`, `papEspecifico(actividadId)`, `informeAnual(año)`

- [x] **12.2** Crear `ReporteController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/ReporteController.java`
  - `@PreAuthorize` — solo Admin, Coordinacion, Decanato, Director (`REPORTES_VER`)

- [x] **12.3** Crear `templates/reportes/index.html`
  - 4 tabs (Bootstrap nav-tabs):
    1. **Expedientes:** tarjetas total + por estado + vencidos, gráfico de barras (Chart.js), tabla detalle
    2. **POI:** tabla con todas las actividades, % ejecución, barras de progreso
    3. **PAP:** tabla con necesidades, cantidades, montos, % ejecutado
    4. **Anual:** selector de año + tabla comparativa
  - Cada tab con botón "Exportar CSV"

---

## FASE 13: Usuarios (solo Administrador)

**Objetivo:** CRUD de usuarios con gestión de roles, horario y activación.

**Dependencias:** FASE 6 (dashboard).

### Tareas

- [x] **13.1** Crear `UsuarioService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/UsuarioService.java`

- [x] **13.2** Crear `UsuarioController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/UsuarioController.java`
  - `@PreAuthorize("hasRole('Administrador')")` en TODAS las rutas

- [x] **13.3** Crear `templates/usuarios/lista.html`
  - Tabla: nombre, email, rol (badge de color), perfil, horario restringido (toggle), activo (toggle), acciones
  - Colores por rol (mismos que SISEXP original): Admin=rojo #dc2626, Coord=azul #2563eb, Sec=violeta #7c3aed, Dir=cyan #0891b2, Lab=naranja #d97706, Dec=gris #64748b
  - Modal crear/editar: nombre, email, password (solo al crear), rol (select), horarioRestringido (checkbox)
  - **Ref:** `DOMINIO_SISEXP.md` sección 3.1 (Roles y colores)

---

## FASE 14: Consulta pública (rastreo)

**Objetivo:** Página pública sin autenticación para consultar expediente por código.

**Dependencias:** FASE 4 (ExpedienteRepository con findByCodigo).

### Tareas

- [x] **14.1** Crear `RastreoController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/RastreoController.java`
  - `GET /rastreo` — página con formulario de búsqueda (público, sin auth)
  - `POST /rastreo` — buscar por código, mostrar resultado (mismos datos que detalle pero simplificado)

- [x] **14.2** Crear `templates/rastreo.html`
  - Página independiente sin sidebar (layout propio simple)
  - Input de código + botón buscar
  - Si encuentra: mostrar datos básicos (código, estado, actividad, urgencia, última actualización)
  - Si no encuentra: mensaje "No se encontró el expediente"
  - Estilo: mismo que el SISEXP original (página limpia centrada)

---

## FASE 15: Notificaciones

**Objetivo:** Badge en header con conteo de no leídas, página de lista con opción marcar leída. Notificaciones automáticas al cambiar estado de expediente.

**Dependencias:** FASE 6 (dashboard), FASE 7 (expedientes con cambio de estado).

### Tareas

- [x] **15.1** Crear `NotificacionService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/NotificacionService.java`
  - Métodos: `crearNotificacion(usuarioId, mensaje, tipo, expedienteId)`, `marcarLeida(id)`, `contarNoLeidas(usuarioId)`

- [x] **15.2** Crear `NotificacionController.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/controller/NotificacionController.java`
  - `GET /notificaciones` — lista paginada
  - `@RestController` para AJAX:
    - `GET /api/notificaciones/count` — badge en header (se consulta en cada carga de página)

- [x] **15.3** Actualizar `fragments/header.html` para mostrar badge
  - Añadir span con contador de notificaciones no leídas (AJAX cada 60s o al cargar página)

- [x] **15.4** Crear `templates/notificaciones/lista.html`
  - Tabla: mensaje, tipo (badge), expediente (enlace), fecha, leída (checkbox)
  - Botón "Marcar todas como leídas"

- [x] **15.5** Integrar notificaciones automáticas en `ExpedienteService.actualizarEstado()`
  - Al cambiar estado: crear notificación para el solicitante del expediente
  - Tipos:
    - Aprobado → tipo `aprobacion`, mensaje: "Su expediente {codigo} ha sido aprobado"
    - Rechazado → tipo `rechazo`, mensaje: "Su expediente {codigo} ha sido rechazado. Motivo: {observacion}"
    - Observado → tipo `observacion`, mensaje: "Su expediente {codigo} tiene observaciones: {observacion}"
    - Alerta fecha → tipo `alerta_fecha` cuando falta ≤ 3 días para fecha límite

---

## FASE 16: Lógica de negocio (BusinessRulesService + BusinessValidationsService)

**Objetivo:** Centralizar todas las reglas de negocio y validaciones.

**Dependencias:** FASE 4 (entidades + repositorios). Se integra progresivamente en FASE 7 (expedientes) y resto de módulos.

**Ref exacta:** `docs/referencia/DOMINIO_SISEXP.md` sección 5 (BusinessRulesService) y sección 6 (BusinessValidationsService) — contienen el código Java listo para copiar.

### Tareas

- [x] **16.1** Crear `BusinessRulesService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/BusinessRulesService.java`
  - `@Service`, `@Transactional`
  - 8 métodos públicos (copiar código Java del DOMINIO_SISEXP.md sección 5):
    1. `validarFechaLimite(actividadPoiId)`
    2. `validarSaldoDisponible(actividadPoiId, costo)`
    3. `obtenerCostoNecesidad(necesidadPapId, cantidadSolicitada)`
    4. `obtenerInfoDisponibilidad(necesidadPapId)`
    5. `reservarSaldo(actividadPoiId, costo)`
    6. `liberarSaldo(actividadPoiId, costo)`
    7. `ejecutarSaldo(actividadPoiId, costo, necesidadPapId, cantidadSolicitada)`
    8. `reservarSaldoPAP()`, `liberarSaldoPAP()`, `ejecutarSaldoPAP()`
    9. `validarCorrespondenciaCaja(necesidadPapId, naturaleza)`
    10. `generarNumeroExpediente()` — consulta el último código del año y genera el siguiente

- [x] **16.2** Crear `BusinessValidationsService.java`
  - Archivo: `sisexp/src/main/java/com/upla/sisexp/service/BusinessValidationsService.java`
  - `@Service`
  - 10 validaciones (copiar código Java del DOMINIO_SISEXP.md sección 6):
    1. `checkLoginAttempts(usuario)` — 5 fallos = bloqueo 30 min
    2. `registerFailedAttempt(usuario)` — incrementar contador
    3. `validarInmutabilidad(expediente)` — Aprobado/Finalizado/Derivado no se modifican
    4. `getLimiteMontoPorRol(rol)` — límite S/ por rol
    5. `validarActividadActiva(actividadPoiId)` — no estado Cerrado
    6. `validarPAPObligatorio(actividadPoiId)` — al menos 1 necesidad registrada
    7. `validarTopeExpediente(actividadPoiId, costo)` — max 80% del disponible
    8. `validarPeriodoFiscal(actividadPoiId)` — no años pasados
    9. `validarDocumentoObligatorio(expedienteId)` — al menos 1 PDF
    10. `validarEdicionBloqueada(expediente)` — solo Borrador
    11. `validarTechoCerrado(techoPresupuestalId)` — planificado=true no modificar

- [x] **16.3** Integrar BusinessRulesService en servicios existentes
  - `ExpedienteService.crear()` → llamar validarFechaLimite, validarSaldoDisponible, validarCorrespondenciaCaja, validarTopeExpediente, validarActividadActiva, validarPeriodoFiscal, generarNumeroExpediente
  - `ExpedienteService.actualizarEstado()` → según transición: reservarSaldo, liberarSaldo, ejecutarSaldo (con sus versiones PAP)
  - `ExpedienteService.adjuntarDocumento()` → validarInmutabilidad
  - `CustomUserDetailsService` → checkLoginAttempts, registerFailedAttempt
  - `TechoPresupuestalService` → validarTechoCerrado
  - `ActividadPOIService` → validarActividadActiva

---

## FASE 17: Docker y despliegue

**Objetivo:** Contenerización completa, preparado para Railway o cualquier nube.

**Dependencias:** FASE 1-16 completas.

### Tareas

- [x] **17.1** Crear `Dockerfile` para sisexp
  - Archivo: `sisexp/Dockerfile`
  - Multi-stage: Maven build → JRE slim
  - Puerto: 8080
  - **Ref:** `AGENTS.md` sección 6 (Dockerfile)

- [ ] **17.2** Actualizar `docker-compose.yml`
  - Archivo: `docker-compose.yml` (raíz)
  - Servicios: `db` (PostgreSQL 15, puerto 5432) + `sisexp` (build: sisexp/, puerto 8080, depends_on: db)

- [ ] **17.3** Crear `railway.toml`
  - Archivo: `railway.toml` (raíz)
  - **Ref:** `AGENTS.md` sección 7 (railway.toml)

- [ ] **17.4** Verificar `application.properties` con variables de entorno
  - `SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/sisexp}`
  - `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
  - Funciona tanto local (docker-compose) como en Railway

---

## Resumen: orden de ejecución

```
FASE 1  → Setup (pom.xml, properties, carpetas, DbIndex, ExceptionHandler)
FASE 2  → Layouts Thymeleaf (sidebar, header, scripts, CSS, WebConfig)
FASE 3  → Seguridad (UserDetails, SecurityConfig, HorarioLaboral, login.html)
FASE 4  → 11 entidades JPA + 9 ENUMs + 11 repositorios
FASE 5  → Seed data (6 usuarios, techos, POI, PAP, expedientes, logs)

─── A PARTIR DE AQUÍ LA APP ES FUNCIONAL (login + seed) ───

FASE 6  → Dashboard (KPIs, saldos, alertas) ← primera página completa
FASE 7  → Expedientes (CRUD + 7 estados + docs + timeline + derivación)
FASE 8  → Techo Presupuestal (CRUD)
FASE 9  → Actividades POI (CRUD)
FASE 10 → Necesidades PAP (CRUD)
FASE 11 → Notas Modificatorias (flujo completo)
FASE 12 → Reportes (4 tipos con gráficos y exportación CSV)
FASE 13 → Usuarios (CRUD solo Admin)
FASE 14 → Rastreo público (consulta sin login)
FASE 15 → Notificaciones (badge + lista + automáticas)
FASE 16 → Business Rules (integradas progresivamente desde FASE 7)
FASE 17 → Docker + deploy
FASE 18 → Documentación ICONIX (ERS, BCE, SSD + DOCX)
```

### FASE 18: Documentación ICONIX (Completada ✅)
- **18.1** Skill: diagramas-ers (ISO 29148 + ICONIX Fase 1)
- **18.2** Skill: diagramas-bce (Robustez Boundary-Control-Entity Fase 3)
- **18.3** Skill: diagramas-ssd (System Sequence Diagrams Fase 4)
- **18.4** ERS_SISEXP_COMPLETO.md + .docx (14 RF, 8 RNF, 14 CU)
- **18.5** BCE_COMPLETO.md + .docx (14 diagramas Boundary-Control-Entity)
- **18.6** SSD_COMPLETO.md + .docx (14 diagramas de secuencia)
- **18.7** SISEXP_DIAGRAMAS_COMPLETO.md + .docx (documento consolidado)

---

## Convenciones

- **Package base:** `com.upla.sisexp`
- **Nombre de tablas:** snake_case plural (expedientes, actividades_poi, necesidades_pap, etc.)
- **Variables de entorno:** mayúsculas con `_` (SPRING_DATASOURCE_URL, JWT_SECRET, etc.)
- **Rutas:** kebab-case (/expedientes/nuevo, /api/necesidades/por-actividad)
- **Templates:** nombre simple (lista.html, formulario.html, detalle.html)
- **Métodos:** camelCase en Java, mismo nombre que el original Express para trazabilidad
- **Validaciones:** siempre server-side (`@Valid` + BindingResult) + feedback visual en HTML (`th:errors`, `th:classappend`)
- **Permisos:** `sec:authorize` en Thymeleaf para UI + `@PreAuthorize` en controllers para seguridad real
- **Mensajes flash:** `redirectAttributes.addFlashAttribute("success"|"error", mensaje)` para feedback post-redirect

---

## Archivos de referencia

| Archivo | Contenido |
|---|---|
| `AGENTS.md` | Plantillas genéricas Spring Boot + lecciones aprendidas FarmaGest |
| `docs/referencia/DOMINIO_SISEXP.md` | Dominio completo SISEXP (entidades, reglas, RBAC, endpoints, seed, diagramas) |
| `docs/referencia/doc/ERS_IEEE830_SISEXP_UPLA.docx` | Especificación de Requisitos original (IEEE 830) |
| `docs/referencia/doc/SDD_diseño_del_sistema.docx` | Diseño del Sistema original (IEEE 1016) |
| `docs/referencia/doc/Informe_Trazabilidad_v4.md` | Trazabilidad ICONIX completa con diagramas y prompts IA |
| `docs/referencia/doc/Analisis_Alineacion_SDD_ERS.md` | Gaps entre documentación e implementación |
| `docs/referencia/doc/Informe_Inconsistencias_SDD_ERS.md` | 10 inconsistencias detectadas (corregir en nueva doc) |
| `docs/referencia/doc/DEPLOY_SISEXP_original.md` | Guía de despliegue del proyecto original |
| `este archivo (PLAN.md)` | Plan de implementación detallado con tareas y dependencias |
