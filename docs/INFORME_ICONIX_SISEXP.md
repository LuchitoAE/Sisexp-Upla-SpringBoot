---
title: "SISEXP-UPLA — Documentacion ICONIX"
subtitle: "Universidad Peruana Los Andes — Analisis y Diseno de Microservicios"
author: "Arquitectura de Software — VIII Ciclo — Julio 2026"
lang: es
---

# **SISEXP-UPLA — Documentacion ICONIX**

---

## **1. Metodologia ICONIX**

ICONIX es una metodologia de desarrollo de software basada en casos de uso, que ocupa un punto intermedio entre la complejidad de RUP y la simplicidad de XP. Consta de 4 fases principales:

| **Fase** | **Artefacto** | **Representacion** |
|:---------|:--------------|:-------------------|
| Fase I — Analisis de Requisitos | Diagrama de Casos de Uso + Descripcion narrativa | Actores, casos de uso, pre/post condiciones |
| Fase II — Analisis Preliminar | Diagrama de Robustez | Boundary → Controller → Entity por cada caso de uso |
| Fase III — Diseno Detallado | Diagrama de Secuencia | Flujo completo a traves de capas controller → service → repository |
| Fase IV — Implementacion | Diagrama de Clases + Modelo de Dominio | Entidades, relaciones, enums, bounded contexts |

Cada fase se documenta a continuacion para el sistema SISEXP-UPLA.

---

## **2. Actores del Sistema**

| **Actor** | **Rol en SISEXP** | **Descripcion** |
|:----------|:------------------|:----------------|
| Administrador | Admin | Gestiona usuarios, techos, POI, PAP, notas, reportes. Acceso total al sistema. |
| Coordinacion | Coord | Aprueba/rechaza expedientes, gestiona presupuesto, dashboard, reportes. |
| Secretaria | Secretaria | Crea expedientes, sube documentos, finaliza y deriva. |
| Director | Director | Solicita expedientes, sube documentos, consulta reportes. |
| Laboratorio | Lab | Crea expedientes desde laboratorio, solicita notas modificatorias. |
| Decanato | Decanato | Solo consulta: dashboard, PAP, reportes, notificaciones. |
| Usuario Anonimo | — | Rastrea un expediente por su codigo (sin autenticacion). |
| Sistema (RabbitMQ) | — | Consume eventos y genera notificaciones automaticamente. |

---

## **3. Fase I — Diagrama de Casos de Uso**

### **3.1 Diagrama General**

```
                          ┌──────────────────────────┐
                          │     SISEXP-UPLA           │
                          │                          │
   Administrador ────────►│  Gestionar Usuarios       │
   Coordinacion ────────►│  Gestionar Techos          │
   Secretaria ──────────►│  Gestionar POI             │◄───── Sistema
   Director ────────────►│  Gestionar PAP             │       (RabbitMQ)
   Laboratorio ─────────►│  Crear Expediente          │
   Decanato ────────────►│  Cambiar Estado Expediente  │
                          │  Gestionar Notas           │
   Anonimo ─────────────►│  Rastrear Expediente        │
                          │  Consultar Dashboard       │
                          │  Generar Reportes          │
                          │  Exportar Reportes         │
                          │  Monitorear Sistema        │
                          │  Recibir Notificaciones    │
                          └──────────────────────────┘
```

### **3.2 Casos de Uso Detallados**

---

#### **CU-01: Gestionar Usuarios**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Administrador |
| **Precondicion** | Usuario autenticado con rol Administrador |
| **Flujo principal** | 1. Admin lista usuarios. 2. Crea/edita/elimina. 3. Toggle activo/inactivo. 4. Cambia password. |
| **Postcondicion** | Usuario creado, modificado o desactivado. |
| **Endpoint** | `GET/POST /api/usuarios`, `PUT /api/usuarios/{id}`, `PUT /api/usuarios/{id}/toggle-activo` |

---

#### **CU-02: Gestionar Techo Presupuestal**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Administrador, Coordinacion, Secretaria, Director |
| **Precondicion** | Usuario autenticado |
| **Flujo principal** | 1. Usuario ve lista de techos por ano. 2. Crea nuevo techo (ano + montoTotal). 3. Edita monto. 4. Toggle activo/inactivo. 5. Finaliza POI (planifica todas las actividades hijas). |
| **Postcondicion** | Techo creado/modificado. Si se finaliza POI, las actividades quedan bloqueadas. |
| **Endpoints** | `GET/POST/PUT /api/techos-presupuestales`, `POST /api/techos-presupuestales/{id}/finalizar-poi` |

