---
title: "SISEXP-UPLA — Sistema de Seguimiento y Control de Expedientes"
subtitle: "Universidad Peruana Los Andes — Informe Tecnico Completo"
author: "Arquitectura de Software — VIII Ciclo — Julio 2026"
lang: es
---

# **SISEXP-UPLA — Arquitectura de Microservicios con Spring Boot**

---

## **1. Resumen Ejecutivo**

### **1.1 Que es SISEXP-UPLA**

SISEXP-UPLA es un sistema de seguimiento y control de expedientes disenado para la Universidad Peruana Los Andes. Gestiona el flujo presupuestal completo: desde la asignacion de techos presupuestales anuales hasta la creacion, aprobacion y seguimiento de expedientes, pasando por actividades POI (Plan Operativo Institucional) y necesidades PAP (Plan Anual de Contrataciones).

### **1.2 Flujo del Negocio**

```
Techo Presupuestal (anual)
    └── Actividad POI (por techo)
            └── Necesidad PAP (por actividad)
                   └── Expediente (por necesidad)
                          ├── Documentos Adjuntos
                          ├── Seguimiento de Estados
                          └── Notificaciones (via RabbitMQ)
```

Cada nivel superior restringe y alimenta al inferior. Un techo define el presupuesto maximo por ano; las actividades distribuyen ese presupuesto en rubros especificos; las necesidades PAP detallan bienes y servicios requeridos; y los expedientes formalizan las solicitudes con documentos, estados de aprobacion y seguimiento en tiempo real.

### **1.3 Objetivos del Sistema**

- Automatizar el flujo presupuestal de principio a fin
- Proveer trazabilidad completa de cada expediente (rastreo publico por codigo)
- Notificar en tiempo real cambios de estado via RabbitMQ
- Exponer un dashboard de monitoreo con actividad en vivo de todos los microservicios
- Generar reportes exportables en Excel (.xlsx) y PDF/HTML con branding institucional
- Permitir grabacion y reproduccion de acciones del usuario para demostraciones

---

## **2. Metodologia de Desarrollo**

### **2.1 Enfoque**

El proyecto se desarrollo siguiendo una **arquitectura de microservicios** con principios de **Domain-Driven Design (DDD)**. Cada contexto delimitado del negocio (Autenticacion, Presupuesto, Expedientes, Notificaciones) se implemento como un servicio independiente con su propia base de datos, eliminando acoplamientos fuertes y permitiendo despliegues y escalabilidad independientes.

### **2.2 Fases del Proyecto**

| **Fase** | **Actividad** | **Resultado** |
|:---------|:--------------|:--------------|
| **Fase 1** | Diseno de arquitectura y modelo de datos | 9 entidades, 10 enums, 4 bounded contexts definidos |
| **Fase 2** | Implementacion de microservicios core | auth-service, presupuesto-service, expediente-service, notificacion-service |
| **Fase 3** | Infraestructura | Eureka Server, API Gateway con Spring Cloud Gateway, NGINX, RabbitMQ, 4 PostgreSQL |
| **Fase 4** | Frontend React SPA | 9 modulos con Heroicons, dashboard KPIs, CRUD completo, monitor interactivo |
| **Fase 5** | Despliegue en la nube | 12 servicios en Railway, frontend en Vercel, CI/CD automatico |
| **Fase 6** | Correcciones y pulido | 37 bugs corregidos, GlobalExceptionHandler, validacion de estados, export Excel/PDF, rediseno UI con Heroicons |
| **Fase 7** | Documentacion | AGENTS.md, README.md, informe tecnico completo (este documento) |

### **2.3 Herramientas de Desarrollo**

| **Herramienta** | **Proposito** |
|:----------------|:--------------|
| IntelliJ IDEA / VS Code | IDEs de desarrollo |
| Docker Desktop | Orquestacion local de 12 contenedores |
| Railway CLI v4.30.5 | Despliegue y gestion de servicios en la nube |
| Vercel CLI | Despliegue del frontend React |
| Git + GitHub | Control de versiones y CI/CD |
| Maven Wrapper (mvnw) | Build de proyectos Spring Boot sin instalacion global |
| pnpm | Gestor de paquetes del frontend |
| Pandoc | Conversion de Markdown a DOCX profesional |

### **2.4 Principios de Diseno Aplicados**

