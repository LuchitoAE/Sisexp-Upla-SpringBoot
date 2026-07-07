---
title: "SISEXP-UPLA — Sistema de Seguimiento y Control de Expedientes"
subtitle: "Universidad Peruana Los Andes — Informe Tecnico Completo"
author: "Arquitectura de Software — VIII Ciclo — Julio 2026"
lang: es
---

# **SISEXP-UPLA — Arquitectura de Microservicios con Spring Boot**

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
- Permitir grabacion y reproduccion de acciones del usuario para demostraciones

---

## **2. Stack Tecnologico**

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
| Iconos | react-icons (Heroicons) | 5.7.0 | Iconografia outline profesional |
| Servidor Web | NGINX | alpine | Proxy inverso + serving SPA |
| Contenedores | Docker + Docker Compose | latest | 12 contenedores |
| Deploy Frontend | Vercel | — | Hosting gratuito SPA |
| Deploy Backend | Railway | — | 12 servicios en la nube |
| Documentacion | Pandoc | latest | MD a DOCX profesional |
| Testing | Bash + curl | — | Smoke test 21 endpoints |

---

## **3. Arquitectura del Sistema**

### **3.1 Diagrama de Contenedores (12)**

El sistema se compone de 12 contenedores Docker orquestados con Docker Compose:

| **#** | **Contenedor** | **Puerto(s)** | **Imagen** | **Rol** |
|:-----:|:---------------|:-------------:|:-----------|:--------|
| 1 | sisexp-nginx | 80 | nginx:alpine | React SPA + proxy /api al gateway |
| 2 | sisexp-api-gateway | 8080 | Spring Boot | JWT, CORS, ruteo, ActivityLog, Monitor |
| 3 | sisexp-eureka | 8761 | Spring Boot | Service Discovery (Netflix Eureka) |
| 4 | sisexp-auth-service | 8081 | Spring Boot | Login JWT, usuarios, roles |
| 5 | sisexp-presupuesto-service | 8082 | Spring Boot | Techos, POI, PAP, Notas, Dashboard, Reportes |
| 6 | sisexp-expediente-service | 8083 | Spring Boot | Expedientes, documentos, disponibilidad, RabbitMQ |
| 7 | sisexp-notificacion-service | 8084 | Spring Boot | Consume RabbitMQ, notificaciones |
| 8 | sisexp-auth-db | 5433 | postgres:16-alpine | Base de datos de autenticacion |
| 9 | sisexp-presupuesto-db | 5434 | postgres:16-alpine | Base de datos de presupuesto |
| 10 | sisexp-expediente-db | 5435 | postgres:16-alpine | Base de datos de expedientes |
| 11 | sisexp-notificacion-db | 5436 | postgres:16-alpine | Base de datos de notificaciones |
| 12 | sisexp-rabbitmq | 5672, 15672 | rabbitmq:3-management-alpine | Mensajeria asincrona |

### **3.2 Bounded Contexts (Domain-Driven Design)**

Cada microservicio representa un contexto delimitado del negocio:

| **Contexto** | **Servicio** | **Puerto** | **Responsabilidad** |
|:-------------|:-------------|:----------:|:--------------------|
| Autenticacion | auth-service | 8081 | Login JWT stateless, CRUD de usuarios, asignacion de roles, validacion de tokens |
| Presupuesto | presupuesto-service | 8082 | Gestion de techos presupuestales, actividades POI, necesidades PAP, notas modificatorias, dashboard de alertas y saldos, reportes anuales. Usa RestTemplate para consultar expediente-service |
| Expedientes | expediente-service | 8083 | CRUD de expedientes, carga de documentos adjuntos, cambio de estados con maquina de estados, consulta de disponibilidad presupuestal (via RestTemplate a presupuesto-service), generacion de codigos unicos, publicacion de eventos a RabbitMQ |
| Notificaciones | notificacion-service | 8084 | Consumo de eventos RabbitMQ (expediente creado, estado cambiado), creacion y consulta de notificaciones por usuario, marcado como leidas |
| Monitoreo | api-gateway | 8080 | ActivityLogFilter intercepta todas las llamadas HTTP, traduce paths a descripciones humanas, almacena en buffer circular. MonitorController expone feed de actividad en tiempo real |
| Ruteo | api-gateway | 8080 | Punto unico de entrada, filtro JWT global, CORS configurado para dominios externos, ruteo load-balanced via Eureka |