---

#### **CU-03: Gestionar Actividades POI**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Administrador, Coordinacion, Secretaria, Director, Laboratorio |
| **Precondicion** | Techo presupuestal existente |
| **Flujo principal** | 1. Usuario selecciona un techo. 2. Crea/edita/elimina actividades. 3. Asigna presupuesto, fecha limite. 4. Finaliza PAP de la actividad. |
| **Postcondicion** | Actividad creada/modificada. Si se finaliza PAP, la actividad pasa a estado Cerrado. |
| **Endpoints** | `GET/POST /api/actividades-poi/techo/{id}`, `PUT/DELETE /api/actividades-poi/{id}`, `POST /api/actividades-poi/{id}/finalizar-pap` |

---

#### **CU-04: Gestionar Necesidades PAP**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Todos los roles |
| **Precondicion** | Actividad POI existente |
| **Flujo principal** | 1. Usuario selecciona una actividad. 2. Crea item PAP (nombre, cantidad, precio unitario, tipo Bien/Servicio, clasificador). 3. Edita item. 4. Elimina item. |
| **Postcondicion** | Item PAP creado/modificado con cantidades disponibles y montos calculados. |
| **Endpoints** | `GET/POST /api/necesidades-pap/actividad/{id}`, `PUT/DELETE /api/necesidades-pap/{id}` |

---

#### **CU-05: Crear Expediente** (Flujo Critico)

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Administrador, Coordinacion, Secretaria, Director, Laboratorio |
| **Precondicion** | Actividad POI con necesidades PAP existentes y saldo disponible |
| **Flujo principal** | 1. Usuario selecciona techo → actividad → item PAP. 2. El sistema consulta disponibilidad (saldo, fecha limite). 3. Usuario ingresa: cantidad, urgencia, naturaleza, descripcion. 4. Sistema valida saldo suficiente y fecha vigente. 5. Sistema genera codigo unico `EXP-YYYY-NNNNN`. 6. Expediente se crea en estado `Borrador`. 7. Se registra `SeguimientoLog`. 8. Se publica evento RabbitMQ. |
| **Flujo alternativo** | 3a. Saldo insuficiente → sistema muestra alerta y bloquea boton. 3b. Fecha vencida → sistema muestra advertencia y bloquea boton. |
| **Postcondicion** | Expediente creado con codigo unico, estado Borrador. |
| **Endpoint** | `POST /api/expedientes`, `GET /api/expedientes/disponibilidad/{poiId}/{papId}` |

---

#### **CU-06: Cambiar Estado de Expediente** (Maquina de Estados)

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Administrador, Coordinacion (aprobar/rechazar/observar), Secretaria (finalizar/derivar) |
| **Precondicion** | Expediente existente |
| **Flujo principal** | El sistema implementa la siguiente maquina de estados: |

```
  ┌──────────┐
  │ Borrador │──► Enviar a revision ──►┌────────────┐
  └──────────┘                        │En_revision │
                                      └──┬──┬──┬───┘
                           Aprobar ──────┘  │  └── Rechazar (terminal)
                                            │
                           Observar ────────┘
                              │
                              ▼
                      ┌──────────┐
                      │Observado │──► Reenviar a revision
                      └──────────┘
                      ┌──────────┐
                      │ Aprobado │──► Finalizar (terminal)
                      └──┬───────┘
                         └── Derivar ──►┌─────────┐
                                        │Derivado │──► Finalizar (terminal)
                                        └─────────┘
```

| **Postcondicion** | Estado actualizado, SeguimientoLog creado, evento RabbitMQ publicado, notificacion generada. |
| **Endpoints** | `PUT /api/expedientes/{id}/estado` |

**Tabla de Transiciones con Actores:**

