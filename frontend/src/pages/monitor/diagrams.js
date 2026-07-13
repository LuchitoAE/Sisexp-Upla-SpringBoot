export const GLOBAL_DIAGRAMS = [
  {
    id: 'arquitectura-microservicios',
    title: 'Arquitectura de Microservicios',
    type: 'flowchart',
    desc: '12 contenedores Docker Compose: NGINX → Gateway → 4 servicios → 4 DBs + Eureka + RabbitMQ.',
    mermaid: `flowchart TB
    nginx("NGINX :80 - React SPA")
    gw("API Gateway :8080 - JWT + CORS + ActivityLog")
    eureka("Eureka Server :8761 - Service Discovery")
    auth("AUTH-SERVICE :8081 - Usuarios + JWT")
    pres("PRESUPUESTO-SERVICE :8082 - Techos + POI + PAP + Export")
    exp("EXPEDIENTE-SERVICE :8083 - Expedientes + Documentos")
    notif("NOTIFICACION-SERVICE :8084 - Notificaciones")
    adb[("auth-db :5433")]
    pdb[("presupuesto-db :5434")]
    edb[("expediente-db :5435")]
    ndb[("notificacion-db :5436")]
    rmq(("RabbitMQ :5672"))

    nginx -->|api| gw
    gw --> auth
    gw --> pres
    gw --> exp
    gw --> notif
    auth -.-> eureka
    pres -.-> eureka
    exp -.-> eureka
    notif -.-> eureka
    gw -.-> eureka
    auth === adb
    pres === pdb
    exp === edb
    notif === ndb
    exp -->|eventos| rmq
    rmq -->|consume| notif`
  },
  {
    id: 'componentes',
    title: 'Diagrama de Componentes',
    type: 'flowchart',
    desc: 'Estructura interna de cada microservicio: Controller → Service → Repository → Entity + sisexp-common.',
    mermaid: `flowchart TB
    subgraph common["sisexp-common (JAR compartido)"]
      ENUMS("10 enums compartidos")
      DTO("DTOs")
      UTIL("EnumUtils + BusinessException")
    end

    subgraph ms["Estructura por Microservicio"]
      direction TB
      CTL("CONTROLLER - @RestController")
      SVC("SERVICE - @Service @Transactional")
      REPO("REPOSITORY - JpaRepository")
      ENT("MODEL - @Entity @Table")
      CFG("CONFIG - DataInitializer + ExceptionHandler + Beans")

      CTL --> SVC
      SVC --> REPO
      REPO --> ENT
      CTL -.-> CFG
    end

    common -.-> ms`
  },
  {
    id: 'despliegue',
    title: 'Diagrama de Despliegue',
    type: 'flowchart',
    desc: 'Produccion: Railway (11 servicios backend) + Vercel (frontend). Desarrollo: Docker Compose local (12 contenedores).',
    mermaid: `flowchart TB
    subgraph vercel["Vercel (CDN)"]
      fe("React SPA - frontend-ivory-nine-43.vercel.app")
    end

    subgraph railway["Railway (PaaS) - proyecto 38350e4a"]
      gw2("API Gateway :8080 - api-gateway-production-e01a.up.railway.app")
      eureka2("Eureka Server :8761")
      auth2("AUTH-SERVICE :8081")
      pres2("PRESUPUESTO-SERVICE :8082")
      exp2("EXPEDIENTE-SERVICE :8083")
      notif2("NOTIFICACION-SERVICE :8084")
      adb2[("auth-db - auth_db")]
      pdb2[("presupuesto-db - presupuesto_db")]
      edb2[("expediente-db - expediente_db")]
      ndb2[("notificacion-db - notific_db")]
      rmq2(("RabbitMQ"))
    end

    subgraph docker["Docker Compose Local"]
      nginx3("NGINX :80")
      gw3("API Gateway :8080")
      eureka3("Eureka :8761")
      auth3("AUTH :8081")
      pres3("PRESUPUESTO :8082")
      exp3("EXPEDIENTE :8083")
      notif3("NOTIFICACION :8084")
      adb3[("auth-db :5433")]
      pdb3[("presupuesto-db :5434")]
      edb3[("expediente-db :5435")]
      ndb3[("notificacion-db :5436")]
      rmq3(("RabbitMQ :5672"))
    end

    fe -->|HTTPS /api| gw2
    gw2 --> auth2
    gw2 --> pres2
    gw2 --> exp2
    gw2 --> notif2
    docker -->|"docker compose up"| nginx3`
  },
];