### **3.3 Comunicacion entre Servicios**

| **Origen** | **Destino** | **Mecanismo** | **Proposito** |
|:-----------|:------------|:--------------|:--------------|
| Cliente (React SPA) | api-gateway | HTTP REST via NGINX proxy | Todas las llamadas API |
| api-gateway | Todos los servicios | Spring Cloud Gateway + Eureka LB | Ruteo de requests |
| expediente-service | presupuesto-service | RestTemplate (HTTP) | Consulta de disponibilidad presupuestal |
| presupuesto-service | expediente-service | RestTemplate (HTTP) | Dashboard de expedientes estancados |
| expediente-service | RabbitMQ | AMQP (publisher) | Eventos de expediente creado/cambiado |
| RabbitMQ | notificacion-service | AMQP (consumer) | Recepcion de eventos para generar notificaciones |
| Todos los servicios | eureka-server | HTTP (heartbeat) | Registro y descubrimiento |

### **3.4 Maquina de Estados del Expediente**

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

Cada cambio de estado genera un `SeguimientoLog` con el estado anterior, nuevo estado, usuario que realizo el cambio, observacion y timestamp. Ademas publica un evento en RabbitMQ para que el servicio de notificaciones alerte a los usuarios involucrados.

---

## **4. Modelo de Datos**

### **4.1 Entidades del Sistema (9)**

| **Entidad** | **Servicio** | **Campos Clave** | **Relaciones** |
|:------------|:-------------|:-----------------|:---------------|
| Usuario | auth-service | id, nombre, email, password (BCrypt), rol (enum), activo | — |
| TechoPresupuestal | presupuesto-service | id, anio, monto (BigDecimal 12,2), montoUtilizado | 1:N con ActividadPOI |
| ActividadPOI | presupuesto-service | id, codigo, nombre, presupuestoAsignado, saldoEjecutado, saldoComprometido, fechaLimite | N:1 con Techo, 1:N con NecesidadPAP |
| NecesidadPAP | presupuesto-service | id, nombre, cantidad, precioEstimado, tipo (Naturaleza enum), clasificadorGasto, cantidadDisponible, cantidadEjecutada | N:1 con ActividadPOI |
| NotaModificatoria | presupuesto-service | id, tipo (TipoNota enum), estado (EstadoNota enum), nuevoTipo (Naturaleza), justificacion (TEXT), archivoPdf (BYTEA) | N:1 con ActividadPOI origen/destino |
| Expediente | expediente-service | id, codigo (UNIQUE), actividadPOIId (FK), necesidadPAPId (FK), solicitanteId (FK), urgencia (Urgencia enum), naturaleza (Naturaleza enum), descripcion (TEXT), estado (EstadoExpediente enum), cantidadSolicitada, costoEstimado (BigDecimal 12,2) | 1:N con DocumentoAdjunto, 1:N con SeguimientoLog |
| DocumentoAdjunto | expediente-service | id, tipo (TipoDocumento enum), nombreOriginal, nombreArchivo (UUID), mimeType, tamano (Long) | N:1 con Expediente |
| SeguimientoLog | expediente-service | id, estadoAnterior, estadoNuevo, usuarioId, observacion, createdAt | N:1 con Expediente |
| Notificacion | notificacion-service | id, usuarioId, mensaje, tipo (TipoNotificacion enum), leida, expedienteId, createdAt | — |

### **4.2 Tipos de Datos Criticos**