| **De** | **A** | **Quien** | **Validacion** |
|:-------|:------|:----------|:---------------|
| Borrador | En_revision | Admin, Coord | — |
| En_revision | Aprobado | Admin, Coord | — |
| En_revision | Rechazado | Admin, Coord | Requiere observacion |
| En_revision | Observado | Admin, Coord | Requiere detalle |
| Observado | En_revision | Admin, Coord | — |
| Aprobado | Finalizado | Admin, Coord, Secretaria | — |
| Aprobado | Derivado | Admin, Coord, Secretaria | — |
| Derivado | Finalizado | Admin, Coord, Secretaria | — |

---

#### **CU-07: Gestionar Notas Modificatorias**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Laboratorio, Director (crear) / Administrador, Coordinacion (configurar/rechazar) |
| **Precondicion** | Usuario autenticado |
| **Flujo principal** | 1. Solicitante crea nota (tipo: inclusion_item o inclusion_actividad). 2. Admin revisa bandeja. 3a. Admin configura: asigna monto, clasificador, tipo → ejecuta creando item/actividad. 3b. Admin rechaza con observacion. |
| **Postcondicion** | Nota configurada (cambios aplicados al POI/PAP) o rechazada. |
| **Endpoints** | `GET/POST /api/notas-modificatorias`, `PUT /api/notas-modificatorias/{id}/configurar`, `PUT /api/notas-modificatorias/{id}/rechazar` |

---

#### **CU-08: Consultar Dashboard**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Todos los roles |
| **Precondicion** | Usuario autenticado |
| **Flujo principal** | 1. Sistema carga KPIs presupuestales. 2. Sistema calcula semaforo de alertas (rojo/amarillo/verde). 3. Sistema lista actividades proximas a vencer. 4. Sistema muestra expedientes sin movimiento +7 dias. 5. Sistema muestra saldos en tiempo real. |
| **Postcondicion** | Dashboard renderizado con datos actualizados. |
| **Endpoints** | `GET /api/dashboard/alertas`, `GET /api/dashboard/saldos` |

---

#### **CU-09: Generar y Exportar Reportes**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Administrador, Coordinacion, Director, Decanato |
| **Precondicion** | Usuario autenticado |
| **Flujo principal** | 1. Usuario selecciona seccion (Anual, Expedientes, POI, PAP) y ano. 2. Sistema carga reporte con datos normalizados. 3. Usuario exporta a Excel (.xlsx con branding UPLA) o PDF (HTML estilizado con marca de agua). |
| **Postcondicion** | Reporte visualizado y/o archivo descargado. |
| **Endpoints** | `GET /api/reportes/anual/{anio}`, `/excel`, `/pdf` (4 secciones x 2 formatos) |

---

#### **CU-10: Monitorear Sistema**

| **Campo** | **Descripcion** |
|:----------|:----------------|
| **Actor** | Todos los roles |
| **Precondicion** | Ninguna (endpoint publico) |
| **Flujo principal** | 1. Usuario accede al Monitor desde el Header. 2. Canvas muestra 12 nodos de infraestructura con estado UP/DOWN. 3. Activity feed en tiempo real con acciones de usuarios. 4. Panel de detalle al clickear nodo. |
| **Postcondicion** | Vista de monitoreo actualizada en tiempo real. |
| **Endpoint** | `GET /api/monitor/activity?since=5` |

---

## **4. Fase II — Diagramas de Robustez**

Los diagramas de robustez conectan los casos de uso con el modelo de dominio usando 3 estereotipos:
- **Boundary** (<<boundary>>): Componentes de interfaz (paginas React, controladores REST)
- **Controller** (<<controller>>): Logica de negocio (servicios Spring)
- **Entity** (<<entity>>): Objetos del dominio (modelos JPA)

### **4.1 Robustez — CU-05: Crear Expediente**

```
┌─────────────────────┐     ┌──────────────────────┐     ┌────────────────┐
│   <<boundary>>      │     │   <<controller>>      │     │  <<entity>>    │
│  ExpedientePage.js  │────►│ ExpedienteService.java│────►│  Expediente     │
│  (React Form)       │     │  + crear()            │     │  - codigo       │
│                     │     │  + generarNumero()    │     │  - estado       │
│  <<boundary>>       │     │  + obtenerConLogs()  │     │  - urgencia     │
│  ApiExpedienteCtrl  │     └──────────┬───────────┘     │  - costoEstimado│
│  (POST /expedientes)│                │                  └────────────────┘
└─────────────────────┘     ┌──────────┴───────────┐     ┌────────────────┐
                             │   <<controller>>      │     │  <<entity>>    │
                             │ Disponibilidad        │────►│ ActividadPOI   │
                             │ (validacion saldo)    │     │ - presupuesto  │
                             │                       │     │ - saldoEjecutado│
                             │ <<controller>>        │     └────────────────┘
                             │ SeguimientoLog        │     ┌────────────────┐
                             │ (registro historico)  │────►│SeguimientoLog  │
                             └───────────────────────┘     │ - estadoAnt    │
                                                           │ - estadoNuevo  │
                                                           └────────────────┘
```