- **SOLID**: Cada servicio tiene una unica responsabilidad. Las dependencias se inyectan por constructor.
- **Stateless**: Autenticacion JWT sin estado. No se almacenan sesiones en el servidor.
- **Database per Service**: Cada microservicio tiene su propia base de datos PostgreSQL independiente. La comunicacion entre servicios es via HTTP/REST o mensajeria asincrona (RabbitMQ).
- **Resiliencia**: Manejo de fallos con GlobalExceptionHandler centralizado por servicio. Fallbacks graceful en comunicacion cross-service (disponibilidad presupuestal).
- **Observabilidad**: ActivityLogFilter en el API Gateway captura toda la actividad. MonitorController expone feed en tiempo real. Eureka Dashboard para salud de servicios.

---

## **3. Stack Tecnologico**

| **Componente** | **Tecnologia** | **Version** | **Proposito** |
|:---------------|:---------------|:-----------:|:--------------|
| Lenguaje Backend | Java | 17 (LTS) | Servicios Spring Boot |
| Framework | Spring Boot | 3.4.1 | Microservicios REST |
| API Gateway | Spring Cloud Gateway | 4.1.x | Ruteo, JWT, CORS, Activity Log |
| Service Discovery | Netflix Eureka | 2.0.x | Registro y descubrimiento de servicios |
| Seguridad | JJWT (io.jsonwebtoken) | 0.12.6 | Autenticacion JWT stateless |
| ORM | Spring Data JPA / Hibernate | 6.6.x | Persistencia de entidades |
| Base de Datos | PostgreSQL | 16-alpine | 4 instancias independientes |
| Mensajeria | RabbitMQ | 3-management-alpine | Eventos async entre servicios |
| Frontend | React (CRA) | 19.x | SPA con lazy loading |
| Iconos | react-icons (Heroicons v2) | 5.7.0 | Iconografia outline profesional (67 iconos en 10 archivos) |
| Export Excel | Apache POI (poi-ooxml) | 5.2.5 | Generacion de archivos .xlsx con branding UPLA |
| Servidor Web | NGINX | alpine | Proxy inverso + serving SPA |
| Contenedores | Docker + Docker Compose | latest | 12 contenedores |
| Deploy Frontend | Vercel | — | Hosting gratuito SPA |
| Deploy Backend | Railway | — | 12 servicios en la nube |
| Documentacion | Pandoc | latest | MD a DOCX profesional |

---

## **4. Arquitectura del Sistema**

![](diagramas/arquitectura-microservicios.png)

### **4.1 Diagrama de Contenedores (12)**

El sistema se compone de 12 contenedores Docker orquestados con Docker Compose:

| **#** | **Contenedor** | **Puerto(s)** | **Imagen** | **Rol** |
|:-----:|:---------------|:-------------:|:-----------|:--------|
| 1 | sisexp-nginx | 80 | nginx:alpine | React SPA + proxy /api al gateway |
| 2 | sisexp-api-gateway | 8080 | Spring Boot | JWT, CORS, ruteo, ActivityLog, Monitor |
| 3 | sisexp-eureka | 8761 | Spring Boot | Service Discovery (Netflix Eureka) |
| 4 | sisexp-auth-service | 8081 | Spring Boot | Login JWT, usuarios, roles |
| 5 | sisexp-presupuesto-service | 8082 | Spring Boot | Techos, POI, PAP, Notas, Dashboard, Reportes, Export |
| 6 | sisexp-expediente-service | 8083 | Spring Boot | Expedientes, documentos, disponibilidad, RabbitMQ |
| 7 | sisexp-notificacion-service | 8084 | Spring Boot | Consume RabbitMQ, notificaciones |
| 8 | sisexp-auth-db | 5433 | postgres:16-alpine | Base de datos de autenticacion |
| 9 | sisexp-presupuesto-db | 5434 | postgres:16-alpine | Base de datos de presupuesto |
| 10 | sisexp-expediente-db | 5435 | postgres:16-alpine | Base de datos de expedientes |
| 11 | sisexp-notificacion-db | 5436 | postgres:16-alpine | Base de datos de notificaciones |
| 12 | sisexp-rabbitmq | 5672, 15672 | rabbitmq:3-management-alpine | Mensajeria asincrona |

### **4.2 Bounded Contexts (Domain-Driven Design)**

Cada microservicio representa un contexto delimitado del negocio:

| **Contexto** | **Servicio** | **Puerto** | **Responsabilidad** |
|:-------------|:-------------|:----------:|:--------------------|
| Autenticacion | auth-service | 8081 | Login JWT stateless, CRUD de usuarios, asignacion de roles, validacion de tokens |
| Presupuesto | presupuesto-service | 8082 | Gestion de techos presupuestales (90k-210k), actividades POI, necesidades PAP, notas modificatorias, dashboard de alertas y saldos, reportes anuales, export Excel/PDF. Usa RestTemplate para consultar expediente-service |
| Expedientes | expediente-service | 8083 | CRUD de expedientes, carga de documentos adjuntos, cambio de estados con maquina de estados, consulta de disponibilidad presupuestal (via RestTemplate a presupuesto-service), generacion de codigos unicos, publicacion de eventos a RabbitMQ |
| Notificaciones | notificacion-service | 8084 | Consumo de eventos RabbitMQ (expediente creado, estado cambiado), creacion y consulta de notificaciones por usuario, marcado como leidas |
| Monitoreo | api-gateway | 8080 | ActivityLogFilter intercepta todas las llamadas HTTP, traduce paths a descripciones humanas, almacena en buffer circular. MonitorController expone feed de actividad en tiempo real |
| Ruteo | api-gateway | 8080 | Punto unico de entrada, filtro JWT global, CORS configurado para dominios externos, ruteo load-balanced via Eureka |

### **4.3 Comunicacion entre Servicios**

| **Origen** | **Destino** | **Mecanismo** | **Proposito** |
|:-----------|:------------|:--------------|:--------------|
| Cliente (React SPA) | api-gateway | HTTP REST via NGINX proxy | Todas las llamadas API |
| api-gateway | Todos los servicios | Spring Cloud Gateway + Eureka LB | Ruteo de requests |
| expediente-service | presupuesto-service | RestTemplate (HTTP) | Consulta de disponibilidad presupuestal |
| presupuesto-service | expediente-service | RestTemplate (HTTP) | Dashboard de expedientes estancados + reportes |
| expediente-service | RabbitMQ | AMQP (publisher) | Eventos de expediente creado/cambiado |
| RabbitMQ | notificacion-service | AMQP (consumer) | Recepcion de eventos para generar notificaciones |
| Todos los servicios | eureka-server | HTTP (heartbeat) | Registro y descubrimiento |

### **4.4 Maquina de Estados del Expediente**

El expediente transita por 7 estados con reglas estrictas de transicion:

| **Estado Actual** | **Estados Permitidos Siguientes** |
|:------------------|:----------------------------------|
| Borrador | En revision |
| En revision | Aprobado, Rechazado, Observado |
| Observado | En revision |
| Aprobado | Finalizado, Derivado |
| Derivado | Finalizado |
| Rechazado | (estado terminal) |
| Finalizado | (estado terminal) |

Cada cambio de estado genera un `SeguimientoLog` y publica un evento en RabbitMQ. El frontend muestra feedback visual con modal de exito (verde) o error (rojo con mensaje descriptivo). Las transiciones invalidas (ej: Borrador → Rechazado) son bloqueadas tanto en el frontend (boton no se muestra) como en el backend (BusinessException → 400).

---

## **5. Modelo de Datos**

### **5.1 Entidades del Sistema (9)**

| **Entidad** | **Servicio** | **Campos Clave** | **Relaciones** |
|:------------|:-------------|:-----------------|:---------------|
| Usuario | auth-service | id, nombre, email, password (BCrypt), rol (enum), activo, horarioRestringido | — |
| TechoPresupuestal | presupuesto-service | id, ano, montoTotal (BigDecimal 12,2), montoUtilizado, planificado | 1:N con ActividadPOI |
| ActividadPOI | presupuesto-service | id, codigo, nombre, presupuestoAsignado, saldoEjecutado, saldoComprometido, fechaLimite, planificado | N:1 con Techo, 1:N con NecesidadPAP |
| NecesidadPAP | presupuesto-service | id, nombre, cantidad, precioEstimado, tipo (Naturaleza enum), clasificadorGasto, cantidadDisponible, cantidadEjecutada, montoDisponible, montoEjecutado | N:1 con ActividadPOI |
| NotaModificatoria | presupuesto-service | id, codigo, tipo (TipoNota enum), estado (EstadoNota enum), nuevoTipo (Naturaleza), justificacion (TEXT), montoTransferir, archivoPdf (BYTEA) | N:1 con ActividadPOI |
| Expediente | expediente-service | id, codigo (UNIQUE VARCHAR 20), actividadPOIId (FK), necesidadPAPId (FK), solicitanteId (FK), urgencia (Urgencia enum), naturaleza (Naturaleza enum), descripcion (TEXT), estado (EstadoExpediente enum), cantidadSolicitada, costoEstimado (BigDecimal 12,2), observacion | 1:N con DocumentoAdjunto, 1:N con SeguimientoLog |
| DocumentoAdjunto | expediente-service | id, tipo (TipoDocumento enum), nombreOriginal, nombreArchivo (UUID), mimeType, tamano (Long / BIGINT) | N:1 con Expediente |
| SeguimientoLog | expediente-service | id, estadoAnterior, estadoNuevo, usuarioId, observacion, createdAt | N:1 con Expediente |
| Notificacion | notificacion-service | id, usuarioId, mensaje, tipo (TipoNotificacion enum), leida, expedienteId, createdAt | — |