export const AUTH_DIAGRAMS = [
  {
    id: 'cu-auth',
    title: 'Casos de Uso — AUTH-SERVICE',
    type: 'flowchart',
    desc: 'Actores y casos de uso del servicio de autenticacion: login JWT, gestion de usuarios, status del sistema.',
    mermaid: `flowchart LR
    A("Administrador") --> CU1("Gestionar Usuarios")
    C("Coordinacion") --> CU3("Ver Perfil")
    S("Secretaria") --> CU3
    D("Director") --> CU3
    L("Laboratorio") --> CU3
    DC("Decanato") --> CU3
    AN("Anonimo") --> CU2("Login JWT")
    A --> CU3
    A --> CU4("Ver Status Sistema")
    A --> CU5("Cambiar Password")
    CU3("Ver Perfil - /api/auth/me")
    CU2("Login - POST /api/auth/login")
    CU1("CRUD Usuarios - /api/usuarios")
    CU4("Status - GET /api/status")
    CU5("Cambiar Password - POST /api/usuarios/{id}/cambiar-password")`
  },
  {
    id: 'dominio-auth',
    title: 'Modelo del Dominio — AUTH-SERVICE',
    type: 'class',
    desc: 'Una entidad: Usuario con sus campos, enum RolUsuario y bounded context de autenticacion.',
    mermaid: `classDiagram
    class Usuario {
      Long id
      String nombre
      String email
      String password
      RolUsuario rol
      Boolean activo
      Boolean horarioRestringido
      Integer intentosFallidos
      LocalDateTime bloqueadoHasta
      LocalDateTime createdAt
      LocalDateTime updatedAt
    }

    class RolUsuario {
      Administrador
      Coordinacion
      Secretaria
      Director
      Laboratorio
      Decanato
    }
    Usuario --> RolUsuario : rol`
  },
  {
    id: 'robustez-auth',
    title: 'Robustez — CU-01: Gestionar Usuarios',
    type: 'flowchart',
    desc: 'Boundary → Controller → Entity. Admin gestiona usuarios a traves del CRUD protegido por JWT.',
    mermaid: `flowchart LR
    UP("UsuariosPage.js - React Form")
    AC3("ApiUsuarioController - GET/POST/PUT /usuarios")
    US("UsuarioService - listar / crear / editar / toggle")
    UR("UsuarioRepository - findByEmail / existsByEmail")
    ENT3("Usuario - nombre, email, rol, activo")
    UP --> AC3
    AC3 --> US
    US --> UR
    UR --> ENT3`
  },
  {
    id: 'secuencia-auth',
    title: 'Secuencia — Login JWT',
    type: 'sequence',
    desc: 'Flujo de autenticacion: frontend → gateway → auth-service. Validacion de credenciales y generacion de token.',
    mermaid: `sequenceDiagram
    participant F as Frontend
    participant GW as API Gateway
    participant AC as ApiAuthController
    participant AS as AuthService
    participant UR as UsuarioRepository
    F->>GW: POST /api/auth/login (email, password)
    GW->>AC: login(body)
    AC->>AS: autenticar(email, password)
    AS->>UR: findByEmail(email)
    UR-->>AS: Usuario
    AS->>AS: BCryptPasswordEncoder.matches()
    AS->>AS: generarToken(usuario)
    AS-->>AC: JWT + UsuarioDTO
    AC-->>GW: 200 (token, usuario)
    GW-->>F: JWT token
    F->>F: guardar en localStorage`
  },
  {
    id: 'clases-auth',
    title: 'Diagrama de Clases — AUTH-SERVICE',
    type: 'class',
    desc: 'Clases Java del servicio: controladores, servicios, repositorios, entidad, configuracion.',
    mermaid: `classDiagram
    class ApiAuthController {
      login(LoginRequest) LoginResponse
      me(Token) UsuarioDTO
      validate(Token) boolean
    }
    class ApiUsuarioController {
      listar() List
      obtenerPorId(id) UsuarioDTO
      crear(UsuarioDTO) UsuarioDTO
      editar(id, UsuarioDTO) UsuarioDTO
      toggleActivo(id) void
      cambiarPassword(id, PasswordDTO) void
    }
    class UsuarioService {
      autenticar(email, password) Usuario
      generarToken(Usuario) String
      listar() List
      crear(UsuarioDTO) Usuario
      editar(id, UsuarioDTO) Usuario
    }
    class UsuarioRepository {
      findByEmail(String) Usuario
      existsByEmail(String) boolean
    }
    class SecurityConfig {
      securityFilterChain() SecurityFilterChain
      passwordEncoder() BCryptPasswordEncoder
    }
    class JwtAuthenticationFilter {
      doFilterInternal(request, response, chain) void
    }
    class Usuario {
      Long id
      String nombre
      String email
      String password
      RolUsuario rol
      Boolean activo
    }

    ApiAuthController --> UsuarioService
    ApiUsuarioController --> UsuarioService
    UsuarioService --> UsuarioRepository
    UsuarioRepository --> Usuario
    SecurityConfig --> JwtAuthenticationFilter
    JwtAuthenticationFilter --> UsuarioService`
  },
];