### **4.2 Robustez — CU-06: Cambiar Estado**

```
┌─────────────────────┐     ┌──────────────────────┐     ┌────────────────┐
│   <<boundary>>      │     │   <<controller>>      │     │  <<entity>>    │
│  ExpedientePage.js  │────►│ ExpedienteService.java│────►│  Expediente     │
│  (Botones estado)   │     │  + actualizarEstado() │     │  + setEstado()  │
│                     │     │  + TRANSICIONES       │     │  + setObs()     │
│  <<boundary>>       │     │  + crearLog()         │     └────────────────┘
│  ApiExpedienteCtrl  │     └──────────┬───────────┘
│  (PUT /{id}/estado) │                │                  ┌────────────────┐
└─────────────────────┘                ├─────────────────►│SeguimientoLog  │
                                       │                  └────────────────┘
                                       │                  ┌────────────────┐
                                       └─────────────────►│  RabbitMQ      │
                                                          │  (evento)      │
                                                          └───────┬────────┘
                                                                  │ consume
                                                          ┌───────┴────────┐
                                                          │ Notificacion   │
                                                          │ (creada async) │
                                                          └────────────────┘
```

### **4.3 Robustez — CU-09: Exportar Reportes**

```
┌─────────────────────┐     ┌──────────────────────┐     ┌────────────────┐
│   <<boundary>>      │     │   <<controller>>      │     │  <<entity>>    │
│  ReportesPage.js    │────►│ ExportService.java    │────►│TechoPresupuestal│
│  (Boton Exportar)   │     │  + exportarExcelAnual │     └────────────────┘
│                     │     │  + exportarExcelPOI   │     ┌────────────────┐
│  <<boundary>>       │     │  + exportarPDF()      │────►│ ActividadPOI   │
│  ReportesController │     │  (Apache POI)         │     └────────────────┘
│  (GET /excel /pdf)  │     └───────────────────────┘     ┌────────────────┐
└─────────────────────┘                                   │ NecesidadPAP   │
                                                          └────────────────┘
```

---

## **5. Fase III — Diagramas de Secuencia**

### **5.1 Secuencia — CU-05: Crear Expediente**

```
Frontend        ApiGateway    ExpedienteCtrl   ExpedienteService   ExpedienteRepo   PresupuestoService   RabbitMQ
   │                │               │                 │                  │                  │               │
   │ POST /expedientes              │                 │                  │                  │               │
   │───────────────►│ JWT valid     │                 │                  │                  │               │
   │                │──────────────►│ crear(body)     │                  │                  │               │
   │                │               │────────────────►│                  │                  │               │
   │                │               │                 │ validar urgencia │                  │               │
   │                │               │                 │ con EnumUtils    │                  │               │
   │                │               │                 │ generarNumero()  │                  │               │
   │                │               │                 │ countByCodigo()  │                  │               │
   │                │               │                 │─────────────────►│                  │               │
   │                │               │                 │◄─────────────────│                  │               │
   │                │               │                 │ new Expediente() │                  │               │
   │                │               │                 │ save(exp)        │                  │               │
   │                │               │                 │─────────────────►│                  │               │
   │                │               │                 │◄─────────────────│ expediente saved │               │
   │                │               │                 │ crearLog()       │                  │               │
   │                │               │                 │─────────────────►│ seguimientoLog   │               │
   │                │               │                 │◄─────────────────│ saved            │               │
   │                │               │                 │ evento creado ───────────────────────────────────────────────►│
   │                │               │                 │                  │                  │               │
   │                │               │◄── 201 CREATED ─│                  │                  │               │
   │                │◄── 201 ───────│                 │                  │                  │               │
   │◄── 201 ────────│               │                 │                  │                  │               │
   │                │               │                 │                  │                  │               │
   │ invalidarCache │               │                 │                  │                  │               │
   │ reload lista   │               │                 │                  │                  │               │
```