### **5.2 Tipos de Datos Criticos**

| **Entidad** | **Campo** | **Tipo SQL** | **Tipo Java** | **Justificacion** |
|:------------|:----------|:-------------|:--------------|:------------------|
| TechoPresupuestal | montoTotal | NUMERIC(12,2) | BigDecimal | Montos de hasta S/ 999,999,999.99 |
| ActividadPOI | presupuestoAsignado | NUMERIC(12,2) | BigDecimal | Precision financiera obligatoria |
| Expediente | costoEstimado | NUMERIC(12,2) | BigDecimal | Costo total del expediente |
| NotaModificatoria | justificacion | TEXT | String | Sin limite arbitrario |
| DocumentoAdjunto | tamano | BIGINT | Long | Soporta archivos > 2GB |
| Usuario | email | VARCHAR(254) | String | RFC 5321 |
| Expediente | codigo | VARCHAR(20) UNIQUE | String | Formato EXP-YYYY-NNNNN |

### **5.3 Enums del Sistema (10)**

| **Enum** | **Valores** | **Ubicacion** |
|:---------|:------------|:--------------|
| RolUsuario | Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato | sisexp-common |
| EstadoExpediente | Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado | sisexp-common |
| Urgencia | Urgente, No_tan_urgente, Puede_esperar | sisexp-common |
| Naturaleza | Bien, Servicio | sisexp-common |
| EstadoActividad | Pendiente, En_Ejecucion, Cerrado | sisexp-common |
| TipoDocumento | TDR, Especificaciones_Tecnicas, Cotizacion, Informe_Tecnico | sisexp-common |
| TipoNotificacion | observacion, rechazo, aprobacion, alerta_fecha, nota_aprobada, nota_rechazada, info | sisexp-common |
| TipoNota | inclusion_item, inclusion_actividad | sisexp-common |
| EstadoNota | pendiente, configurada, rechazada | sisexp-common |
| ActivityAction | LOGIN, CREAR, ACTUALIZAR, ELIMINAR, ESTADO, CONSULTA, SUBIR_DOC, CONFIGURAR, RECHAZAR, ERROR | api-gateway |

### **5.4 Claves Foraneas entre Servicios**

Las relaciones entre entidades de diferentes servicios se modelan como `Long` (ID numerico), sin `@ManyToOne`. Cada servicio es dueno de sus propios datos.

| **Entidad Origen** | **Campo FK** | **Servicio Destino** | **Entidad Destino** |
|:-------------------|:-------------|:---------------------|:--------------------|
| Expediente | actividadPOIId | presupuesto-service | ActividadPOI |
| Expediente | necesidadPAPId | presupuesto-service | NecesidadPAP |
| Expediente | solicitanteId | auth-service | Usuario |
| Notificacion | usuarioId | auth-service | Usuario |

---

## **6. API de Microservicios**

### **6.1 API Gateway — Punto Unico de Entrada**

Todas las peticiones externas ingresan por el API Gateway (`:8080`):

- Valida el JWT en todas las rutas excepto las exentas
- Aplica CORS con `allowedOriginPatterns=*`
- ActivityLogFilter intercepta toda la actividad (GlobalFilter orden 10)
- Rutea via Eureka load balancing