| **Entidad** | **Campo** | **Tipo SQL** | **Tipo Java** | **Justificacion** |
|:------------|:----------|:-------------|:--------------|:------------------|
| TechoPresupuestal | monto | NUMERIC(12,2) | BigDecimal | Montos de hasta S/ 999,999,999.99 |
| ActividadPOI | presupuestoAsignado | NUMERIC(12,2) | BigDecimal | Precision financiera obligatoria |
| ActividadPOI | saldoEjecutado | NUMERIC(12,2) | BigDecimal | Control de ejecucion presupuestal |
| ActividadPOI | saldoComprometido | NUMERIC(12,2) | BigDecimal | Control de compromisos |
| NecesidadPAP | precioEstimado | NUMERIC(12,2) | BigDecimal | Calculo de costo de expedientes |
| Expediente | costoEstimado | NUMERIC(12,2) | BigDecimal | Costo total del expediente |
| NotaModificatoria | justificacion | TEXT | String | Sin limite arbitrario (antes VARCHAR 2000) |
| DocumentoAdjunto | tamano | BIGINT | Long | Soporta archivos > 2GB (antes INTEGER, max 2.1GB) |
| Usuario | email | VARCHAR(254) | String | RFC 5321 (longitud maxima de email) |
| Usuario | nombre | VARCHAR(150) | String | Acotado para indices eficientes |
| Usuario | password | VARCHAR(255) | String | Hash BCrypt (60 caracteres + margen) |
| Expediente | codigo | VARCHAR(20) UNIQUE | String | Formato EXP-YYYY-NNN |
| Expediente | descripcion | TEXT | String | Descripciones extensas sin limite |

### **4.3 Enums del Sistema (10)**

| **Enum** | **Valores** | **Ubicacion** | **Uso** |
|:---------|:------------|:--------------|:-------|
| RolUsuario | Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato | sisexp-common | Roles de usuario con permisos diferenciados |
| EstadoExpediente | Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado | sisexp-common | Control de flujo del expediente |
| Urgencia | Urgente, No_tan_urgente, Puede_esperar | sisexp-common | Nivel de urgencia del expediente |
| Naturaleza | Bien, Servicio | sisexp-common | Tipo de requerimiento |
| EstadoActividad | Activo, Finalizado, Bloqueado | sisexp-common | Estado de actividad POI |
| TipoDocumento | TDR, Especificaciones_Tecnicas, Cotizacion, Informe_Tecnico | sisexp-common | Clasificacion de documentos adjuntos |
| TipoNotificacion | observacion, rechazo, aprobacion, alerta_fecha, nota_aprobada, nota_rechazada, info | sisexp-common | Categorias de notificaciones |
| TipoNota | inclusion_item, inclusion_actividad | sisexp-common | Tipo de nota modificatoria |
| EstadoNota | pendiente, configurada, rechazada | sisexp-common | Ciclo de vida de nota modificatoria |
| ActivityAction | LOGIN, CREAR, ACTUALIZAR, ELIMINAR, ESTADO, CONSULTA, SUBIR_DOC, CONFIGURAR, RECHAZAR, ERROR | api-gateway | Categorias de actividad para el monitor |

Todos los enums usan `@Enumerated(EnumType.STRING)` para almacenarse como texto legible en la base de datos, no como ordinales numericos.

### **4.4 Claves Foraneas entre Servicios**

Las relaciones entre entidades de diferentes servicios se modelan como `Long` (ID numerico), sin anotaciones `@ManyToOne`. Cada servicio es dueno de sus propios datos y consulta a otros servicios via RestTemplate cuando necesita datos complementarios.

| **Entidad Origen** | **Campo FK** | **Servicio Destino** | **Entidad Destino** |
|:-------------------|:-------------|:---------------------|:--------------------|
| ActividadPOI | techoPresupuestalId | presupuesto-service | TechoPresupuestal |
| NecesidadPAP | actividadPOIId | presupuesto-service | ActividadPOI |
| Expediente | actividadPOIId | presupuesto-service | ActividadPOI |
| Expediente | necesidadPAPId | presupuesto-service | NecesidadPAP |
| Expediente | solicitanteId | auth-service | Usuario |
| Notificacion | usuarioId | auth-service | Usuario |
| Notificacion | expedienteId | expediente-service | Expediente |

---

## **5. API de Microservicios**

### **5.1 API Gateway — Punto Unico de Entrada**

Todas las peticiones externas ingresan por el API Gateway (`:8080`), que:

- Valida el JWT en todas las rutas excepto las exentas: `/api/auth/login`, `/api/health`, `/api/expedientes/rastreo/*`, `/api/status`, `/api/monitor/*`
- Aplica CORS con `allowedOriginPatterns=*` y `allowCredentials=false`
- Intercepta y registra toda la actividad en el `ActivityLogFilter` (GlobalFilter orden 10)
- Rutea las peticiones a los servicios correspondientes via Eureka load balancing

| **#** | **Ruta** | **Servicio Destino** |
|:-----:|:---------|:---------------------|
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

### **5.2 Endpoints por Servicio (44 total)**

#### **Auth Service (5 endpoints)**

| **Metodo** | **Ruta** | **Auth** | **Descripcion** |
|:-----------|:---------|:--------:|:----------------|
| POST | /api/auth/login | No | Iniciar sesion. Retorna JWT + datos del usuario |
| GET | /api/auth/me | JWT | Obtener datos del usuario autenticado |
| GET | /api/usuarios | JWT | Listar todos los usuarios |
| GET | /api/status | No | Estado de salud de los 7 nodos |
| GET | /api/health | No | Health check basico |

#### **Presupuesto Service (14 endpoints)**

| **Metodo** | **Ruta** | **Auth** | **Descripcion** |
|:-----------|:---------|:--------:|:----------------|
| GET | /api/techos-presupuestales | JWT | Listar techos presupuestales |
| POST | /api/techos-presupuestales | JWT | Crear nuevo techo presupuestal |
| GET | /api/actividades-poi/techo/{id} | JWT | Listar actividades de un techo |
| POST | /api/actividades-poi/techo/{id} | JWT | Crear actividad en un techo |
| PUT | /api/actividades-poi/{id} | JWT | Editar actividad |
| DELETE | /api/actividades-poi/{id} | JWT | Eliminar actividad |
| GET | /api/necesidades-pap/actividad/{id} | JWT | Listar necesidades de una actividad |
| POST | /api/necesidades-pap/actividad/{id} | JWT | Crear necesidad PAP |
| DELETE | /api/necesidades-pap/{id} | JWT | Eliminar necesidad |
| GET/POST | /api/notas-modificatorias | JWT | Listar/crear notas modificatorias |
| PUT | /api/notas-modificatorias/{id}/configurar | JWT | Aprobar y aplicar nota |
| PUT | /api/notas-modificatorias/{id}/rechazar | JWT | Rechazar nota |
| GET | /api/dashboard/alertas | JWT | Alertas con semaforo (rojo/amarillo/verde) |
| GET | /api/dashboard/saldos | JWT | Saldos presupuestales por actividad |

#### **Expediente Service (6 endpoints)**

| **Metodo** | **Ruta** | **Auth** | **Descripcion** |
|:-----------|:---------|:--------:|:----------------|
| GET | /api/expedientes | JWT | Listar todos los expedientes |
| POST | /api/expedientes | JWT | Crear nuevo expediente (codigo auto-generado) |
| GET | /api/expedientes/{id} | JWT | Obtener expediente con documentos y logs |
| PUT | /api/expedientes/{id}/estado | JWT | Cambiar estado (maquina de estados) |
| GET | /api/expedientes/rastreo/{codigo} | No | Consulta publica por codigo |
| POST | /api/expedientes/{id}/documentos | JWT | Subir documento adjunto (PDF, max 20MB) |
| GET | /api/expedientes/disponibilidad/{poiId}/{papId} | JWT | Validar disponibilidad presupuestal antes de crear |

#### **Notificacion Service (2 endpoints)**

| **Metodo** | **Ruta** | **Auth** | **Descripcion** |
|:-----------|:---------|:--------:|:----------------|
| GET | /api/notificaciones?usuarioId= | JWT | Listar notificaciones del usuario |
| GET | /api/notificaciones/count?usuarioId= | JWT | Contar notificaciones no leidas |

#### **Monitor (2 endpoints — publicos, servidos por el gateway)**

| **Metodo** | **Ruta** | **Auth** | **Descripcion** |
|:-----------|:---------|:--------:|:----------------|
| GET | /api/monitor/activity?since=5 | No | Actividad del sistema en los ultimos N minutos |
| GET | /api/monitor/activity/service?name=X&since=5 | No | Actividad filtrada por servicio |

---

## **6. Implementacion**

### **6.1 Generacion de Codigos de Expediente**