### **5.2 Secuencia — CU-06: Aprobar Expediente (En_revision → Aprobado)**

```
Frontend        ApiGateway    ExpedienteCtrl   ExpedienteService   SeguimientoRepo   RabbitMQ   NotificacionService
   │                │               │                 │                  │               │              │
   │ PUT /exp/1/estado              │                 │                  │               │              │
   │ {estado:"Aprobado"}            │                 │                  │               │              │
   │───────────────►│──────────────►│ actualizarEstado(1,"Aprobado",obs,userId)           │              │
   │                │               │────────────────►│                  │               │              │
   │                │               │                 │ obtenerConLogs(1)│               │              │
   │                │               │                 │◄── exp ──────────│               │              │
   │                │               │                 │ EnumUtils.parseSafe("Aprobado")     │              │
   │                │               │                 │ TRANSICIONES[En_revision].contains(Aprobado)? ✓  │
   │                │               │                 │ exp.setEstado(Aprobado)              │              │
   │                │               │                 │ exp.setAprobadoPorId(userId)         │              │
   │                │               │                 │ save(exp) ────────►│               │              │
   │                │               │                 │◄── saved ─────────│               │              │
   │                │               │                 │ crearLog(saved, "En_revision", "Aprobado")        │
   │                │               │                 │──────────────────►│ log guardado   │              │
   │                │               │                 │◄─────────────────│               │              │
   │                │               │                 │ evento cambiado ──────────────────►│              │
   │                │               │                 │                  │               │ consume ────►│
   │                │               │                 │                  │               │ notif creada │
   │                │               │◄── 200 exp ─────│                  │               │              │
   │                │◄── 200 ───────│                 │                  │               │              │
   │◄── 200 ────────│               │                 │                  │               │              │
   │                │               │                 │                  │               │              │
   │ modals.alerta("Listo","Expediente paso a Aprobado")                                              │
   │ reload detalle │               │                 │                  │               │              │
```

### **5.3 Secuencia — CU-07: Configurar Nota Modificatoria**

```
Frontend          ApiGateway    NotaCtrl            NotaService        ActividadRepo     NecesidadRepo
   │                  │              │                    │                  │               │
   │ PUT /notas/1     │              │                    │                  │               │
   │ /configurar      │              │                    │                  │               │
   │ {monto:5000}     │              │                    │                  │               │
   │─────────────────►│─────────────►│ configurar(1,...)  │                  │               │
   │                  │              │───────────────────►│                  │               │
   │                  │              │                    │ findById(1)      │               │
   │                  │              │                    │ nota encontrada  │               │
   │                  │              │                    │ validar tipo     │               │
   │                  │              │                    │ (inclusion_item) │               │
   │                  │              │                    │ findById(actId)  │               │
   │                  │              │                    │─────────────────►│               │
   │                  │              │                    │◄── actividad ────│               │
   │                  │              │                    │ validar saldo    │               │
   │                  │              │                    │ (monto <= disp)  │               │
   │                  │              │                    │ crear nuevo PAP  │               │
   │                  │              │                    │──────────────────────────────────────►│
   │                  │              │                    │◄── PAP creado ───────────────────────│
   │                  │              │                    │ nota.estado = configurada              │
   │                  │              │                    │ save(nota)                          │
   │                  │              │◄── 200 OK ─────────│                  │               │
   │                  │◄── 200 ──────│                    │                  │               │
   │◄── 200 ──────────│              │                    │                  │               │
```

---

## **6. Fase IV — Diagrama de Clases (Modelo de Dominio)**

