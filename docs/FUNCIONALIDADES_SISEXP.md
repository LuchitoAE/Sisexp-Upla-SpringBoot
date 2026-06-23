# SISEXP-UPLA — Manual de Funcionalidades

**Sistema de Seguimiento y Control de Expedientes**

**Universidad Peruana Los Andes — Facultad de Ingeniería**

**Versión 1.0.0 — Junio 2026**

---

## Índice

1. [Visión General](#1-visión-general)
2. [Autenticación y Seguridad](#2-autenticación-y-seguridad)
3. [Dashboard](#3-dashboard)
4. [Gestión de Expedientes](#4-gestión-de-expedientes)
5. [Ciclo Presupuestal](#5-ciclo-presupuestal)
6. [Documentos Adjuntos](#6-documentos-adjuntos)
7. [Notificaciones](#7-notificaciones)
8. [Reportes](#8-reportes)
9. [Rastreo Público](#9-rastreo-público)
10. [Notas Modificatorias](#10-notas-modificatorias)
11. [Administración de Usuarios](#11-administración-de-usuarios)
12. [Horario Laboral](#12-horario-laboral)
13. [Stack Tecnológico](#13-stack-tecnológico)
14. [Credenciales de Prueba](#14-credenciales-de-prueba)

---

## 1. Visión General

SISEXP-UPLA es un sistema web que automatiza la gestión presupuestal de expedientes administrativos de la Facultad de Ingeniería de la Universidad Peruana Los Andes. Implementa el ciclo completo desde la asignación del presupuesto anual hasta la ejecución final de cada gasto, con control de saldos en tiempo real y 7 estados de flujo de trabajo.

**Ciclo presupuestal completo:**

```
Techo Presupuestal → Actividades POI → Necesidades PAP → Expedientes
```

**Entidades del sistema (10):**

| Entidad | Propósito |
|---|---|
| Techo Presupuestal | Presupuesto anual asignado a la facultad |
| Actividad POI | Distribución del presupuesto en actividades del Plan Operativo |
| Necesidad PAP | Ítems específicos a adquirir (bienes/servicios) dentro de cada actividad |
| Expediente | Solicitud formal de gasto con flujo de aprobación de 7 estados |
| Documento Adjunto | Archivos PDF de sustento (TDR, cotizaciones, especificaciones) |
| Seguimiento Log | Historial completo de cambios de estado del expediente |
| Nota Modificatoria | Solicitud de redistribución presupuestal entre actividades |
| Notificación | Alertas automáticas al usuario sobre cambios en sus expedientes |
| Usuario | Cuenta de acceso con rol, permisos y restricción horaria |
| Rol | Catálogo de roles del sistema |

---

## 2. Autenticación y Seguridad

### 2.1 Login

El ingreso al sistema se realiza mediante correo electrónico y contraseña. La página de login incluye:

- **Formulario de acceso:** email + contraseña + checkbox "Recordarme (30 días)"
- **Mensajes contextuales:** error de credenciales, cuenta bloqueada, sesión expirada, fuera de horario, cierre de sesión exitoso
- **Botones de acceso rápido:** 6 botones demo que autocompletan las credenciales de cada rol (Admin, Coordinación, Secretaria, Director, Laboratorio, Decanato)
- **Enlace a rastreo público:** acceso directo a la consulta de expedientes sin autenticación

### 2.2 Remember Me (30 días)

Al marcar "Recordarme", el sistema almacena una cookie segura con validez de 30 días (2,592,000 segundos). Durante ese período, el usuario no necesita volver a autenticarse. La cookie se invalida al cerrar sesión explícitamente.

### 2.3 Control de Intentos Fallidos (Anti-Brute Force)

- **5 intentos fallidos** → bloqueo automático de la cuenta por 30 minutos
- El campo `intentosFallidos` se incrementa con cada login fallido
- El campo `bloqueadoHasta` almacena la fecha/hora de desbloqueo
- Al expirar el bloqueo, el contador se reinicia automáticamente a 0
- El Administrador puede ver y gestionar estos bloqueos desde el panel de usuarios

### 2.4 RBAC — Control de Acceso Basado en Roles

**6 roles con permisos granulares por acción:**

| Rol | Crear Exp. | Aprobar/Observar | Rechazar | Finalizar | Derivar | Ver Reportes | Gestionar Usuarios |
|---|---|---|---|---|---|---|---|
| Administrador | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Coordinación | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| Secretaria | ✓ | ✗ | ✗ | ✓ | ✓ | ✗ | ✗ |
| Director | ✓ | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |
| Laboratorio | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Decanato | ✗ | ✗ | ✗ | ✗ | ✗ | ✓ | ✗ |

**Visibilidad de módulos en la barra lateral por rol:**

| Módulo | Admin | Coord | Sec | Dir | Lab | Dec |
|---|---|---|---|---|---|---|
| Dashboard | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Expedientes | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| Techo Presupuestal | ✓ | ✓ | ✓ | ✓ | ✗ | ✗ |
| Actividades POI | ✓ | ✓ | ✓ | ✓ | ✓ | ✗ |
| Necesidades PAP | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Notas Modificatorias | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Reportes | ✓ | ✓ | ✗ | ✓ | ✗ | ✓ |
| Usuarios | ✓ | ✗ | ✗ | ✗ | ✗ | ✗ |
| Notificaciones | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| Rastreo | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |

### 2.5 Límites de Monto por Rol

Para controlar el gasto, cada rol tiene un límite máximo por expediente:

| Rol | Límite por expediente |
|---|---|
| Administrador | Ilimitado |
| Coordinación | Ilimitado |
| Director | S/ 15,000 |
| Secretaria | S/ 5,000 |
| Laboratorio | S/ 5,000 |
| Decanato | S/ 0 (solo consulta) |

El sistema valida automáticamente este límite al crear un expediente. Si el costo estimado excede el límite del rol, se rechaza la creación con un mensaje explicativo.

---

## 3. Dashboard

El dashboard es la página principal post-login. Proporciona una vista panorámica del estado del sistema en tiempo real.

### 3.1 Tarjetas KPI (Indicadores Clave)

Se muestran 6 tarjetas con indicadores:

| KPI | Descripción | Color |
|---|---|---|
| Total | Número total de expedientes en el sistema | Azul |
| Borrador | Expedientes en creación, aún no enviados | Gris |
| En revisión | Expedientes siendo evaluados por Coordinación | Amarillo |
| Aprobado | Expedientes con gasto autorizado | Verde |
| Finalizado | Expedientes completados y cerrados | Cyan |
| Vencidos | Expedientes con fecha límite pasada (no finalizados ni rechazados) | Rojo |

### 3.2 Barras de Ejecución Presupuestal

Por cada Actividad POI con presupuesto asignado, se muestra una barra de progreso de tres colores:

- **Verde (ejecutado):** monto ya gastado en expedientes aprobados/finalizados
- **Naranja (comprometido):** monto reservado para expedientes en revisión
- **Gris (disponible):** saldo libre para nuevos expedientes

Cada barra incluye leyenda con los montos exactos en soles (S/).

Fórmula: `Disponible = Presupuesto Asignado − Saldo Comprometido − Saldo Ejecutado`

### 3.3 Alertas

Lista de expedientes que requieren atención:

- **Ícono rojo (⚠):** expedientes vencidos (fecha límite < hoy)
- **Ícono amarillo (🕐):** expedientes próximos a vencer (fecha límite en los próximos 7 días)
- Se excluyen automáticamente los expedientes en estado Finalizado o Rechazado

Cada alerta muestra: código del expediente (enlace clickable), nombre del solicitante, descripción, fecha límite y badge del estado actual.

---

## 4. Gestión de Expedientes

Módulo central del sistema. Implementa el CRUD completo con flujo de trabajo de 7 estados.

### 4.1 Listado de Expedientes

Tabla paginada con todas las solicitudes. Columnas:

| Columna | Contenido |
|---|---|
| Código | Formato EXP-YYYY-NNNN (clickable → detalle) |
| Actividad POI | Código y nombre de la actividad asociada |
| Ítem PAP | Nombre del bien o servicio solicitado |
| Solicitante | Nombre del usuario que creó el expediente |
| Urgencia | Badge de color: Urgente (rojo), No tan urgente (amarillo), Puede esperar (gris) |
| Estado | Badge de color específico por estado |
| Fecha Límite | Fecha máxima para resolver el expediente |
| Costo | Monto estimado en soles (S/) |

**Filtros disponibles:**
- Búsqueda por código de expediente (filtro de texto en tiempo real)
- Filtro por estado (selector desplegable con los 7 estados)

### 4.2 Badges de Color por Estado

| Estado | Color de Badge | Significado |
|---|---|---|
| Borrador | Gris (#e2e8f0) | El expediente está en creación, editable |
| En revisión | Amarillo (#fef3c7) | Enviado a Coordinación para evaluación |
| Aprobado | Verde (#d1fae5) | Gasto autorizado, en ejecución |
| Rechazado | Rojo (#fee2e2) | Denegado, estado terminal |
| Finalizado | Azul (#dbeafe) | Completado y cerrado |
| Observado | Rosa (#fce7f3) | Tiene observaciones, requiere subsanación |
| Derivado | Púrpura (#ede9fe) | Redirigido a otra instancia |

### 4.3 Creación de Expediente

El formulario de creación incluye:

1. **Selector de Actividad POI:** carga inicial con todas las actividades activas. Muestra código + nombre.
2. **Selector de Necesidad PAP (carga dinámica AJAX):** al seleccionar una actividad, el sistema consulta vía AJAX las necesidades PAP de esa actividad y las carga en el segundo selector. Muestra: nombre del ítem + precio unitario + cantidad disponible.
3. **Cálculo automático de costo:** al seleccionar una necesidad PAP, el costo estimado se calcula automáticamente como `cantidad solicitada × precio unitario del PAP`.
4. **Auto-completado de naturaleza:** si la necesidad PAP es de tipo "Bien" o "Servicio", el campo naturaleza se autocompleta.
5. **Selector de urgencia:** Urgente / No tan urgente / Puede esperar.
6. **Cantidad solicitada:** número entero, mínimo 1, máximo 9999.
7. **Fecha límite sugerida:** campo de fecha opcional.
8. **Descripción:** campo de texto libre (mínimo 10 caracteres, máximo 2000).

**Validaciones al crear:**

| Validación | Regla |
|---|---|
| Actividad activa | No se puede crear expedientes en actividades cerradas o planificadas |
| Fecha límite de actividad | Si la actividad tiene fecha límite vencida, se rechaza |
| Saldo disponible | El costo estimado no puede exceder el saldo disponible de la actividad |
| Límite por rol | El costo no puede exceder el límite de monto del rol del solicitante |
| Tope 80% | Un solo expediente no puede consumir más del 80% del saldo disponible |
| Correspondencia de tipo | El tipo (Bien/Servicio) debe coincidir con el tipo del ítem PAP |
| Período fiscal | No se pueden crear expedientes en techos de años cerrados |

### 4.4 Generación de Código de Expediente

El código se genera automáticamente con el formato:

```
EXP-YYYY-NNNN
```

Donde:
- `YYYY` = año actual
- `NNNN` = secuencial de 4 dígitos que se incrementa automáticamente

Ejemplos: `EXP-2026-0001`, `EXP-2026-0002`, etc.

La lógica consulta el último código del año en la base de datos, extrae el secuencial, lo incrementa y lo formatea con 4 dígitos.

### 4.5 Detalle del Expediente

Página de vista detallada con 5 secciones:

**Sección 1 — Datos del expediente:**
Tabla con todos los campos: código, actividad POI, ítem PAP, solicitante, urgencia, naturaleza, cantidad solicitada, costo estimado, fecha límite, aprobado por, observaciones.

**Sección 2 — Panel de cambio de estado:**
Visible solo para roles autorizados (Admin, Coordinación, Secretaria). Contiene:
- Selector desplegable con el nuevo estado
- Campo de observación (motivo del cambio)
- Botón "Actualizar"

Validaciones de transición:

| Estado Actual | Estados Permitidos |
|---|---|
| Borrador | → En revisión |
| En revisión | → Aprobado, Rechazado, Observado |
| Observado | → En revisión (subsanar) |
| Aprobado | → Finalizado, Derivado |
| Derivado | → Finalizado |

Estados terminales: Rechazado, Finalizado (no admiten más cambios).

**Sección 3 — Timeline de seguimiento:**
Línea de tiempo vertical con el historial completo de cambios. Cada entrada muestra:
- Fecha y hora del cambio
- Estado nuevo (en negrita)
- Transición (ej: "De Borrador → En revisión")
- Nombre del usuario que realizó el cambio
- Observación (si existe)

Estilo visual: círculos de colores conectados por línea vertical.

**Sección 4 — Documentos adjuntos:**
Tabla con los documentos del expediente:
- Tipo (badge): TDR, Especificaciones Técnicas, Cotización, Informe Técnico
- Nombre original del archivo
- Tamaño en KB
- Botón eliminar (con confirmación)

Botón "Subir documento" que abre un modal con:
- Selector de tipo de documento
- Input de archivo (solo PDF, máximo 15 MB)

**Sección 5 — Datos del presupuesto asociado:**
Información de la actividad POI y necesidad PAP vinculadas, con montos y saldos actualizados.

### 4.6 Cambio de Estado con Reglas de Negocio

Cada cambio de estado ejecuta automáticamente reglas presupuestales:

| Transición | Acción en POI | Acción en PAP |
|---|---|---|
| Borrador → En revisión | `saldoComprometido += costo` | `cantidadDisponible -= cantidad`, `montoDisponible -= costo` |
| En revisión → Aprobado | `saldoEjecutado += costo`, `saldoComprometido -= costo` | `cantidadEjecutada += cantidad`, `montoEjecutado += costo` |
| En revisión → Rechazado | `saldoComprometido -= costo` (libera) | `cantidadDisponible += cantidad`, `montoDisponible += costo` |
| En revisión → Observado | `saldoComprometido -= costo` (libera) | `cantidadDisponible += cantidad`, `montoDisponible += costo` |
| Observado → En revisión | `saldoComprometido += costo` (vuelve a reservar) | `cantidadDisponible -= cantidad`, `montoDisponible -= costo` |
| Aprobado → Finalizado | Sin cambio (ya está ejecutado) | Sin cambio |
| Aprobado → Derivado | Sin cambio | Sin cambio |

---

## 5. Ciclo Presupuestal

### 5.1 Techo Presupuestal

Representa el presupuesto anual total asignado a la facultad.

**Campos:**
- Año (único, ej: 2026)
- Monto total asignado (S/)
- Monto utilizado (calculado automáticamente como suma de saldos ejecutados de actividades)
- Estado: Activo / Inactivo
- Planificación: Abierto (permite modificaciones) / Cerrado (no permite cambios)

**Funcionalidades:**
- Vista de cards por año con barra de progreso de utilización
- Modal para crear/editar (solo Admin y Coordinación)
- Toggle activo/inactivo desde menú contextual
- Cerrar planificación (bloquea modificaciones cuando el techo está planificado)

### 5.2 Actividades POI

Distribuyen el presupuesto del techo en actividades específicas del Plan Operativo Institucional.

**Campos:**
- Código (ej: POI-2.01)
- Nombre descriptivo
- Presupuesto asignado
- Saldo comprometido (reservado para expedientes en revisión)
- Saldo ejecutado (ya gastado en expedientes aprobados)
- Fecha límite
- Estado: Pendiente / En Ejecución / Ejecutado / Cerrado

**Fórmula de disponible real:**

```
Disponible = Presupuesto Asignado − Saldo Comprometido − Saldo Ejecutado
```

### 5.3 Necesidades PAP

Ítems específicos del Plan Anual de Contrataciones vinculados a cada actividad POI.

**Campos:**
- Nombre del ítem
- Cantidad planificada total
- Precio unitario estimado
- Unidad de medida (ej: unidad, licencia, global, lote)
- Oficina o laboratorio de destino
- Tipo: Bien o Servicio
- Clasificador de gasto (ej: 2.3.1.2.1.1)
- Cantidad disponible (para nuevos expedientes)
- Monto disponible
- Cantidad ejecutada (ya consumida)
- Monto ejecutado

---

## 6. Documentos Adjuntos

### 6.1 Tipos de Documento

| Tipo | Uso |
|---|---|
| TDR | Términos de Referencia |
| Especificaciones Técnicas | Detalle técnico del bien o servicio |
| Cotización | Propuesta económica de proveedor |
| Informe Técnico | Sustento técnico de la necesidad |

### 6.2 Funcionalidades

- **Subida:** modal con selector de tipo + input file (solo PDF, máximo 15 MB)
- **Almacenamiento:** nombre original preservado, nombre en disco con UUID para evitar colisiones
- **Eliminación:** con confirmación previa (solo Admin puede eliminar documentos de cualquier expediente)
- **Validación:** se requiere al menos 1 documento PDF para enviar un expediente a revisión

---

## 7. Notificaciones

### 7.1 Notificaciones Automáticas

Al cambiar el estado de un expediente, el sistema genera automáticamente una notificación para el solicitante:

| Cambio de Estado | Tipo de Notificación | Mensaje de Ejemplo |
|---|---|---|
| → Aprobado | `aprobacion` | "Su expediente EXP-2026-0001 ha sido aprobado" |
| → Rechazado | `rechazo` | "Su expediente EXP-2026-0002 ha sido rechazado. Motivo: presupuesto insuficiente" |
| → Observado | `observacion` | "Su expediente EXP-2026-0005 tiene observaciones: Falta ficha técnica" |

### 7.2 Tipos de Notificación

| Tipo | Color de Badge | Contexto |
|---|---|---|
| `aprobacion` | Verde | Expediente o nota aprobada |
| `rechazo` | Rojo | Expediente o nota rechazada |
| `observacion` | Amarillo | Expediente con observaciones |
| `alerta_fecha` | Cyan | Fecha límite próxima a vencer |
| `info` | Gris | Información general |
| `nota_aprobada` | Verde | Nota modificatoria configurada |
| `nota_rechazada` | Rojo | Nota modificatoria rechazada |

### 7.3 Badge en Cabecera

- El ícono de campana 🔔 en la barra superior muestra un badge rojo con el número de notificaciones no leídas
- Se actualiza automáticamente cada 60 segundos vía AJAX (`GET /api/notificaciones/count`)
- Al hacer clic, navega a la página de notificaciones

### 7.4 Página de Notificaciones

- Lista de todas las notificaciones del usuario, ordenadas por fecha (más recientes primero)
- Filas resaltadas para notificaciones no leídas
- Cada notificación muestra: mensaje, tipo (badge de color), expediente relacionado (enlace clickable), fecha
- Botón "Marcar como leída" individual
- Botón "Marcar todas como leídas"

---

## 8. Reportes

Módulo accesible para Admin, Coordinación, Director y Decanato. Organizado en 4 pestañas (tabs de Bootstrap):

### 8.1 Tab: Expedientes

- **Tarjetas KPI:** total + conteo por cada uno de los 7 estados + vencidos
- **Gráfico de barras (Chart.js):** distribución de expedientes por estado, con colores diferenciados
- **Tabla detalle:** código, actividad, ítem, solicitante, estado, costo, fecha límite
- **Exportar CSV:** botón que descarga `expedientes.csv` con todos los registros

### 8.2 Tab: POI (Plan Operativo Institucional)

- **Tabla:** código, nombre, año del techo, presupuesto, ejecutado, % de ejecución (barra de progreso), estado
- **Exportar CSV:** `poi.csv`

### 8.3 Tab: PAP (Plan Anual de Contrataciones)

- **Tabla:** nombre del ítem, actividad asociada, cantidad planificada, precio unitario, tipo (Bien/Servicio), cantidades disponible y ejecutada, monto disponible
- **Exportar CSV:** `pap.csv`

### 8.4 Tab: Informe Anual

- **Cards comparativas por año:** cada techo presupuestal muestra su año, monto total, monto utilizado y barra de progreso con porcentaje
- Permite comparar visualmente la ejecución entre diferentes años fiscales

---

## 9. Rastreo Público

Página de consulta pública que no requiere autenticación. Accesible desde `http://localhost:8080/rastreo`.

### 9.1 Funcionalidad

- **Campo de búsqueda:** ingreso del código de expediente (formato EXP-YYYY-NNNN)
- **Resultado exitoso:** muestra código, estado (badge de color), actividad POI, ítem PAP, urgencia, fecha límite y última actualización
- **Resultado no encontrado:** mensaje "No se encontró ningún expediente con el código XXX"
- **Observaciones:** si el expediente tiene observaciones, se muestran en un recuadro amarillo destacado
- **Enlace a login:** botón para volver al sistema

### 9.2 Seguridad

- La ruta `/rastreo/**` está excluida de autenticación en Spring Security
- No expone información sensible (no muestra montos exactos ni datos del solicitante)
- Diseño independiente con fondo oscuro, sin barra lateral ni cabecera del sistema

---

## 10. Notas Modificatorias

Módulo para solicitar redistribución del presupuesto entre actividades. Flujo completo:

### 10.1 Creación de Solicitud (cualquier rol con permisos)

Formulario con:
- **Tipo:** Inclusión de Ítem (nuevo ítem dentro de una actividad existente) o Inclusión de Actividad (nueva actividad completa)
- **Actividad existente:** selector de la actividad donde se incorporará
- **Nombre del nuevo ítem/actividad**
- **Justificación:** texto libre explicando la necesidad
- **Costo estimado referencial**
- **Archivo sustento:** PDF opcional con documentación de respaldo

Al crear, la nota queda en estado **pendiente**.

### 10.2 Configuración (solo Admin y Coordinación)

Modal de configuración accesible desde la tabla:
- **Actividad origen:** de dónde se transfiere el dinero
- **Monto a transferir**
- **Nuevo clasificador de gasto**
- **Nuevo tipo:** Bien o Servicio
- **Observación administrativa**

Al configurar, la nota pasa a estado **configurada** (aprobada).

### 10.3 Rechazo (solo Admin y Coordinación)

Formulario inline en la tabla:
- Campo de motivo de rechazo
- Botón para ejecutar el rechazo

Al rechazar, la nota pasa a estado **rechazada** (terminal).

---

## 11. Administración de Usuarios

Módulo exclusivo del rol Administrador (`@PreAuthorize("hasRole('Administrador')")`).

### 11.1 Listado de Usuarios

Tabla con columnas:

| Columna | Contenido |
|---|---|
| Nombre | Nombre completo del usuario |
| Email | Correo electrónico (usado como username para login) |
| Rol | Badge de color específico por rol |
| Horario | Badge: "Restringido" (cyan, solo 8am-8pm) o "Bypass" (verde, 24/7) |
| Activo | Botón toggle ON/OFF (verde/gris) |
| Acciones | Botones editar y cambiar contraseña |

### 11.2 Colores por Rol

| Rol | Color | Código Hex |
|---|---|---|
| Administrador | Rojo | #dc2626 |
| Coordinación | Azul | #2563eb |
| Secretaria | Violeta | #7c3aed |
| Director | Cyan | #0891b2 |
| Laboratorio | Naranja | #d97706 |
| Decanato | Gris | #64748b |

### 11.3 Funcionalidades

- **Crear usuario:** modal con nombre, email, contraseña, selector de rol, selector de horario
- **Editar usuario:** modal que oculta el campo contraseña (solo permite cambiar datos no sensibles)
- **Cambiar contraseña:** modal independiente con solo el campo de nueva contraseña
- **Activar/Desactivar:** toggle que cambia el estado `activo` del usuario (soft delete)
- **Validación:** no permite emails duplicados

---

## 12. Horario Laboral

### 12.1 Regla General

El sistema solo es accesible de **8:00 AM a 8:00 PM** en horario de Perú (`America/Lima`). Fuera de este horario, las solicitudes son rechazadas con redirección a `/login?horario`.

### 12.2 Bypass para Administrador

Los usuarios con `horarioRestringido = false` (por defecto solo el Administrador) pueden acceder al sistema 24/7 sin restricción horaria.

### 12.3 Rutas Exentas

Las siguientes rutas NO están sujetas a restricción horaria:

- `/login` — página de inicio de sesión
- `/rastreo/**` — consulta pública de expedientes
- `/css/**`, `/js/**`, `/vendor/**` — recursos estáticos
- `/api/health` — health check
- `/error` — página de error
- `/favicon.ico` — ícono del sitio

### 12.4 Implementación Técnica

- `HorarioLaboralFilter` extiende `OncePerRequestFilter`
- Se ejecuta antes de `UsernamePasswordAuthenticationFilter`
- Calcula la hora actual en zona horaria `America/Lima`
- Si el usuario está autenticado y tiene `horarioRestringido = false`, se omite la validación

---

## 13. Stack Tecnológico

| Capa | Tecnología | Versión |
|---|---|---|
| Backend Framework | Spring Boot | 3.4.1 |
| Lenguaje | Java | 17 |
| Build Tool | Maven | 3.9 |
| ORM | Hibernate / Spring Data JPA | 6.6.4 |
| Base de Datos (dev) | H2 (en memoria) | 2.3 |
| Base de Datos (prod) | PostgreSQL | 15 |
| Frontend | Thymeleaf + Bootstrap 5.3 | 3.1.3 / 5.3.3 |
| Iconos | Bootstrap Icons | 1.11.3 |
| Gráficos | Chart.js | 4.4.0 |
| Seguridad | Spring Security (form login + rememberMe) | 6.4.2 |
| Contraseñas | BCrypt | — |
| Contenerización | Docker + Docker Compose | — |
| Despliegue Cloud | Railway | — |

**Estructura del proyecto (63 archivos Java + 20 recursos):**

```
sisexp/
├── pom.xml
├── Dockerfile
├── src/main/java/com/upla/sisexp/
│   ├── SisexpApplication.java
│   ├── config/          (4 archivos: SecurityConfig, WebConfig, DbIndexInitializer, DataInitializer)
│   ├── security/        (3 archivos: CustomUserDetails, CustomUserDetailsService, HorarioLaboralFilter)
│   ├── model/           (10 entidades JPA)
│   ├── enums/           (9 enumeraciones)
│   ├── repository/      (10 interfaces Spring Data JPA)
│   ├── dto/             (3 DTOs para formularios)
│   ├── service/         (11 servicios de negocio)
│   ├── controller/      (10 controladores MVC + REST)
│   └── exception/       (2 archivos: BusinessException, GlobalExceptionHandler)
└── src/main/resources/
    ├── application.properties
    ├── templates/       (12 vistas Thymeleaf + fragments)
    └── static/          (CSS, JS, vendor)
```

---

## 14. Credenciales de Prueba

El sistema incluye datos de prueba (seed data) que se cargan automáticamente al iniciar con la base de datos vacía.

### Usuarios

| Nombre | Email | Contraseña | Rol | Horario |
|---|---|---|---|---|
| Jefe Administrativo | jefe@upla.edu.pe | jefe123 | Administrador | Bypass 24/7 |
| Coordinador Admin | coord@upla.edu.pe | coord123 | Coordinación | Restringido |
| Secretaria General | secretaria@upla.edu.pe | secretaria123 | Secretaria | Restringido |
| Director de Escuela | director@upla.edu.pe | director123 | Director | Restringido |
| Resp. Laboratorio | lab@upla.edu.pe | lab123 | Laboratorio | Restringido |
| Decano | decanato@upla.edu.pe | decanato123 | Decanato | Restringido |

### Datos de prueba incluidos

| Entidad | Cantidad |
|---|---|
| Roles | 6 |
| Usuarios | 6 |
| Techos Presupuestales | 2 (2025: S/ 45,000 cerrado, 2026: S/ 115,000 activo) |
| Actividades POI | 20 (4 históricas 2025 + 16 vigentes 2026) |
| Necesidades PAP | 13 (en 7 actividades: computadoras, licencias, catering, microscopios, etc.) |
| Expedientes | 5 (1 Borrador, 1 En revisión, 1 Aprobado, 1 Finalizado, 1 Observado) |
| Seguimiento Logs | 13 (cronología completa de cada expediente) |
| Notificaciones | 5 (muestras de cada tipo) |

### Expedientes de ejemplo

| Código | Estado | Costo | Descripción |
|---|---|---|---|
| EXP-2026-0001 | En revisión | S/ 3,600 | Renovación de 3 computadoras del Lab. Cómputo 02 |
| EXP-2026-0002 | Borrador | S/ 1,500 | 15 licencias Microsoft 365 para nuevos docentes |
| EXP-2026-0003 | Aprobado | S/ 3,750 | Servicio de catering para congreso institucional 2026 |
| EXP-2026-0004 | Finalizado | S/ 2,500 | Microscopio binocular para prácticas de biología celular |
| EXP-2026-0005 | Observado | S/ 2,000 | Reactivos para laboratorio de química — segundo semestre |

---

**Documento generado el 22 de junio de 2026.**

**Proyecto académico — Arquitectura de Software — VIII Ciclo — UPLA**