El metodo `generarNumero()` en `ExpedienteService.java` produce codigos unicos con el formato `EXP-YYYY-NNN`. La implementacion final usa un contador basado en `COUNT(*)` de registros existentes para el ano actual, evitando los problemas de ordenamiento lexicografico que ocurrian con `OrderByCodigoDesc` cuando convivian codigos de 3 y 4 digitos.

```java
private String generarNumero() {
    int anio = Year.now().getValue();
    String prefix = "EXP-" + anio + "-";
    long count = expedienteRepo.countByCodigoStartingWith(prefix);
    return prefix + String.format("%03d", count + 1);
}
```

### **6.2 Resiliencia en Enums**

Los valores de enum enviados desde el frontend (con espacios para legibilidad humana, ej: `"No tan urgente"`) son normalizados en el backend reemplazando espacios por guiones bajos antes de llamar a `valueOf()`:

```java
e.setUrgencia(Urgencia.valueOf(urgencia.replace(' ', '_')));
e.setNaturaleza(Naturaleza.valueOf(naturaleza.replace(' ', '_')));
```

Esto permite que el frontend use texto amigable sin romper la convencion Java de enums con underscores.

### **6.3 Disponibilidad Presupuestal (Cross-Service)**

El endpoint `GET /api/expedientes/disponibilidad/{poiId}/{papId}` en expediente-service consulta a presupuesto-service via RestTemplate para obtener:

- Datos de la actividad POI: presupuesto asignado, saldo ejecutado, saldo comprometido, fecha limite
- Datos de la necesidad PAP: precio unitario, cantidad planificada, disponible, ejecutada, tipo, clasificador
- Calculo del costo estimado: `cantidadSolicitada x precioUnitario`
- Validacion de fecha limite (si ya vencio)
- Validacion de saldo suficiente (asignado - ejecutado - comprometido - costo >= 0)
- Fallback graceful: si presupuesto-service no responde, retorna valores por defecto que permiten continuar

### **6.4 ActivityLogFilter — Monitoreo en Tiempo Real**

El `ActivityLogFilter` es un `GlobalFilter` de Spring Cloud Gateway (orden 10) que intercepta todas las peticiones HTTP y:

1. Extrae el email del usuario del JWT (claim `sub`)
2. Traduce el path y metodo HTTP a una accion humana: `"jefe@upla.edu.pe creo un nuevo expediente"`
3. Resuelve el servicio responsable del path
4. Almacena el evento en un `ActivityBuffer` (buffer circular de 200 eventos thread-safe)
5. Expone los datos via `MonitorController` en endpoints publicos

**Tabla de traduccion path a descripcion:**

| **Path Pattern** | **Metodo** | **Descripcion Generada** |
|:-----------------|:-----------|:-------------------------|
| /api/auth/login | POST | "usuario inicio sesion" |
| /api/expedientes | POST | "usuario creo un nuevo expediente" |
| /api/expedientes/{id}/estado | PUT | "usuario cambio estado del expediente #N" |
| /api/expedientes/{id}/documentos | POST | "usuario subio documento al expediente #N" |
| /api/notas-modificatorias/{id}/configurar | PUT | "usuario configuro nota modificatoria #N" |
| /api/notas-modificatorias/{id}/rechazar | PUT | "usuario rechazo nota modificatoria #N" |
| /api/expedientes/rastreo/{codigo} | GET | "Anonimo consulto estado de {codigo}" |

### **6.5 Sistema de Grabacion y Reproduccion**

El sistema permite grabar sesiones de uso y reproducirlas posteriormente en el monitor:

- **Activacion**: boton "Grabar" en el Header con toggle y dot rojo pulsante con timer
- **Captura**: `client.js` intercepta todas las llamadas API (GET, POST, PUT, DELETE, PATCH, UPLOAD) y guarda `{ ts, method, path, status, bodySnapshot }`
- **Almacenamiento**: `localStorage.sisexp_recordings`, hasta 20 sesiones
- **Reproduccion**: en el Monitor, panel "Grabaciones" lista las sesiones guardadas. Al seleccionar "Reproducir", las acciones se ejecutan secuencialmente con delay de 500ms, iluminando los nodos del canvas correspondientes
- **Controles**: velocidad 1x/2x/4x, pausa, detener