export const PRESUPUESTO_DIAGRAMS = [
  {
    id: 'cu-presupuesto',
    title: 'Casos de Uso — PRESUPUESTO-SERVICE',
    type: 'flowchart',
    desc: 'Actores que interactuan con el servicio de presupuesto: techos, POI, PAP, notas, dashboard y reportes.',
    mermaid: `flowchart LR
    A("Administrador") --> CU2("Gestionar Techos")
    C("Coordinacion") --> CU2
    S("Secretaria") --> CU3("Gestionar POI")
    D("Director") --> CU4("Gestionar PAP")
    L("Laboratorio") --> CU7("Solicitar Nota Modif.")
    DC("Decanato") --> CU8("Consultar Dashboard")
    A --> CU3
    A --> CU5("Configurar Nota")
    C --> CU5
    C --> CU6("Exportar Reportes")
    D --> CU6
    DC --> CU6
    A --> CU4
    CU2("Techos - /api/techos-presupuestales")
    CU3("POI - /api/actividades-poi")
    CU4("PAP - /api/necesidades-pap")
    CU5("Notas - /api/notas-modificatorias")
    CU6("Reportes - /api/reportes")
    CU7("Solicitar Nota - POST /api/notas-modificatorias")
    CU8("Dashboard - /api/dashboard")`
  },
  {
    id: 'dominio-presupuesto',
    title: 'Modelo del Dominio — PRESUPUESTO-SERVICE',
    type: 'class',
    desc: '4 entidades: TechoPresupuestal → ActividadPOI → NecesidadPAP. NotaModificatoria independiente.',
    mermaid: `classDiagram
    TechoPresupuestal "1" -- "*" ActividadPOI : contiene
    ActividadPOI "1" -- "*" NecesidadPAP : contiene
    ActividadPOI "1" -- "*" NotaModificatoria : modifica

    class TechoPresupuestal {
      Long id
      Integer anio
      BigDecimal montoTotal
      BigDecimal montoUtilizado
      Boolean activo
      Boolean planificado
    }
    class ActividadPOI {
      Long id
      String codigo
      String nombre
      BigDecimal presupuestoAsignado
      BigDecimal saldoEjecutado
      BigDecimal saldoComprometido
      LocalDate fechaLimite
      EstadoActividad estado
      Boolean planificado
    }
    class NecesidadPAP {
      Long id
      String nombre
      Integer cantidad
      BigDecimal precioEstimado
      Naturaleza tipo
      String clasificadorGasto
      Integer cantidadDisponible
      BigDecimal montoDisponible
    }
    class NotaModificatoria {
      Long id
      String codigo
      TipoNota tipo
      EstadoNota estado
      String justificacion
      BigDecimal costoEstimadoReferencial
      BigDecimal montoTransferir
    }`
  },
  {
    id: 'robustez-presupuesto',
    title: 'Robustez — CU-09: Exportar Reportes',
    type: 'flowchart',
    desc: 'Boundary → Controller → Entity. Exportacion Excel (Apache POI 5.2.5) y PDF con marca de agua UPLA.',
    mermaid: `flowchart LR
    RP("ReportesPage.js - Boton Exportar")
    RC("ReportesController - GET /excel /pdf")
    EXS("ExportService - exportarExcel / exportarPDF")
    TEC("TechoPresupuestal")
    POI2("ActividadPOI")
    PAP("NecesidadPAP")
    RP --> RC
    RC --> EXS
    EXS --> TEC
    EXS --> POI2
    EXS --> PAP`
  },
  {
    id: 'secuencia-presupuesto',
    title: 'Secuencia — CU-07: Configurar Nota Modificatoria',
    type: 'sequence',
    desc: 'Admin configura una nota modificatoria: validacion de tipo, saldo, y creacion del nuevo item PAP.',
    mermaid: `sequenceDiagram
    participant F as Frontend
    participant GW as API Gateway
    participant NC as NotaCtrl
    participant NS as NotaService
    participant AR as ActividadRepo
    participant NR as NecesidadRepo
    F->>GW: PUT /notas/1/configurar
    GW->>NC: configurar(id, body)
    NC->>NS: configurar()
    NS->>NS: findById(nota)
    NS->>NS: validar tipo (inclusion_item)
    NS->>AR: findById(actId)
    AR-->>NS: actividad
    NS->>NS: validar saldo suficiente
    NS->>NR: save(nuevo PAP)
    NR-->>NS: PAP creado
    NS->>NS: nota.estado = configurada
    NS->>NS: save(nota)
    NS-->>NC: 200 OK
    NC-->>GW: 200
    GW-->>F: 200 OK`
  },
  {
    id: 'clases-presupuesto',
    title: 'Diagrama de Clases — PRESUPUESTO-SERVICE',
    type: 'class',
    desc: 'Clases Java: 4 controladores, servicios, repositorios, 4 entidades, RestTemplate a expediente-service.',
    mermaid: `classDiagram
    class ApiTechoController {
      listar() List
      crear(TechoDTO) Techo
      editar(id, TechoDTO) Techo
      finalizarPOI(id) void
      desbloquearPOI(id) void
    }
    class ApiActividadPOIController {
      listarPorTecho(techoId) List
      crear(techoId, DTO) ActividadPOI
      editar(id, DTO) ActividadPOI
      finalizarPAP(id) void
    }
    class ApiNecesidadPAPController {
      listarPorActividad(actId) List
      crear(actId, DTO) NecesidadPAP
      editar(id, DTO) NecesidadPAP
    }
    class NotaModificatoriaController {
      listar() List
      crear(NotaDTO) Nota
      configurar(id, body) Nota
      rechazar(id, obs) Nota
    }
    class ExportService {
      exportarExcelAnual(anio) byte[]
      exportarPDFAnual(anio) byte[]
      exportarExcelPOI(anio) byte[]
      exportarPDFPOI(anio) byte[]
    }
    class NotaModificatoriaService {
      crear(body) Nota
      configurar(id, config) Nota
      rechazar(id, obs) Nota
    }
    class RestTemplateConfig {
      restTemplate() RestTemplate
    }
    class TechoPresupuestal {
      Long id Integer anio
      BigDecimal montoTotal BigDecimal montoUtilizado
    }
    class ActividadPOI {
      Long id String nombre
      BigDecimal presupuestoAsignado
    }
    class NecesidadPAP {
      Long id String nombre
      Integer cantidad BigDecimal precioEstimado
    }
    class NotaModificatoria {
      Long id String codigo
      TipoNota tipo EstadoNota estado
    }

    ApiTechoController --> TechoPresupuestal
    ApiActividadPOIController --> ActividadPOI
    ApiNecesidadPAPController --> NecesidadPAP
    NotaModificatoriaController --> NotaModificatoriaService
    NotaModificatoriaService --> NotaModificatoria
    NotaModificatoriaService --> ActividadPOI
    NotaModificatoriaService --> NecesidadPAP
    ExportService --> TechoPresupuestal
    ExportService --> ActividadPOI
    ExportService --> NecesidadPAP`
  },
];