| **#** | **Ruta** | **Servicio** |
|:-----:|:---------|:-------------|
| 0 | /api/auth/** | auth-service |
| 1 | /api/usuarios/** | auth-service |
| 2 | /api/techos-presupuestales/** | presupuesto-service |
| 3 | /api/actividades-poi/** | presupuesto-service |
| 4 | /api/necesidades-pap/** | presupuesto-service |
| 5 | /api/notas-modificatorias/** | presupuesto-service |
| 6 | /api/expedientes/** | expediente-service |
| 7 | /api/dashboard/** | presupuesto-service |
| 8 | /api/reportes/** | presupuesto-service |
| 9 | /api/notificaciones/** | notificacion-service |
| 10 | /api/health | auth-service |
| 11 | /api/status | auth-service |
| 12 | /api/admin/reset-presupuesto | presupuesto-service |
| 13 | /api/admin/reset-expedientes | expediente-service |

### **6.2 Resumen de Endpoints por Servicio**

> **Total: 50+ endpoints** (originalmente 44, expandidos con export, edicion PAP, finalizar POI, reset admin)

| **Servicio** | **Endpoints** | **Destacados** |
|:-------------|:-------------:|:---------------|
| Auth Service | 6 | Login, CRUD usuarios, toggle activo/horario, cambiar password |
| Presupuesto Service | 28 | Techos CRUD, POI CRUD + finalizar/desbloquear, PAP CRUD + PUT editar, Notas CRUD + configurar/rechazar, Dashboard alertas/saldos, Reportes anual/POI/PAP/expedientes, Export Excel 4 tipos + PDF 4 tipos, Reset admin |
| Expediente Service | 8 | CRUD expedientes, cambio de estado, rastreo publico, documentos, disponibilidad cross-service |
| Notificacion Service | 4 | Listar, count, marcar leida, marcar todas leidas |
| Monitor (Gateway) | 2 | Activity feed, filtrado por servicio |

### **6.3 Endpoints de Export (Excel + PDF)**

Nuevos endpoints que generan archivos reales con branding institucional UPLA:

| **Metodo** | **Ruta** | **Formato** | **Contenido** |
|:-----------|:---------|:------------|:--------------|
| GET | /api/reportes/anual/{anio}/excel | .xlsx | Informe anual con resumen KPIs + hoja de actividades |
| GET | /api/reportes/anual/{anio}/pdf | .html | Informe anual con marca de agua UPLA, KPIs y tablas |
| GET | /api/reportes/expedientes/excel?anio= | .xlsx | Listado de expedientes del ano |
| GET | /api/reportes/expedientes/pdf?anio= | .html | Tabla de expedientes con estados y urgencias |
| GET | /api/reportes/poi/excel?anio= | .xlsx | Actividades POI con presupuestos y saldos |
| GET | /api/reportes/poi/pdf?anio= | .html | Tabla de actividades POI |
| GET | /api/reportes/pap/excel?anio= | .xlsx | Items PAP con cantidades y precios |
| GET | /api/reportes/pap/pdf?anio= | .html | Desglose completo del PAP |

---

## **7. Implementaciones Destacadas**

### **7.1 GlobalExceptionHandler**

Cada microservicio incluye un `@RestControllerAdvice` que captura excepciones de forma centralizada, evitando que errores no controlados lleguen al cliente como HTTP 500 sin contexto:

| **Excepcion** | **HTTP** | **Mensaje** |
|:--------------|:--------:|:------------|
| BusinessException | 400 | Mensaje de negocio descriptivo |
| IllegalArgumentException | 400 | "Valor no valido: ..." |
| NoSuchElementException | 404 | "Recurso no encontrado" |
| DataIntegrityViolationException | 409 | "Conflicto de datos (posible duplicado)" |
| Exception (generico) | 500 | "Error interno del servidor" + stacktrace en logs |

### **7.2 EnumUtils — Parseo Seguro de Enums**

Se creo una utilidad `EnumUtils.parseSafe()` en `sisexp-common` que reemplaza los 12+ llamados directos a `Enum.valueOf()`. Convierte espacios por guiones bajos automaticamente y lanza `BusinessException` con mensaje descriptivo si el valor es invalido.

### **7.3 ExportService — Excel con Branding UPLA**

El servicio de exportacion usa **Apache POI 5.2.5** para generar archivos `.xlsx` profesionales:

- **Header institucional**: "UNIVERSIDAD PERUANA LOS ANDES" en azul oscuro
- **Filas zebra**: alternancia de colores para legibilidad
- **Formato monetario**: columnas con alineacion derecha y formato `#,##0.00`
- **Multi-hoja**: informes anuales con hoja de resumen + hoja de actividades detalladas
- **PDF/HTML**: Marca de agua "SISEXP-UPLA" en diagonal, membrete institucional, KPIs resumen, tablas estilizadas con colores UPLA (azul `#1e3a5f`, dorado `#c9a84c`), optimizado para impresion con `@media print`

### **7.4 Rediseno UI con Heroicons**

Se reemplazaron **67 emojis** en **10 archivos** del frontend por **iconos Heroicons v2 Outline** profesionales. Cada icono se selecciono para corresponder semanticamente con su funcion:

| **Archivo** | **Iconos** | **Ejemplos** |
|:------------|:----------:|:-------------|
| ExpedientePage | 14 | Check, X, Search, Reply, PaperAirplane |
| MonitorPage | 24 | Globe, Cog, Database, Play, Pause, Refresh |
| NotaModificatoriaPage | 13 | DocumentAdd, Cube, CurrencyDollar, Cog |
| Dashboard | 7 | TrendingUp, Eye, Calendar, Clock |
| Header | 7 | Reply, CheckCircle, Calendar, BadgeCheck |
| Resto de paginas | 12 | LockClosed, LockOpen, PencilAlt, Trash, Download |

### **7.5 Sistema de Alertas y Feedbak Visual**

- **Modal de exito/error**: `App.js` ahora soporta variante `success` (check verde ✓) y `error` (X roja ✗) en el modal `alerta()`
- **Validacion de transiciones**: El boton "Rechazar" solo aparece en estados donde es valido (`En_revision`, `Observado`). En `Borrador` solo se permite "Enviar a revision"
- **Confirmacion de acciones**: Al cambiar estado, el frontend muestra "Listo: Expediente paso a [estado]" con modal verde

---

## **8. Frontend — Diseno y Componentes**

### **8.1 Modulos del Sistema (8 en sidebar + Monitor en header)**

| **Modulo** | **Icono** | **Roles** | **Descripcion** |
|:-----------|:----------|:----------|:---------------|
| Dashboard | HiOutlineChartBar | Todos | KPIs presupuestales, alertas semaforo, saldos en tiempo real |
| Expedientes | HiOutlineFolderOpen | Admin, Coord, Secretaria, Director, Lab | CRUD completo, documentos, cambio de estados con validacion |
| Techo Presupuestal | HiOutlineCurrencyDollar | Admin, Coord, Secretaria, Director | Gestion de techos, finalizar/desbloquear POI |
| Actividades POI | HiOutlineClipboardList | Admin, Coord, Secretaria, Director, Lab | Actividades con sub-lista PAP expandible |
| PAP | HiOutlineArchive | Todos | Vista jerarquica: POI → Items → Ejecucion |
| Reportes | HiOutlineChartPie | Admin, Coord, Director, Decanato | 4 tabs + export Excel/PDF con branding UPLA |
| Notas Modif. | HiOutlinePencilAlt | Todos excepto Decanato | Solicitudes de inclusion/modificacion |
| Usuarios | HiOutlineUsers | Admin | CRUD usuarios, toggle activo/horario |
| Monitor | HiOutlineDesktopComputer (Header) | Todos | Canvas 12 nodos arrastrables, activity feed, grabaciones |

### **8.2 Monitor — Canvas Interactivo de 12 Nodos**

Accesible desde el boton "Monitor" en el Header. Vista a pantalla completa (sin sidebar):

- **12 nodos arrastrables**: NGINX, Gateway, Eureka, 4 servicios, 4 bases de datos, RabbitMQ. Posiciones guardadas en localStorage
- **16 edges animados**: solidos (gateway routing), dashed (DB), dotted (Eureka/RabbitMQ)
- **Panel de detalle**: status, host, puerto, acciones recientes. Boton "Ir al modulo →"
- **Activity Feed**: timeline en tiempo real con polling cada 5s. Verde = exito, rojo = error
- **Grabacion/Replay**: boton "Grabar" en Header con toggle y timer. Sesiones guardadas en localStorage. Reproduccion secuencial con velocidad 1x/2x/4x
- **Tema oscuro**: `#0a0f14`

### **8.3 Reportes y Export**

Los reportes muestran datos en tiempo real con normalizacion correcta de campos del backend. El selector de ano usa por defecto el ultimo ano activo (2026). Los botones de export generan:

- **Excel**: archivo `.xlsx` real (Apache POI) con header UPLA, formato monetario, filas zebra, multi-hoja
- **PDF**: archivo `.html` estilizado con marca de agua, membrete institucional, KPIs, tablas, optimizado para Ctrl+P → Guardar como PDF

---

## **9. Despliegue**

![](diagramas/despliegue.png)

### **9.1 Entornos**

| **Entorno** | **Frontend** | **Backend** | **Estado** |
|:------------|:-------------|:------------|:----------:|
| Desarrollo Local | Docker Compose (12 contenedores) | Docker Compose | OK |
| Produccion | Vercel (gratuito) | Railway (12 servicios) | OK |

### **9.2 URLs de Produccion**

| **Recurso** | **URL** |
|:------------|:--------|
| Frontend SPA | https://frontend-ivory-nine-43.vercel.app |
| API Gateway | https://api-gateway-production-e01a.up.railway.app/api |
| Status (publico) | https://api-gateway-production-e01a.up.railway.app/api/status |

### **9.3 Railway — Variables Criticas**

| **Variable** | **Valor** | **Aplica a** |
|:-------------|:----------|:-------------|
| EUREKA_CLIENT_SERVICEURL_DEFAULTZONE | `http://sisexp-upla-springboot.railway.internal:8761/eureka` | Todos |
| EUREKA_INSTANCE_HOSTNAME | `{servicio}.railway.internal` | Cada servicio |
| JWT_SECRET | `SisexpJwtSecret2026MicroservicesKey!` | Todos |
| SPRING_DATASOURCE_URL | `jdbc:postgresql://{db-host}:5432/{db}` | Cada servicio |
| SPRING_RABBITMQ_HOST | `rabbitmq.railway.internal` | expediente, notificacion |
| PRESUPUESTO_SERVICE_URL | `http://presupuesto-service.railway.internal:8082` | expediente-service |

### **9.4 Lecciones Aprendidas en Railway**

1. **EUREKA_INSTANCE_HOSTNAME es obligatorio**: sin esta variable, los servicios registran su Docker container ID en Eureka, que no es resoluble por otros servicios → `UnknownHostException`
2. **CORS con `allowCredentials=false`**: necesario porque `allowedOriginPatterns=*` es incompatible con `allowCredentials=true`
3. **Railway CLI v4.30.5**: usar flags `--service`, `-y`. No abrir multiples ventanas (rate limit Cloudflare 1015)
4. **Builds desde raiz del proyecto**: el `dockerfilePath` en `railway.toml` es relativo a la raiz. Ejecutar `railway deployment up` desde el directorio root
5. **No usar ngrok**: el free tier muestra pagina interstitial que bloquea CORS preflight

### **9.5 Comandos de Despliegue**

```bash
# Railway — redesplegar un servicio
railway deployment up --service presupuesto-service   # desde raiz del proyecto

# Railway — resetear datos
curl -X POST https://api-gateway-production-e01a.up.railway.app/api/admin/reset-presupuesto \
  -H "Authorization: Bearer <TOKEN>"

# Vercel — forzar deploy
cd frontend && npx vercel deploy --prod --yes && cd ..

# Docker local — rebuild
docker compose build presupuesto-service expediente-service && docker compose up -d
```

---

## **10. Seed Data**

### **10.1 Datos de Prueba**

Los datos se cargan automaticamente al iniciar cada servicio si la base de datos esta vacia. Pueden resetearse manualmente con los endpoints `/api/admin/reset-*`.

| **Servicio** | **Cantidad** | **Detalle** |
|:-------------|:------------:|:------------|
| auth-service | 6 usuarios | 1 por cada rol. Formato: `{rol}@upla.edu.pe` / `{rol}123` |
| presupuesto-service | 5 techos | 2022: S/90k → 2026: S/210k (50% ejecutado) |
| presupuesto-service | 20 actividades POI | 4 por techo: MANT, EQUIP, CAPA, SERV/DIGI |
| presupuesto-service | 80 necesidades PAP | 4 por actividad. Bienes y servicios con precios realistas |
| presupuesto-service | 4 notas modificatorias | 2 pendientes, 1 configurada, 1 rechazada |
| expediente-service | 8 expedientes | En 7 estados distintos con diferentes urgencias y naturalezas |

### **10.2 Credenciales**

| **Rol** | **Email** | **Password** |
|:--------|:----------|:-------------|
| Administrador | jefe@upla.edu.pe | jefe123 |
| Coordinacion | coord@upla.edu.pe | coord123 |
| Secretaria | secretaria@upla.edu.pe | secretaria123 |
| Director | director@upla.edu.pe | director123 |
| Laboratorio | lab@upla.edu.pe | lab123 |
| Decanato | decanato@upla.edu.pe | decanato123 |

### **10.3 Estructura del Techo 2026**

| **Actividad** | **Presupuesto** | **Ejecutado** | **Items PAP** |
|:--------------|:---------------:|:-------------:|:-------------:|
| MANT-2026 — Mantenimiento de Infraestructura | S/ 73,500 | S/ 36,750 | Pintura, techos, luminarias, ascensores |
| EQUIP-2026 — Equipamiento de Laboratorios | S/ 63,000 | S/ 31,500 | Computadoras, proyectores, microscopios, servidores |
| CAPA-2026 — Capacitacion Docente | S/ 42,000 | S/ 21,000 | Talleres, certificaciones, material didactico, congresos |
| DIGI-2026 — Transformacion Digital | S/ 31,500 | S/ 15,750 | Plataforma LMS, tablets, videoconferencia, capacitacion TICs |

---

## **11. Seguridad**

### **11.1 Autenticacion JWT**

- **Algoritmo**: HMAC-SHA256
- **Expiracion**: 24 horas
- **Claims**: `sub` (email), `rol`, `iat`, `exp`
- **Validacion**: `JwtAuthFilter` en el API Gateway valida cada peticion. Rutas publicas: `/api/auth/login`, `/api/health`, `/api/expedientes/rastreo/*`, `/api/status`, `/api/monitor/*`

### **11.2 Roles**

6 roles con acceso diferenciado. Configuracion en `frontend/src/utils/config.js`. El sidebar oculta modulos no autorizados; el backend valida permisos en endpoints sensibles.

### **11.3 Passwords**

Hash BCrypt. El seed inicial hashea las passwords en texto plano antes de persistir.

---

## **12. Control de Versiones**

| **Elemento** | **Valor** |
|:-------------|:----------|
| Repositorio | https://github.com/LuchitoAE/Sisexp-Upla-SpringBoot |
| Rama principal | master |
| Estrategia de commits | Conventional commits descriptivos en espanol |
| CI/CD | Vercel auto-deploy en push a master + Railway via GitHub integration |
| Working Directory | `E:\proyecto\UPLA - Clases\octavo ciclo\arquitectura de software\semana 10\Proyecto-spring boot\` |

---

## **13. Correcciones y Mejoras (Fase 6)**

Durante la fase final del proyecto se identificaron y corrigieron **37 bugs** en una auditoria completa de 49 archivos. Las correcciones principales:

| **Categoria** | **Bugs** | **Mejora** |
|:--------------|:--------:|:-----------|
| Manejo de errores | 12 | GlobalExceptionHandler en los 4 servicios: adios a los HTTP 500 sin contexto |
| Validacion de estados | 4 | Transiciones invalidas bloqueadas en frontend y backend. Mensajes descriptivos |
| Exportacion | 8 | Excel real (.xlsx) con Apache POI + PDF/HTML con branding UPLA |
| UI/UX | 14 | 67 emojis reemplazados por Heroicons. Modal exito/error con colores |
| Datos | 5 | Techos 90k-210k (antes 800k-1.6M). PAP con FK matching corregido |
| Notificaciones | 4 | Rutas corregidas (leer/leer-todas). Parametro usuarioId agregado |
| Endpoints faltantes | 6 | PUT necesidades PAP, finalizar/desbloquear POI, reset admin |

---

## **14. Documentacion del Proyecto**

| **Archivo** | **Contenido** |
|:------------|:--------------|
| `AGENTS.md` | Guia completa para AI agents: contexto, arquitectura, endpoints, comandos, lecciones |
| `README.md` | Documentacion general: setup, endpoints, credenciales, deploy |
| `docker-compose.yml` | 12 contenedores con health checks |
| `docker-compose.test.yml` | Smoke test automatizado |
| `frontend/vercel.json` | Build y deploy en Vercel |
| `microservicios/*/railway.toml` | Config-as-code Railway (6 servicios) |
| `microservicios/*/Dockerfile` | Build multi-stage para cada servicio |
| `docs/INFORME_COMPLETO_SISEXP.md` | Este documento |
| `docs/INFORME_COMPLETO_SISEXP.docx` | Version Word generada con Pandoc |

---

<div style="text-align: center; margin-top: 40px; padding: 20px; border-top: 3px solid #1e3a5f;">

**SISEXP-UPLA** — Sistema de Gestion de Expedientes

Universidad Peruana Los Andes — Arquitectura de Software — VIII Ciclo

Julio 2026

</div>