---

## **7. Frontend — Diseno y Componentes**

### **7.1 Tecnologias del Frontend**

| **Tecnologia** | **Proposito** |
|:---------------|:--------------|
| React 19 | SPA con lazy loading y code splitting |
| react-icons 5.7.0 (Heroicons outline) | Iconografia profesional en toda la interfaz |
| Context API | Auth (JWT), Recorder (grabacion), Modals |
| CSS Modules + inline styles | Estilizado de componentes |
| NGINX | Serving de static assets + proxy inverso a /api |

### **7.2 Modulos del Sistema (9)**

| **Modulo** | **Icono** | **Roles con Acceso** | **Descripcion** |
|:-----------|:----------|:---------------------|:----------------|
| Dashboard | HiOutlineChartBar | Todos | KPIs presupuestales por ano con selector, alertas semaforo, expedientes estancados |
| Expedientes | HiOutlineFolderOpen | Admin, Coord, Secretaria, Director, Lab | CRUD de expedientes, carga de documentos, cambio de estados, consulta de disponibilidad |
| Techo Presupuestal | HiOutlineCurrencyDollar | Admin, Coord, Secretaria, Director | Gestion de techos anuales |
| Actividades POI | HiOutlineClipboardList | Admin, Coord, Secretaria, Director, Lab | Gestion de actividades por techo |
| PAP | HiOutlineArchive | Todos | Gestion de necesidades PAP |
| Reportes | HiOutlineChartPie | Admin, Coord, Director, Decanato | Reportes anuales, por POI, por PAP |
| Notas Modif. | HiOutlinePencilAlt | Admin, Coord, Secretaria, Director, Lab, Decanato | Creacion y gestion de notas modificatorias |
| Usuarios | HiOutlineUsers | Admin | Gestion de usuarios del sistema |
| Monitor | HiOutlineDesktopComputer | Todos | Dashboard de monitoreo con 12 nodos, activity feed, grabaciones |

### **7.3 Deteccion de Entorno (config.js)**

El frontend detecta automaticamente si esta corriendo en local (Docker) o en Vercel (produccion) y configura la URL correcta de la API:

```javascript
window.__SISEXP_CONFIG__ = {
  API_URL: (function() {
    var host = window.location.hostname;
    if (host === "localhost" || host === "127.0.0.1") {
      return "/api";  // Docker local: NGINX proxy a gateway
    }
    return "https://api-gateway-production-e01a.up.railway.app/api";  // Vercel → Railway
  })()
};
```

### **7.4 Monitor — Canvas Interactivo de 12 Nodos**

La pagina de monitor es una experiencia a pantalla completa (sin sidebar ni header) que incluye:

- **Canvas de 12 nodos arrastrables**: cada nodo representa un contenedor Docker. Posiciones guardadas en localStorage
- **16 edges animados**: conexiones visuales entre servicios (solid=gateway routing, dashed=DB, dotted=Eureka/RabbitMQ)
- **Panel de detalle**: al clickear un nodo muestra status, host, puerto, instancias, acciones recientes y componentes. Incluye boton "Ir al modulo →" para navegar directamente a la pagina correspondiente
- **Activity Feed**: timeline en tiempo real con las acciones capturadas por el ActivityLogFilter. Verde para exitos, rojo para errores. Filtrable por servicio
- **Panel de Grabaciones**: lista sesiones guardadas con botones de reproducir y eliminar
- **Top Bar**: contador UP/DOWN, latencia, botones Pausar/Sondear
- **Tema oscuro**: `#0a0f14` fondo, optimizado para visualizacion prolongada

---

## **8. Despliegue**

### **8.1 Entornos**

| **Entorno** | **Frontend** | **Backend** | **Estado** |
|:------------|:-------------|:------------|:----------:|
| Desarrollo Local | Docker Compose (12 contenedores) | Docker Compose | OK |
| Produccion | Vercel (gratuito) | Railway (12 servicios) | OK |

### **8.2 Railway — Configuracion de Servicios**

**URL publica del API Gateway:** `https://api-gateway-production-e01a.up.railway.app`