export const EXPEDIENTE_DIAGRAMS = [
  {
    id: 'cu-expediente',
    title: 'Casos de Uso — EXPEDIENTE-SERVICE',
    type: 'flowchart',
    desc: 'Actores del servicio de expedientes: crear, cambiar estado, subir documentos, rastreo publico.',
    mermaid: `flowchart LR
    A("Administrador") --> CU5("Crear Expediente")
    C("Coordinacion") --> CU6("Cambiar Estado")
    S("Secretaria") --> CU5
    D("Director") --> CU5
    L("Laboratorio") --> CU5
    A --> CU6
    C --> CU5
    AN("Anonimo") --> CU8("Rastrear Expediente")
    S --> CU7("Finalizar/Derivar")
    S --> CU9("Subir Documentos")
    CU5("Crear - POST /api/expedientes")
    CU6("Cambiar Estado - PUT /api/expedientes/{id}/estado")
    CU7("Finalizar/Derivar - PUT /api/expedientes/{id}/estado")
    CU8("Rastreo - GET /api/expedientes/rastreo/{codigo}")
    CU9("Subir Docs - POST /api/expedientes/{id}/documentos")`
  },
  {
    id: 'dominio-expediente',
    title: 'Modelo del Dominio — EXPEDIENTE-SERVICE',
    type: 'class',
    desc: '3 entidades: Expediente con sus DocumentoAdjunto y SeguimientoLog. Incluye maquina de estados.',
    mermaid: `classDiagram
    Expediente "1" -- "*" DocumentoAdjunto : adjunta
    Expediente "1" -- "*" SeguimientoLog : registra

    class Expediente {
      Long id
      String codigo
      Long actividadPOIId
      Long necesidadPAPId
      Long solicitanteId
      Urgencia urgencia
      Naturaleza naturaleza
      String descripcion
      EstadoExpediente estado
      String observacion
      Integer cantidadSolicitada
      BigDecimal costoEstimado
    }
    class DocumentoAdjunto {
      Long id
      TipoDocumento tipo
      String nombreOriginal
      String nombreArchivo
      String mimeType
      Long tamanio
    }
    class SeguimientoLog {
      Long id
      String estadoAnterior
      String estadoNuevo
      Long usuarioId
      String observacion
      LocalDateTime createdAt
    }

    note for Expediente "Transiciones:\nBorrador -> EnRevision -> Aprobado/Observado/Rechazado\nObservado -> EnRevision\nAprobado -> Finalizado/Derivado\nDerivado -> Finalizado"`,
  },
  {
    id: 'robustez-expediente',
    title: 'Robustez — CU-05: Crear Expediente + CU-06: Cambiar Estado',
    type: 'flowchart',
    desc: 'Dos flujos criticos: creacion con validacion de disponibilidad y cambio de estado con publicacion RabbitMQ.',
    mermaid: `flowchart TB
    subgraph creacion["CU-05: Crear Expediente"]
      EP("ExpedientePage.js - Form")
      AC("ApiExpedienteCtrl - POST /expedientes")
      ES("ExpedienteService - crear / generarNumero")
      DISP("Validacion Disponibilidad - saldo + fecha")
      EXP("Expediente - codigo, estado=Borrador")
      SL("SeguimientoLog - registro inicial")
      RMQ1("RabbitMQ - evento creado")
      EP --> AC --> ES
      ES --> DISP --> EXP
      ES --> SL
      ES --> RMQ1
    end

    subgraph cambio["CU-06: Cambiar Estado"]
      EP2("ExpedientePage.js - Botones estado")
      AC2("ApiExpedienteCtrl - PUT /id/estado")
      ES2("ExpedienteService - TRANSICIONES validas")
      EXP2("Expediente - setEstado")
      LOG2("SeguimientoLog - registro transicion")
      RMQ2("RabbitMQ - evento cambio")
      NTF("Notificacion - async")
      EP2 --> AC2 --> ES2
      ES2 --> EXP2
      ES2 --> LOG2
      ES2 --> RMQ2
      RMQ2 --> NTF
    end`
  },
  {
    id: 'secuencia-expediente',
    title: 'Secuencia — CU-05: Crear + CU-06: Aprobar Expediente',
    type: 'sequence',
    desc: 'Dos secuencias: creacion de expediente (validacion → codigo → log → evento) y aprobacion (transicion → log → notificacion).',
    mermaid: `sequenceDiagram
    participant F as Frontend
    participant GW as API Gateway
    participant EC as ExpedienteCtrl
    participant ES as ExpedienteService
    participant ER as ExpedienteRepo
    participant RMQ as RabbitMQ

    Note over F,RMQ: CU-05: Crear Expediente
    F->>GW: POST /expedientes
    GW->>EC: crear(body)
    EC->>ES: crear()
    ES->>ES: validar urgencia (EnumUtils)
    ES->>ES: generarNumero() sincronizado
    ES->>ER: save(expediente)
    ER-->>ES: saved
    ES->>ES: crearLog()
    ES->>RMQ: evento creado
    ES-->>EC: 201 CREATED
    EC-->>GW: 201
    GW-->>F: 201

    Note over F,RMQ: CU-06: Aprobar (EnRevision -> Aprobado)
    F->>GW: PUT /exp/1/estado (Aprobado)
    GW->>EC: actualizarEstado(id, Aprobado, obs, userId)
    EC->>ES: actualizarEstado()
    ES->>ES: validar transicion EnRevision->Aprobado
    ES->>ER: save(expediente)
    ER-->>ES: saved
    ES->>ES: crearLog(EnRevision, Aprobado)
    ES->>RMQ: evento aprobado
    ES-->>EC: 200
    EC-->>GW: 200
    GW-->>F: 200 OK`
  },
  {
    id: 'clases-expediente',
    title: 'Diagrama de Clases — EXPEDIENTE-SERVICE',
    type: 'class',
    desc: 'Clases Java: controlador, servicio, repositorios, 3 entidades, configuracion RabbitMQ.',
    mermaid: `classDiagram
    class ApiExpedienteController {
      listar(solicitanteId) List
      obtener(id) Expediente
      crear(ExpedienteDTO) Expediente
      actualizarEstado(id, estado, obs, userId) Expediente
      rastrear(codigo) Expediente
      subirDocumento(id, file) DocumentoAdjunto
      verificarDisponibilidad(poiId, papId, cant) Disponibilidad
    }
    class ExpedienteService {
      crear(ExpedienteDTO) Expediente
      actualizarEstado(id, estado, obs, userId) Expediente
      generarNumero() String
      obtenerConLogs(id) Expediente
      verificarDisponibilidad(poiId, papId, cant) boolean
    }
    class ExpedienteRepository {
      findByCodigo(String) Expediente
      findBySolicitanteId(Long) List
      countByCodigoStartingWith(String) Long
      findFirstByCodigoStartingWithOrderByCodigoDesc(String) Expediente
    }
    class DocumentoAdjuntoRepository {
      findByExpedienteId(Long) List
    }
    class SeguimientoLogRepository {
      findByExpedienteIdOrderByCreatedAtDesc(Long) List
    }
    class RabbitMQConfig {
      template() RabbitTemplate
      topicExchange() TopicExchange
    }
    class Expediente {
      Long id String codigo
      EstadoExpediente estado
      Urgencia urgencia
      BigDecimal costoEstimado
    }
    class DocumentoAdjunto {
      Long id TipoDocumento tipo
      String nombreOriginal Long tamanio
    }
    class SeguimientoLog {
      Long id String estadoAnterior
      String estadoNuevo Long usuarioId
    }

    ApiExpedienteController --> ExpedienteService
    ExpedienteService --> ExpedienteRepository
    ExpedienteService --> DocumentoAdjuntoRepository
    ExpedienteService --> SeguimientoLogRepository
    ExpedienteRepository --> Expediente
    DocumentoAdjuntoRepository --> DocumentoAdjunto
    SeguimientoLogRepository --> SeguimientoLog
    ExpedienteService --> RabbitMQConfig`
  },
];