### **6.1 Modelo de Dominio Completo**

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Bounded Context: AUTENTICACION                  │
│                                                                       │
│  ┌──────────────┐                                                     │
│  │   Usuario    │                                                     │
│  ├──────────────┤                                                     │
│  │ - id: Long   │                                                     │
│  │ - nombre: Str│                                                     │
│  │ - email: Str │                                                     │
│  │ - password   │                                                     │
│  │ - rol: Rol   │                                                     │
│  │ - activo: Bool│                                                    │
│  │ - horarioRest│                                                     │
│  └──────────────┘                                                     │
│                                                                       │
│  <<enum>> RolUsuario                                                  │
│  Administrador, Coordinacion, Secretaria, Director, Laboratorio,      │
│  Decanato                                                             │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                      Bounded Context: PRESUPUESTO                     │
│                                                                       │
│  ┌─────────────────┐    1..*     ┌─────────────────┐                  │
│  │TechoPresupuestal│◇───────────│  ActividadPOI   │                  │
│  ├─────────────────┤             ├─────────────────┤                  │
│  │ - id: Long      │             │ - id: Long      │                  │
│  │ - ano: Integer  │             │ - codigo: Str   │                  │
│  │ - montoTotal:   │             │ - nombre: Str   │                  │
│  │   BigDecimal    │             │ - presupuesto   │                  │
│  │ - montoUtilizado│             │   Asignado: Big │                  │
│  │ - planificado   │             │ - saldoEjecutado│                  │
│  │ - activo: Bool  │             │ - saldoComprom  │                  │
│  └─────────────────┘             │ - fechaLimite   │                  │
│                                  │ - estado: Estado│                  │
│  ┌──────────────────┐            │   Actividad     │                  │
│  │ NotaModificatoria│            │ - planificado   │                  │
│  ├──────────────────┤            └──────┬──────────┘                  │
│  │ - id: Long       │                   │ 1..*                       │
│  │ - codigo: Str    │                   │                            │
│  │ - tipo: TipoNota │                   ◇                            │
│  │ - estado:EstadoN │            ┌─────────────────┐                  │
│  │ - nuevoNombre    │            │  NecesidadPAP   │                  │
│  │ - justificacion  │            ├─────────────────┤                  │
│  │ - montoTransferir│            │ - id: Long      │                  │
│  │ - nuevoTipo: Nat │            │ - nombre: Str   │                  │
│  │ - actividadExist │            │ - cantidad: Int │                  │
│  │   enteId: Long   │            │ - precioEstimado│                  │
│  │ - actividadOrigen│            │ - tipo:Naturalez│                  │
│  │   Id: Long       │            │ - clasificador  │                  │
│  │ - archivoPdf     │            │   Gasto: Str    │                  │
│  │ - observacionAdm │            │ - cantDisponible│                  │
│  │ - createdAt      │            │ - cantEjecutada │                  │
│  │ - updatedAt      │            │ - montoDisponibl│                  │
│  └──────────────────┘            │ - montoEjecutado│                  │
│                                  │ - actividadPOIId│                  │
│  <<enum>> TipoNota               │ - oficinaLab:Str│                  │
│  inclusion_item,                 │ - unidad: Str   │                  │
│  inclusion_actividad             └─────────────────┘                  │
│                                                                       │
│  <<enum>> EstadoNota             <<enum>> EstadoActividad             │
│  pendiente, configurada,         Pendiente, En_Ejecucion,             │
│  rechazada                       Cerrado                              │
│                                                                       │
│  <<enum>> Naturaleza                                                  │
│  Bien, Servicio                                                       │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                     Bounded Context: EXPEDIENTES                      │
│                                                                       │
│  ┌─────────────────┐    1..*     ┌─────────────────┐                  │
│  │   Expediente    │◇───────────│ DocumentoAdjunto│                  │
│  ├─────────────────┤             ├─────────────────┤                  │
│  │ - id: Long      │             │ - id: Long      │                  │
│  │ - codigo: Str   │             │ - tipo: TipoDoc │                  │
│  │   (UNIQUE)      │             │ - nombreOriginal│                  │
│  │ - actividadPOIId│             │ - nombreArchivo │                  │
│  │ - necesidadPAPId│             │ - mimeType: Str │                  │
│  │ - solicitanteId │             │ - tamano: Long  │                  │
│  │ - urgencia: Urg │             └─────────────────┘                  │
│  │ - naturaleza: Nat│                                                 │
│  │ - descripcion   │    1..*     ┌─────────────────┐                  │
│  │ - estado:Estado │◇───────────│ SeguimientoLog  │                  │
│  │   Expediente    │             ├─────────────────┤                  │
│  │ - cantidadSolic │             │ - id: Long      │                  │
│  │ - costoEstimado │             │ - estadoAnterior│                  │
│  │ - observacion   │             │ - estadoNuevo   │                  │
│  │ - aprobadoPorId │             │ - usuarioId     │                  │
│  └─────────────────┘             │ - observacion   │                  │
│                                  │ - createdAt     │                  │
│  <<enum>> Urgencia               └─────────────────┘                  │
│  Urgente, No_tan_urgente,                                            │
│  Puede_esperar                    <<enum>> TipoDocumento              │
│                                   TDR, Especificaciones               │
│  <<enum>> EstadoExpediente        Tecnicas, Cotizacion,               │
│  Borrador, En_revision,           Informe_Tecnico                    │
│  Aprobado, Rechazado,                                                 │
│  Finalizado, Observado,                                               │
│  Derivado                                                             │
└──────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────┐
│                   Bounded Context: NOTIFICACIONES                     │
│                                                                       │
│  ┌─────────────────┐                                                  │
│  │  Notificacion   │                                                  │
│  ├─────────────────┤                                                  │
│  │ - id: Long      │                                                  │
│  │ - usuarioId:Long│                                                  │
│  │ - mensaje: Str  │                                                  │
│  │ - tipo: TipoNot │                                                  │
│  │ - leida: Bool   │                                                  │
│  │ - expedienteId  │                                                  │
│  │ - createdAt     │                                                  │
│  └─────────────────┘                                                  │
│                                                                       │
│  <<enum>> TipoNotificacion                                            │
│  observacion, rechazo, aprobacion, alerta_fecha,                      │
│  nota_aprobada, nota_rechazada, info                                 │
└──────────────────────────────────────────────────────────────────────┘
```

### **6.2 Diagrama de Componentes (Arquitectura de Microservicios)**

```
                            ┌──────────────┐
                            │   NGINX :80  │
                            │  (React SPA) │
                            └──────┬───────┘
                                   │ /api/*
                                   ▼
                         ┌─────────────────┐
                         │  API-GATEWAY    │
                         │  :8080          │
                         │  JWT + CORS     │
                         │  ActivityLog    │
                         │  MonitorCtrl    │
                         └──┬──┬──┬──┬────┘
                            │  │  │  │
                   ┌────────┘  │  │  └────────┐
                   ▼           ▼  ▼           ▼
            ┌──────────┐ ┌──────────┐ ┌──────────────┐
            │AUTH-SVC  │ │PRESUP-SVC│ │EXPEDIENTE-SVC│
            │:8081     │ │:8082     │ │:8083         │
            │          │ │          │ │              │
            │ExportSvc │ │          │ │              │
            └───┬──────┘ └───┬──────┘ └──┬───┬───────┘
                │            │           │   │
                ▼            ▼           ▼   │
         ┌──────────┐ ┌──────────┐ ┌──────┐  │
         │ auth-db  │ │presup-   │ │exp-  │  │
         │ :5433    │ │uesto-db  │ │db    │  │
         │          │ │:5434     │ │:5435 │  │
         └──────────┘ └──────────┘ └──────┘  │
                                             │ RABBITMQ
                    ┌──────────────┐          │ :5672
                    │NOTIFICACION- │◄─────────┘
                    │SVC :8084     │
                    └──────┬───────┘
                           ▼
                    ┌──────────────┐
                    │ notificacion │
                    │ -db :5436    │
                    └──────────────┘

        ┌──────────┐
        │  EUREKA  │  (Service Discovery)
        │  :8761   │
        └──────────┘
```

### **6.3 Arquitectura en Capas por Microservicio**

Cada microservicio implementa la siguiente estructura de capas:

```
┌────────────────────────────────────────────────────────────────┐
│                    <<boundary>> CONTROLLER                      │
│  ApiExpedienteController, ReportesController, etc.             │
│  @RestController @RequestMapping                               │
│  Responsabilidad: Recibir HTTP, validar input, retornar JSON   │
├────────────────────────────────────────────────────────────────┤
│                    <<controller>> SERVICE                       │
│  ExpedienteService, ActividadPOIService, ExportService          │
│  @Service @Transactional                                       │
│  Responsabilidad: Logica de negocio, reglas, validacion        │
├────────────────────────────────────────────────────────────────┤
│                      <<repository>> REPOSITORY                  │
│  ExpedienteRepository, TechoPresupuestalRepository              │
│  extends JpaRepository                                         │
│  Responsabilidad: Acceso a datos, queries personalizados       │
├────────────────────────────────────────────────────────────────┤
│                       <<entity>> MODEL                          │
│  Expediente, ActividadPOI, Usuario, etc.                        │
│  @Entity @Table                                                │
│  Responsabilidad: Mapeo objeto-relacional, validacion          │
├────────────────────────────────────────────────────────────────┤
│                       <<config>> CONFIG                         │
│  DataInitializer, GlobalExceptionHandler, RestTemplateConfig    │
│  @Configuration @Component                                      │
│  Responsabilidad: Beans, filtros, seeds, CORS                  │
└────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌────────────────────────────────────────────────────────────────┐
│                     sisexp-common (JAR compartido)              │
│  enums/  │  dto/  │  exception/  │  util/                      │
│  10 enums│  DTOs  │ BusinessExcp │ EnumUtils                   │
└────────────────────────────────────────────────────────────────┘
```

---

## **7. Validacion ICONIX — Trazabilidad**

ICONIX exige que cada paso del diseno tenga trazabilidad con el caso de uso que lo origina. La siguiente tabla muestra esta trazabilidad:

| **Caso de Uso** | **Robustez** | **Secuencia** | **Clases Participantes** | **Endpoints** |
|:----------------|:------------|:-------------|:-------------------------|:--------------|
| CU-01 Gestionar Usuarios | Sec. 4 | Sec. 5 | UsuarioService, UsuarioRepository, Usuario | /api/usuarios/** |
| CU-02 Gestionar Techo | Sec. 4 | Sec. 5 | TechoPresupuestalService, ActividadPOIService | /api/techos-presupuestales/** |
| CU-03 Gestionar POI | Sec. 4 | Sec. 5 | ActividadPOIService, ActividadPOI | /api/actividades-poi/** |
| CU-04 Gestionar PAP | Sec. 4 | Sec. 5 | NecesidadPAPService, NecesidadPAP | /api/necesidades-pap/** |
| **CU-05 Crear Expediente** | **Sec. 4.1** | **Sec. 5.1** | **ExpedienteService, EnumUtils, RabbitTemplate** | **POST /api/expedientes** |
| **CU-06 Cambiar Estado** | **Sec. 4.2** | **Sec. 5.2** | **ExpedienteService, TRANSICIONES, SeguimientoLog** | **PUT /api/expedientes/{id}/estado** |
| **CU-07 Gestionar Notas** | **Sec. 4** | **Sec. 5.3** | **NotaModificatoriaService, ActividadPOI** | **/api/notas-modificatorias/** |
| CU-08 Dashboard | Sec. 4 | Sec. 5 | DashboardController, alertas(), saldos() | /api/dashboard/** |
| CU-09 Exportar Reportes | Sec. 4.3 | Sec. 5.3 | ExportService (Apache POI), ReportesController | /api/reportes/**/excel, /pdf |
| CU-10 Monitorear | Sec. 4 | Sec. 5 | ActivityLogFilter, MonitorController | /api/monitor/** |

---

## **8. Resumen de Artefactos ICONIX**

| **Artefacto** | **Cantidad** | **Ubicacion en este documento** |
|:--------------|:------------:|:--------------------------------|
| Actores identificados | 8 | Seccion 2 |
| Casos de Uso documentados | 10 | Seccion 3 |
| Diagramas de Robustez | 3 detallados + 7 conceptuales | Seccion 4 |
| Diagramas de Secuencia | 3 detallados | Seccion 5 |
| Entidades del dominio | 9 | Seccion 6 |
| Enums del dominio | 10 | Seccion 6.1 |
| Bounded Contexts | 4 | Seccion 6 |
| Endpoints API | 50+ | Seccion 3 (por caso de uso) |
| Capas por microservicio | 5 (controller, service, repository, model, config) | Seccion 6.3 |
| Tabla de trazabilidad | 10 casos de uso → clases → endpoints | Seccion 7 |

---

<div style="text-align: center; margin-top: 40px; padding: 20px; border-top: 3px solid #1e3a5f;">

**SISEXP-UPLA** — Documentacion ICONIX

Universidad Peruana Los Andes — Arquitectura de Software — VIII Ciclo

Julio 2026

</div>