**Proyecto ID:** `38350e4a-d078-4836-bf40-290719260fde`

#### **Variables de Entorno Criticas**

| **Variable** | **Valor** | **Aplica a** |
|:-------------|:----------|:-------------|
| EUREKA_CLIENT_SERVICEURL_DEFAULTZONE | `http://sisexp-upla-springboot.railway.internal:8761/eureka` | Todos los servicios |
| EUREKA_INSTANCE_HOSTNAME | `{nombre}-service.railway.internal` | Cada servicio |
| JWT_SECRET | `SisexpJwtSecret2026MicroservicesKey!` | Todos |
| JWT_EXPIRATION | `86400000` | auth-service |
| SPRING_DATASOURCE_URL | `jdbc:postgresql://{db}.railway.internal:5432/{db}` | Cada servicio |
| SPRING_RABBITMQ_HOST | `rabbitmq.railway.internal` | expediente, notificacion |
| PRESUPUESTO_SERVICE_URL | `http://presupuesto-service.railway.internal:8082` | expediente-service |

#### **Lecciones Aprendidas en Railway**

1. **EUREKA_INSTANCE_HOSTNAME es obligatorio**: sin esta variable, los servicios registran su Docker container ID (ej: `1cd254d6997e`) en Eureka, que no es resoluble por otros servicios → `UnknownHostException`
2. **EUREKA_CLIENT_SERVICEURL_DEFAULTZONE** debe usar el private domain real del eureka-server, no el nombre del servicio. En Railway, el private domain puede diferir del service name
3. **CORS**: `allowCredentials=false` porque `allowedOriginPatterns=*` es incompatible con credentials
4. **Railway CLI v4.30.5**: es interactiva. Usar flags `--service`, `--variables`, `--image`, `-y`. Espaciar consultas para evitar rate limit Cloudflare Error 1015
5. **No usar ngrok**: el free tier muestra pagina interstitial que bloquea CORS preflight

### **8.3 Vercel — Frontend**

**URL:** `https://frontend-ivory-nine-43.vercel.app`

Configuracion en `frontend/vercel.json`:
```json
{
  "buildCommand": "CI=false react-scripts build",
  "outputDirectory": "build",
  "framework": "create-react-app",
  "build": {
    "env": {
      "REACT_APP_API_URL": "https://api-gateway-production-e01a.up.railway.app/api"
    }
  }
}
```

El frontend se despliega automaticamente en cada push a la rama `master` del repositorio GitHub. Tambien puede forzarse con `npx vercel deploy --prod --yes` desde el directorio `frontend/`.

### **8.4 Comandos de Despliegue Rapidos**

```bash
# Reconstruir y redesplegar frontend local
cd frontend && pnpm run build && cd ..
docker compose build nginx && docker compose up -d --force-recreate nginx

# Reconstruir api-gateway local
docker compose build api-gateway && docker compose up -d --force-recreate api-gateway

# Reconstruir expediente-service local
docker compose build expediente-service && docker compose up -d --force-recreate expediente-service

# Desplegar frontend en Vercel (forzar)
cd frontend && npx vercel deploy --prod --yes && cd ..

# Redesplegar servicio en Railway
railway service redeploy --service api-gateway -y

# Ver estado de Railway
railway service status --all

# Smoke test (21 endpoints)
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from tester
```

---

## **9. Seed Data**

El sistema incluye datos de prueba que se cargan automaticamente al iniciar cada servicio si la base de datos esta vacia:

| **Servicio** | **Cantidad** | **Detalle** |
|:-------------|:------------:|:------------|
| auth-service | 6 usuarios | 1 por cada rol: jefe@upla.edu.pe (Admin), coord@upla.edu.pe (Coordinacion), secretaria@upla.edu.pe (Secretaria), director@upla.edu.pe (Director), lab@upla.edu.pe (Laboratorio), decanato@upla.edu.pe (Decanato). Password: {rol}123 |
| presupuesto-service | 5 techos | Anos 2022-2026. Monto S/ 1,500,000 en 2026 (50% ejecutado) |
| presupuesto-service | 20 actividades POI | Distribuidas en los 5 techos, con fechas limite y saldos |
| presupuesto-service | 80 necesidades PAP | Bienes y servicios con precios unitarios, cantidades y clasificadores |
| presupuesto-service | 4 notas modificatorias | 2 pendientes, 1 configurada, 1 rechazada |
| expediente-service | 8 expedientes | En 7 estados distintos (Borrador, En revision, Aprobado, Rechazado, Finalizado, Observado, Derivado) con diferentes urgencias y naturalezas |
| expediente-service | 6 seguimiento logs | Registros de cambios de estado |