export const NOTIFICACION_DIAGRAMS = [
  {
    id: 'cu-notificacion',
    title: 'Casos de Uso — NOTIFICACION-SERVICE',
    type: 'flowchart',
    desc: 'Consumidor de eventos RabbitMQ que genera notificaciones para usuarios sobre cambios en expedientes.',
    mermaid: `flowchart LR
    SYS("Sistema (RabbitMQ)") --> CU10("Recibir Notificaciones")
    A("Administrador") --> CU11("Consultar Notificaciones")
    C("Coordinacion") --> CU11
    S("Secretaria") --> CU11
    D("Director") --> CU11
    L("Laboratorio") --> CU11
    DC("Decanato") --> CU11
    A --> CU12("Marcar Leidas")
    CU10("Consumir - ExpedienteEventConsumer")
    CU11("Listar - GET /api/notificaciones?usuarioId=")
    CU12("Marcar - PUT /api/notificaciones/{id}/leer")`
  },
  {
    id: 'dominio-notificacion',
    title: 'Modelo del Dominio — NOTIFICACION-SERVICE',
    type: 'class',
    desc: 'Una entidad: Notificacion con sus tipos y estados. Se crea por eventos de RabbitMQ.',
    mermaid: `classDiagram
    class Notificacion {
      Long id
      Long usuarioId
      String mensaje
      TipoNotificacion tipo
      Boolean leida
      Long expedienteId
      LocalDateTime createdAt
    }

    class TipoNotificacion {
      observacion
      rechazo
      aprobacion
      alerta_fecha
      nota_aprobada
      nota_rechazada
      info
    }
    Notificacion --> TipoNotificacion : tipo`
  },
  {
    id: 'robustez-notificacion',
    title: 'Robustez — Consumir Eventos RabbitMQ y Generar Notificaciones',
    type: 'flowchart',
    desc: 'Boundary → Controller → Entity. Listener RabbitMQ consume eventos de expediente y crea notificaciones.',
    mermaid: `flowchart LR
    RMQ("RabbitMQ - evento expediente")
    LSN("ExpedienteEventConsumer - @RabbitListener")
    NS2("NotificacionService")
    NR("NotificacionRepository - save")
    NT("Notificacion - usuarioId, mensaje, tipo")
    ANC("ApiNotificacionController - GET /notificaciones")
    FRT("Frontend - campana notificaciones")
    RMQ --> LSN
    LSN --> NS2
    NS2 --> NR
    NR --> NT
    ANC --> FRT`
  },
  {
    id: 'secuencia-notificacion',
    title: 'Secuencia — Expediente → RabbitMQ → Notificacion',
    type: 'sequence',
    desc: 'Flujo completo: cambio de estado en expediente → publica evento → notificacion-service consume → crea notificacion.',
    mermaid: `sequenceDiagram
    participant EXP as ExpedienteService
    participant RMQ as RabbitMQ
    participant LSN as EventConsumer
    participant NS as NotificacionService
    participant NR as NotificacionRepo
    participant F as Frontend
    EXP->>RMQ: publica evento (estado cambiado)
    RMQ->>LSN: consume mensaje
    LSN->>NS: procesarEvento(mensaje)
    NS->>NS: parsear expedienteId, estado, usuarioId
    NS->>NS: construir mensaje notificacion
    NS->>NR: save(Notificacion)
    NR-->>NS: saved
    Note over F,NR: Cuando el usuario abre la app...
    F->>F: GET /api/notificaciones?usuarioId=1
    F->>F: GET /api/notificaciones/count?usuarioId=1`
  },
  {
    id: 'clases-notificacion',
    title: 'Diagrama de Clases — NOTIFICACION-SERVICE',
    type: 'class',
    desc: 'Clases Java: controlador, consumer RabbitMQ, repositorio, entidad.',
    mermaid: `classDiagram
    class ApiNotificacionController {
      listar(usuarioId) List
      countNoLeidas(usuarioId) Long
      marcarLeida(id) void
      marcarTodasLeidas(usuarioId) void
    }
    class ExpedienteEventConsumer {
      recibirEvento(mensaje) void
      procesarEvento(ExpedienteEvent) Notificacion
    }
    class NotificacionRepository {
      findByUsuarioIdOrderByCreatedAtDesc(Long) List
      countByUsuarioIdAndLeidaFalse(Long) Long
    }
    class Notificacion {
      Long id
      Long usuarioId
      String mensaje
      TipoNotificacion tipo
      Boolean leida
      Long expedienteId
      LocalDateTime createdAt
    }
    class RabbitMQConfig {
      connectionFactory() ConnectionFactory
      rabbitTemplate() RabbitTemplate
    }

    ApiNotificacionController --> NotificacionRepository
    ExpedienteEventConsumer --> NotificacionRepository
    NotificacionRepository --> Notificacion
    ExpedienteEventConsumer --> RabbitMQConfig`
  },
];