---

## **10. Seguridad**

### **10.1 Autenticacion JWT**

- **Algoritmo**: HMAC-SHA256 con clave secreta de 256 bits
- **Expiracion**: 24 horas (86400000 ms)
- **Claims**: `sub` (email), `rol` (RolUsuario), `iat` (emitido en), `exp` (expiracion)
- **Validacion**: el `JwtAuthFilter` en el API Gateway valida el token en cada peticion. Si es invalido o expiro, retorna 401
- **Rutas publicas**: `/api/auth/login`, `/api/health`, `/api/expedientes/rastreo/*`, `/api/status`, `/api/monitor/*`

### **10.2 Roles y Permisos**

6 roles con acceso diferenciado a modulos del sistema. La configuracion esta en `frontend/src/utils/config.js` y se aplica tanto en el frontend (Sidebar oculta modulos no autorizados) como en el backend (validacion de rol en endpoints sensibles).

### **10.3 CORS**

El API Gateway configura CORS para permitir requests desde cualquier origen (`allowedOriginPatterns=*`) con `allowCredentials=false`. Esto es necesario porque el frontend en Vercel y el backend en Railway estan en dominios diferentes.

### **10.4 Passwords**

Las contrasenas se almacenan con hash BCrypt. El seed inicial usa passwords en texto plano que son hasheadas por el `DataInitializer` antes de persistirlas.

---

## **11. Control de Versiones**

| **Elemento** | **Valor** |
|:-------------|:----------|
| Repositorio | https://github.com/LuchitoAE/Sisexp-Upla-SpringBoot |
| Rama principal | master |
| Estrategia de commits | Conventional commits descriptivos en espanol |
| CI/CD | Vercel auto-deploy en push a master. Railway auto-deploy via GitHub integration |
| Working Directory | `E:\proyecto\UPLA - Clases\octavo ciclo\arquitectura de software\semana 10\Proyecto-spring boot\` |

---

## **12. Verificacion y Testing**

### **12.1 Smoke Test Automatizado**

El archivo `docker-compose.test.yml` ejecuta un contenedor `tester` que verifica 21 endpoints del sistema:

```bash
docker compose -f docker-compose.yml -f docker-compose.test.yml up --exit-code-from tester
```

### **12.2 Health Checks**

Cada servicio expone endpoints de health check via Spring Boot Actuator:
- `/actuator/health`: estado general del servicio
- `/actuator/health/db`: conectividad con PostgreSQL
- `/actuator/health/rabbit` (expediente y notificacion): conectividad con RabbitMQ

Docker Compose usa estos health checks para determinar cuando un servicio esta listo (`depends_on` con `condition: service_healthy`).

### **12.3 Eureka Dashboard**

Accesible en `http://localhost:8761` (local) o via el private domain en Railway. Muestra todos los servicios registrados, su estado, y permite verificar que el service discovery funciona correctamente.

---

## **13. Documentacion Adicional**

| **Archivo** | **Contenido** |
|:------------|:--------------|
| `AGENTS.md` | Guia completa para AI agents con contexto, arquitectura, endpoints, comandos y lecciones aprendidas |
| `README.md` | Documentacion general del proyecto con setup rapido, endpoints, credenciales y deploy |
| `docker-compose.yml` | Definicion de los 12 contenedores con health checks y variables de entorno |
| `docker-compose.test.yml` | Smoke test automatizado de 21 endpoints |
| `frontend/vercel.json` | Configuracion de build y despliegue en Vercel |
| `microservicios/*/railway.toml` | Config-as-code para Railway (6 servicios) |
| `docs/INFORME_MICROSERVICIOS_SISEXP.md` | Informe previo de arquitectura |
