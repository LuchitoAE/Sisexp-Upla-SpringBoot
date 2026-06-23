# SISEXP-UPLA — Diagramas de Secuencia del Sistema (SSD)

**ICONIX — Fase 4: Diagramas de Secuencia**

**ISO/IEC 19505 (OMG UML 2.5) · ISO/IEC 29148:2018 · IEEE 830-1998**

---

| Proyecto | SISEXP-UPLA |
|---|---|
| Sistema | Seguimiento y Control de Expedientes |
| Institución | Universidad Peruana Los Andes — Facultad de Ingeniería |
| Metodología | ICONIX (Fase 4) |
| Estándar | ISO/IEC 19505-1:2012 (UML 2.5 Superstructure) |
| Total SSD | 14 (CU01 – CU14) |
| Versión | 1.0 |
| Fecha | Junio 2026 |

---

## Índice

| # | SSD | Caso de Uso | Actor | Pág. |
|---|---|---|---|---|
| 1 | SSD-CU01 | Iniciar Sesión | Usuario | 3 |
| 2 | SSD-CU02 | Ver Dashboard | Usuario | 5 |
| 3 | SSD-CU03 | Crear Expediente | Laboratorio / Secretaria | 7 |
| 4 | SSD-CU04 | Cambiar Estado Expediente | Admin / Coordinación / Secretaria | 9 |
| 5 | SSD-CU05 | Adjuntar Documento | Laboratorio / Secretaria | 11 |
| 6 | SSD-CU06 | Gestionar Techo Presupuestal | Admin / Coordinación | 13 |
| 7 | SSD-CU07 | Gestionar Actividad POI | Admin / Coordinación | 15 |
| 8 | SSD-CU08 | Gestionar Necesidad PAP | Admin / Coordinación | 17 |
| 9 | SSD-CU09 | Gestionar Nota Modificatoria | Admin / Coordinación / Laboratorio / Secretaria / Director | 19 |
| 10 | SSD-CU10 | Ver Reportes | Admin / Coordinación / Director / Decanato | 22 |
| 11 | SSD-CU11 | Gestionar Usuarios | Administrador | 24 |
| 12 | SSD-CU12 | Gestionar Notificaciones | Usuario (cualquier rol) | 26 |
| 13 | SSD-CU13 | Rastrear Expediente | Visitante (público) | 28 |
| 14 | SSD-CU14 | Cerrar Sesión | Usuario (cualquier rol) | 30 |

---

## Introducción

### Propósito de los SSD

Los **Diagramas de Secuencia del Sistema (SSD)** modelan la interacción entre los actores externos y el sistema SISEXP-UPLA, tratando al sistema como una **caja negra**. Cada SSD documenta:

- Los **eventos del sistema** que el actor envía (entradas)
- Las **respuestas del sistema** (salidas)
- Los **fragmentos combinados** que representan bucles, alternativas y opciones
- Las **validaciones y reglas de negocio** ejecutadas internamente

### Contexto en ICONIX (Fase 4)

En la metodología ICONIX, los SSD se construyen a partir de:

1. **ERS** (Fase 1) — casos de uso y requisitos funcionales
2. **Diagramas de Robustez** (Fase 3) — Boundary-Control-Entity
3. **Modelo de Dominio** (Fase 2) — entidades y relaciones

Cada SSD se traza directamente a un caso de uso del ERS y produce las especificaciones de operación que guiarán la implementación en **Spring Boot** (Controllers → Services → Repositories).

### Convenciones utilizadas

| Elemento | Notación Mermaid | Equivalente UML 2.5 |
|---|---|---|
| Actor externo | `actor Nombre` | Actor |
| Sistema (caja negra) | `participant :Sistema` | Lifeline de sistema |
| Mensaje sincrónico | `->>` | Message (sincrónico) |
| Mensaje de retorno | `-->>` | Message (retorno) |
| Activación | `activate / deactivate` | ExecutionSpecification |
| Alternativa | `alt / else / end` | CombinedFragment (alt) |
| Bucle | `loop / end` | CombinedFragment (loop) |
| Opcional | `opt / end` | CombinedFragment (opt) |
| Nota | `Note over` | Note |

### Nomenclatura de mensajes

Los mensajes siguen el patrón `verbo+Complemento()` en camelCase:

- `POST /api/ruta {datos}` — creación
- `PUT /api/ruta/{id} {datos}` — actualización
- `GET /api/ruta` — consulta
- `DELETE /api/ruta/{id}` — eliminación

Las respuestas incluyen el código HTTP y el tipo de dato retornado.

### Formato de código de expediente

`EXP-YYYY-NNNN` donde:
- `YYYY` = año actual (ej: 2026)
- `NNNN` = secuencial de 4 dígitos (ej: 0001)

---

## SSD-CU01: Iniciar Sesión

| Campo | Valor |
|---|---|
| **ID** | SSD-CU01 |
| **CU Reference** | CU01: Iniciar Sesión |
| **Actor** | Usuario (cualquier rol) |
| **Precondición** | Usuario registrado en el sistema, cuenta activa |
| **Descripción** | El usuario se autentica en el sistema mediante email y contraseña. El sistema valida las credenciales, verifica el estado de la cuenta (activa, bloqueada) y aplica las reglas de horario laboral. Si el usuario marca "Recordarme", se genera una cookie persistente por 30 días. |

### Flujo básico

1. Usuario envía credenciales (email + password) desde el formulario de login
2. Sistema busca al usuario por email
3. Sistema verifica que la cuenta esté activa
4. Sistema verifica que la cuenta no esté bloqueada por intentos fallidos
5. Sistema valida la contraseña contra el hash BCrypt
6. Sistema registra el inicio de sesión exitoso (resetea intentos fallidos)
7. Sistema crea la sesión HTTP
8. Sistema redirige al dashboard con datos del usuario

### Flujo alterno A — Credenciales inválidas
1. Sistema incrementa contador de intentos fallidos
2. Si intentos fallidos >= 5, bloquea la cuenta por 30 minutos
3. Sistema retorna mensaje de error "Credenciales inválidas"

### Flujo alterno B — Cuenta bloqueada
1. Sistema detecta que `bloqueadoHasta > ahora`
2. Sistema retorna mensaje "Cuenta bloqueada. Intente nuevamente en X minutos"

### Flujo alterno C — Cuenta inactiva
1. Sistema detecta que `activo = false`
2. Sistema retorna mensaje "Cuenta desactivada. Contacte al administrador"

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: POST /login {email, password}
    activate :Sistema

    :Sistema-->>:Sistema: Buscar usuario por email
    :Sistema-->>:Sistema: Verificar cuenta activa

    alt Cuenta inactiva
        :Sistema-->>Usuario: Redirect /login?error (Cuenta desactivada)
    else Cuenta activa
        :Sistema-->>:Sistema: Verificar bloqueo por intentos

        alt Cuenta bloqueada
            :Sistema-->>Usuario: Redirect /login?error (Cuenta bloqueada)
        else No bloqueada
            :Sistema-->>:Sistema: Validar password (BCrypt)

            alt Password inválido
                :Sistema-->>:Sistema: Incrementar intentosFallidos
                opt Intentos >= 5
                    :Sistema-->>:Sistema: Bloquear cuenta 30 min
                end
                :Sistema-->>Usuario: Redirect /login?error (Credenciales inválidas)
            else Password válido
                :Sistema-->>:Sistema: Resetear intentosFallidos a 0
                :Sistema-->>:Sistema: Crear sesión HTTP + RememberMe
                :Sistema-->>Usuario: Redirect /dashboard (302)
            end
        end
    end

    deactivate :Sistema
```

### Notas para StarUML

| Elemento Mermaid | Elemento StarUML |
|---|---|
| `actor Usuario` | Actor lifeline: nombre "Usuario" |
| `participant :Sistema` | Lifeline: nombre ":Sistema", tipo "Boundary" |
| `activate :Sistema` | Crear ExecutionSpecification en lifeline Sistema |
| Todos los mensajes `->>` | UML Message (sincrónico) |
| Fragmentos `alt/else` | CombinedFragment tipo "alt" |
| Fragmento `opt` | CombinedFragment tipo "opt" |
| Todos los retornos `-->>` | UML Message (retorno), estilo dashed |

---

## SSD-CU02: Ver Dashboard

| Campo | Valor |
|---|---|
| **ID** | SSD-CU02 |
| **CU Reference** | CU02: Ver Dashboard |
| **Actor** | Usuario (cualquier rol) |
| **Precondición** | Usuario autenticado con sesión activa |
| **Descripción** | El usuario accede a la página principal del sistema donde se muestran indicadores clave (KPIs), barras de ejecución presupuestal y alertas de expedientes vencidos o próximos a vencer. |

### Flujo básico

1. Usuario solicita el dashboard (post-login o desde la barra lateral)
2. Sistema verifica que el usuario tenga sesión activa
3. Sistema consulta el total de expedientes agrupados por estado
4. Sistema consulta los saldos de las actividades POI (presupuesto asignado, comprometido, ejecutado, disponible)
5. Sistema consulta las alertas (expedientes vencidos y próximos a vencer en 7 días)
6. Sistema consulta el conteo de notificaciones no leídas del usuario
7. Sistema renderiza la vista dashboard con todos los datos agregados

### Flujo alterno — Sin datos
1. Si no hay expedientes, el dashboard muestra mensaje "No hay expedientes registrados"
2. Las barras de presupuesto se muestran en cero

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /dashboard
    activate :Sistema

    :Sistema-->>:Sistema: Verificar autenticación

    :Sistema-->>:Sistema: Consultar expedientes agrupados por estado
    :Sistema-->>:Sistema: Consultar saldos POI (presupuesto, comprometido, ejecutado)
    :Sistema-->>:Sistema: Consultar alertas (vencidos + próximos 7 días)
    :Sistema-->>:Sistema: Consultar notificaciones no leídas

    :Sistema-->>Usuario: 200 Vista dashboard (KPIs + barras + alertas)

    deactivate :Sistema
```

### Notas para StarUML

- Todas las consultas internas se representan como auto-mensajes (`Sistema -->> Sistema`)
- Las consultas pueden modelarse como mensajes a repositorios si se desea más detalle

---

## SSD-CU03: Crear Expediente

| Campo | Valor |
|---|---|
| **ID** | SSD-CU03 |
| **CU Reference** | CU03: Crear Expediente |
| **Actor** | Laboratorio, Secretaria, Director, Administrador |
| **Precondición** | Usuario autenticado. Existe al menos un Techo Presupuestal activo, una Actividad POI activa y una Necesidad PAP con saldo disponible. |
| **Descripción** | El usuario crea un nuevo expediente seleccionando la actividad POI y necesidad PAP, especificando cantidad, urgencia, descripción y fecha límite. El sistema valida múltiples reglas de negocio (saldo disponible, límite por rol, tope 80%, correspondencia de tipo, período fiscal, fecha límite de actividad) antes de crear el expediente en estado Borrador. |

### Flujo básico

1. Usuario solicita el formulario de creación
2. Sistema carga actividades POI activas con saldo disponible
3. Usuario selecciona actividad POI → Sistema carga necesidades PAP asociadas vía AJAX
4. Usuario completa el formulario (actividad, necesidad, cantidad, urgencia, descripción, fecha límite)
5. Sistema valida que la actividad POI esté activa y no tenga fecha límite vencida
6. Sistema valida que el techo del año esté abierto (no planificado)
7. Sistema calcula el costo estimado = `cantidadSolicitada × precioEstimado`
8. Sistema valida saldo disponible en la actividad POI
9. Sistema valida límite de monto por rol del usuario
10. Sistema valida tope 80% (un expediente no puede consumir más del 80% del saldo disponible)
11. Sistema valida correspondencia de tipo (Bien/Servicio) con la necesidad PAP
12. Sistema genera código de expediente (EXP-YYYY-NNNN)
13. Sistema crea el expediente en estado Borrador
14. Sistema retorna vista con expediente creado

### Flujo alterno A — Error de validación
1. Si alguna validación falla, sistema retorna mensaje de error específico
2. El formulario se mantiene con los datos ingresados para corrección

### Flujo alterno B — Error de carga AJAX
1. Si no hay necesidades PAP disponibles, el selector muestra "Sin ítems disponibles"

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /expedientes/nuevo
    activate :Sistema
    :Sistema-->>Usuario: 200 Vista formulario + actividades POI activas
    deactivate :Sistema

    Usuario->>:Sistema: GET /api/actividades/{id}/necesidades (AJAX)
    activate :Sistema
    :Sistema-->>:Sistema: Consultar necesidades PAP vinculadas
    :Sistema-->>Usuario: 200 [{necesidadPAP}]
    deactivate :Sistema

    Usuario->>:Sistema: POST /expedientes {actividadPoiId, necesidadPapId, cantidad, urgencia, descripcion, fechaLimite}
    activate :Sistema

    :Sistema-->>:Sistema: Validar actividad activa y fecha límite no vencida
    :Sistema-->>:Sistema: Validar techo del año abierto (no planificado)
    :Sistema-->>:Sistema: Calcular costoEstimado = cantidad × precioUnitario
    :Sistema-->>:Sistema: Validar saldo disponible en POI
    :Sistema-->>:Sistema: Validar límite de monto por rol
    :Sistema-->>:Sistema: Validar tope 80% del saldo disponible
    :Sistema-->>:Sistema: Validar correspondencia de tipo (Bien/Servicio)
    :Sistema-->>:Sistema: Generar código EXP-YYYY-NNNN

    alt Validaciones exitosas
        :Sistema-->>:Sistema: Crear expediente (estado: Borrador)
        :Sistema-->>:Sistema: Crear log de seguimiento inicial
        :Sistema-->>Usuario: Redirect /expedientes/{id} (201)
    else Error de validación
        :Sistema-->>Usuario: Redirect /expedientes/nuevo (error específico)
    end

    deactivate :Sistema
```

### Notas para StarUML

- Los mensajes AJAX (`GET /api/actividades/{id}/necesidades`) pueden representarse como un mensaje separado con su propia activación
- Las validaciones internas se agrupan secuencialmente como auto-mensajes
- El fragmento `alt` distingue entre éxito y error de validación

---

## SSD-CU04: Cambiar Estado Expediente

| Campo | Valor |
|---|---|
| **ID** | SSD-CU04 |
| **CU Reference** | CU04: Cambiar Estado Expediente |
| **Actor** | Administrador, Coordinación (aprobar/rechazar/observar), Secretaria (finalizar/derivar) |
| **Precondición** | Usuario autenticado con permisos para la transición. Expediente existe y no está en estado terminal. |
| **Descripción** | El usuario cambia el estado de un expediente siguiendo la máquina de estados definida. Cada transición ejecuta reglas de negocio de reserva/liberación/ejecución de saldos en POI y PAP, y genera notificaciones automáticas. |

### Flujo básico

1. Usuario visualiza detalle del expediente
2. Sistema muestra panel de cambio de estado con transiciones permitidas
3. Usuario selecciona nuevo estado y escribe observación
4. Sistema valida que la transición esté permitida según la máquina de estados
5. Sistema ejecuta reglas de negocio según la transición:
   - `Borrador → En_revisión`: reserva saldo en POI y PAP
   - `En_revisión → Aprobado`: ejecuta saldo (comprometido → ejecutado)
   - `En_revisión → Rechazado`: libera saldo comprometido
   - `En_revisión → Observado`: libera saldo comprometido
   - `Observado → En_revisión`: vuelve a reservar saldo
   - `Aprobado → Finalizado`: sin cambio de saldos
   - `Aprobado → Derivado`: sin cambio de saldos
6. Sistema crea registro de seguimiento (SeguimientoLog)
7. Sistema crea notificación para el solicitante del expediente
8. Sistema redirige al detalle del expediente actualizado

### Flujo alterno A — Transición inválida
1. Sistema muestra error "Transición no permitida desde el estado actual"

### Flujo alterno B — Expediente en estado terminal
1. Si el expediente está Rechazado o Finalizado, el panel de cambio de estado no se muestra

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /expedientes/{id}
    activate :Sistema
    :Sistema-->>:Sistema: Consultar expediente + transiciones permitidas
    :Sistema-->>Usuario: 200 Vista detalle + panel de cambio de estado
    deactivate :Sistema

    Usuario->>:Sistema: PUT /expedientes/{id}/estado {nuevoEstado, observacion}
    activate :Sistema

    :Sistema-->>:Sistema: Validar transición permitida

    alt Transición válida
        :Sistema-->>:Sistema: Ejecutar reglas de negocio según transición

        alt Borrador → En_revision
            :Sistema-->>:Sistema: reservarSaldo(POI)
            :Sistema-->>:Sistema: reservarSaldoPAP(PAP)
        else En_revision → Aprobado
            :Sistema-->>:Sistema: ejecutarSaldo(POI)
            :Sistema-->>:Sistema: ejecutarSaldoPAP(PAP)
        else En_revision → Rechazado
            :Sistema-->>:Sistema: liberarSaldo(POI)
            :Sistema-->>:Sistema: liberarSaldoPAP(PAP)
        else En_revision → Observado
            :Sistema-->>:Sistema: liberarSaldo(POI)
            :Sistema-->>:Sistema: liberarSaldoPAP(PAP)
        else Observado → En_revision
            :Sistema-->>:Sistema: reservarSaldo(POI)
            :Sistema-->>:Sistema: reservarSaldoPAP(PAP)
        end

        :Sistema-->>:Sistema: Actualizar estado del expediente
        :Sistema-->>:Sistema: Crear SeguimientoLog
        :Sistema-->>:Sistema: Crear Notificación para solicitante

        :Sistema-->>Usuario: Redirect /expedientes/{id} (200)
    else Transición inválida
        :Sistema-->>Usuario: Redirect /expedientes/{id}?error (Transición no permitida)
    end

    deactivate :Sistema
```

### Notas para StarUML

- El fragmento `alt` anidado (transición válida + tipo de transición) se modela como CombinedFragment alt anidado
- Las reglas de negocio son auto-mensajes con nombres descriptivos

---

## SSD-CU05: Adjuntar Documento

| Campo | Valor |
|---|---|
| **ID** | SSD-CU05 |
| **CU Reference** | CU05: Adjuntar Documento |
| **Actor** | Laboratorio, Secretaria, Administrador |
| **Precondición** | Usuario autenticado. Expediente existe y no está en estado terminal (Rechazado o Finalizado). |
| **Descripción** | El usuario sube un documento PDF al expediente como sustento. El sistema almacena el archivo en disco (con nombre UUID para evitar colisiones) y registra los metadatos en la base de datos. |

### Flujo básico

1. Usuario abre modal de "Subir documento" desde la vista detalle del expediente
2. Usuario selecciona tipo de documento (TDR, Especificaciones_Técnicas, Cotización, Informe_Técnico)
3. Usuario selecciona archivo PDF (máximo 15 MB)
4. Sistema valida que el archivo sea PDF (por extensión y MIME type)
5. Sistema valida que el tamaño no exceda 15 MB
6. Sistema genera nombre único en disco (UUID + extensión original)
7. Sistema guarda el archivo en el directorio de uploads
8. Sistema registra metadatos en DocumentoAdjunto (tipo, nombreOriginal, nombreArchivo, mimeType, tamaño, expedienteId)
9. Sistema retorna la vista detalle actualizada con el documento listado

### Flujo alterno A — Archivo inválido
1. Sistema muestra error "Solo se permiten archivos PDF"
2. El modal permanece abierto para corregir

### Flujo alterno B — Archivo muy grande
1. Sistema muestra error "El archivo excede el tamaño máximo de 15 MB"

### Flujo alterno C — Eliminación de documento
1. Usuario (solo Admin) solicita eliminar un documento
2. Sistema elimina el archivo del disco y el registro en BD

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: POST /expedientes/{id}/documentos {tipo, archivo}
    activate :Sistema

    :Sistema-->>:Sistema: Validar expediente existe y no es terminal
    :Sistema-->>:Sistema: Validar tipo de documento permitido

    loop Validar archivo
        :Sistema-->>:Sistema: Validar extensión .pdf
        :Sistema-->>:Sistema: Validar MIME type application/pdf
        :Sistema-->>:Sistema: Validar tamaño <= 15 MB
    end

    alt Archivo válido
        :Sistema-->>:Sistema: Generar UUID para nombre en disco
        :Sistema-->>:Sistema: Guardar archivo en /uploads
        :Sistema-->>:Sistema: Crear registro DocumentoAdjunto
        :Sistema-->>Usuario: Redirect /expedientes/{id} (201)
    else Archivo inválido
        :Sistema-->>Usuario: Redirect /expedientes/{id}?error (Tipo/tamaño inválido)
    end

    deactivate :Sistema
```

### Notas para StarUML

- El `loop` de validaciones puede simplificarse a un solo auto-mensaje `validarArchivo()`
- La eliminación es un caso de uso secundario dentro del mismo CU

---

## SSD-CU06: Gestionar Techo Presupuestal

| Campo | Valor |
|---|---|
| **ID** | SSD-CU06 |
| **CU Reference** | CU06: Gestionar Techo Presupuestal |
| **Actor** | Administrador, Coordinación |
| **Precondición** | Usuario autenticado con rol Admin o Coordinación. |
| **Descripción** | El usuario gestiona los techos presupuestales anuales. Puede listar, crear, editar, activar/desactivar y cerrar la planificación de techos. El sistema valida año único y montos positivos. |

### Flujo básico (crear)

1. Usuario solicita la lista de techos presupuestales
2. Sistema consulta todos los techos ordenados por año descendente
3. Usuario hace clic en "Nuevo techo" y completa formulario (año, monto total)
4. Sistema valida que el año no exista ya en la base de datos
5. Sistema valida que el monto total sea mayor a 0
6. Sistema crea el techo con estado activo y planificado = false
7. Sistema redirige al listado con mensaje de éxito

### Flujo básico (editar)
1. Usuario edita año y/o monto total de un techo existente
2. Sistema valida que el techo no esté planificado (cerrado)
3. Sistema actualiza los datos

### Flujo básico (cerrar planificación)
1. Usuario marca un techo como planificado (cerrado)
2. Sistema valida que el techo esté activo
3. Sistema establece `planificado = true`
4. Ya no se permiten modificaciones ni creación de expedientes en ese año

### Flujo alterno — Año duplicado
1. Sistema muestra error "Ya existe un techo para el año XXXX"

### Flujo alterno — Techo planificado
1. Si el techo está planificado, los botones de editar y eliminar no se muestran

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /techos
    activate :Sistema
    :Sistema-->>:Sistema: Consultar todos los techos (ORDER BY año DESC)
    :Sistema-->>Usuario: 200 Vista listado de techos
    deactivate :Sistema

    Usuario->>:Sistema: GET /techos/nuevo
    activate :Sistema
    :Sistema-->>Usuario: 200 Vista formulario (año sugerido: añoActual+1)
    deactivate :Sistema

    Usuario->>:Sistema: POST /techos {año, montoTotal}
    activate :Sistema

    :Sistema-->>:Sistema: Validar año único en BD
    :Sistema-->>:Sistema: Validar montoTotal > 0

    alt Validaciones exitosas
        :Sistema-->>:Sistema: Crear TechoPresupuestal (activo=true, planificado=false)
        :Sistema-->>Usuario: Redirect /techos (201, éxito)
    else Año duplicado
        :Sistema-->>Usuario: Redirect /techos/nuevo (error: año ya existe)
    else Monto inválido
        :Sistema-->>Usuario: Redirect /techos/nuevo (error: monto debe ser > 0)
    end

    deactivate :Sistema

    Usuario->>:Sistema: PUT /techos/{id}/planificar
    activate :Sistema
    :Sistema-->>:Sistema: Validar techo activo
    :Sistema-->>:Sistema: Establecer planificado = true
    :Sistema-->>Usuario: Redirect /techos (200, éxito)
    deactivate :Sistema
```

### Notas para StarUML

- Se muestran tres interacciones separadas: listar, crear, planificar
- Cada interacción tiene su propia activación en StarUML

---

## SSD-CU07: Gestionar Actividad POI

| Campo | Valor |
|---|---|
| **ID** | SSD-CU07 |
| **CU Reference** | CU07: Gestionar Actividad POI |
| **Actor** | Administrador, Coordinación |
| **Precondición** | Existe al menos un Techo Presupuestal activo. Usuario autenticado con permisos. |
| **Descripción** | El usuario gestiona las actividades del Plan Operativo Institucional (POI) asociadas a un techo presupuestal. Puede listar, crear, editar y cambiar el estado de las actividades. |

### Flujo básico (crear)

1. Usuario solicita la lista de actividades POI
2. Sistema consulta todas las actividades con datos del techo asociado
3. Usuario hace clic en "Nueva actividad"
4. Sistema carga los techos disponibles para el selector
5. Usuario completa formulario (techo, código, nombre, presupuesto asignado, fecha límite)
6. Sistema valida que el techo seleccionado esté activo y no planificado
7. Sistema valida que el presupuesto asignado no exceda el disponible del techo
8. Sistema valida que el código sea único dentro del mismo techo
9. Sistema crea la actividad en estado Pendiente
10. Sistema redirige al listado

### Flujo básico (editar)
1. Usuario edita datos de la actividad (excepto si está planificada o cerrada)
2. Sistema actualiza los campos

### Flujo básico (cambiar estado)
1. Usuario cambia estado de la actividad (Pendiente → En Ejecución → Ejecutado → Cerrado)
2. Sistema valida la transición de estado

### Flujo alterno — Presupuesto excede disponible del techo
1. Sistema muestra error "El presupuesto asignado excede el disponible del techo"

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /poi
    activate :Sistema
    :Sistema-->>:Sistema: Consultar actividades POI + techo asociado
    :Sistema-->>Usuario: 200 Vista listado POI
    deactivate :Sistema

    Usuario->>:Sistema: GET /poi/nuevo
    activate :Sistema
    :Sistema-->>:Sistema: Consultar techos activos disponibles
    :Sistema-->>Usuario: 200 Vista formulario + techos
    deactivate :Sistema

    Usuario->>:Sistema: POST /poi {techoId, codigo, nombre, presupuestoAsignado, fechaLimite}
    activate :Sistema

    :Sistema-->>:Sistema: Validar techo activo y no planificado
    :Sistema-->>:Sistema: Validar código único en el techo
    :Sistema-->>:Sistema: Validar presupuesto <= disponible del techo

    alt Validaciones exitosas
        :Sistema-->>:Sistema: Crear ActividadPOI (estado: Pendiente)
        :Sistema-->>Usuario: Redirect /poi (201, éxito)
    else Presupuesto excede disponible
        :Sistema-->>Usuario: Redirect /poi/nuevo (error: presupuesto excede disponible)
    else Código duplicado
        :Sistema-->>Usuario: Redirect /poi/nuevo (error: código ya existe)
    end

    deactivate :Sistema
```

### Notas para StarUML

- Similar estructura a Techo Presupuestal
- Las validaciones de saldo contra el techo padre son críticas

---

## SSD-CU08: Gestionar Necesidad PAP

| Campo | Valor |
|---|---|
| **ID** | SSD-CU08 |
| **CU Reference** | CU08: Gestionar Necesidad PAP |
| **Actor** | Administrador, Coordinación |
| **Precondición** | Existe al menos una Actividad POI activa. Usuario autenticado con permisos. |
| **Descripción** | El usuario gestiona las necesidades del Plan Anual de Contrataciones (PAP) asociadas a una actividad POI. Puede listar, crear, editar y eliminar necesidades. |

### Flujo básico (crear)

1. Usuario solicita la lista de necesidades PAP
2. Sistema consulta todas las necesidades con datos de la actividad asociada
3. Usuario hace clic en "Nueva necesidad"
4. Sistema carga las actividades POI activas para el selector
5. Usuario completa formulario (actividad, nombre, cantidad, precio estimado, unidad, oficina, tipo, clasificador)
6. Sistema valida que la actividad esté activa y no cerrada
7. Sistema valida que el `cantidad × precioEstimado` no exceda el disponible de la actividad
8. Sistema valida que `cantidad >= 1` y `precioEstimado > 0`
9. Sistema calcula: `cantidadDisponible = cantidad`, `montoDisponible = cantidad × precioEstimado`
10. Sistema crea la necesidad PAP
11. Sistema redirige al listado

### Flujo alterno — Presupuesto excede disponible de actividad
1. Sistema muestra error "El costo total excede el saldo disponible de la actividad"

### Flujo alterno — Eliminación
1. Usuario (solo Admin) elimina una necesidad PAP
2. Sistema valida que no tenga expedientes asociados
3. Si tiene expedientes asociados, muestra error "No se puede eliminar: tiene expedientes vinculados"

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /pap
    activate :Sistema
    :Sistema-->>:Sistema: Consultar necesidades + actividad POI asociada
    :Sistema-->>Usuario: 200 Vista listado PAP
    deactivate :Sistema

    Usuario->>:Sistema: GET /pap/nuevo
    activate :Sistema
    :Sistema-->>:Sistema: Consultar actividades POI activas
    :Sistema-->>Usuario: 200 Vista formulario + actividades
    deactivate :Sistema

    Usuario->>:Sistema: POST /pap {actividadPoiId, nombre, cantidad, precioEstimado, unidad, tipo, clasificadorGasto}
    activate :Sistema

    :Sistema-->>:Sistema: Validar actividad activa y no cerrada
    :Sistema-->>:Sistema: Validar cantidad >= 1 y precioEstimado > 0
    :Sistema-->>:Sistema: Calcular costoTotal = cantidad × precioEstimado
    :Sistema-->>:Sistema: Validar costoTotal <= disponible de actividad

    alt Validaciones exitosas
        :Sistema-->>:Sistema: Crear NecesidadPAP (cantDisp=cantidad, montoDisp=costoTotal)
        :Sistema-->>Usuario: Redirect /pap (201, éxito)
    else Costo excede disponible
        :Sistema-->>Usuario: Redirect /pap/nuevo (error: costo excede disponible)
    end

    deactivate :Sistema
```

### Notas para StarUML

- La eliminación es una operación adicional que requiere validación de integridad referencial

---

## SSD-CU09: Gestionar Nota Modificatoria

| Campo | Valor |
|---|---|
| **ID** | SSD-CU09 |
| **CU Reference** | CU09: Gestionar Nota Modificatoria |
| **Actor** | Administrador, Coordinación, Laboratorio, Secretaria, Director (crear); Admin y Coordinación (configurar/rechazar) |
| **Precondición** | Usuario autenticado. Existe al menos una Actividad POI activa. |
| **Descripción** | El usuario solicita una redistribución presupuestal mediante una nota modificatoria. Puede ser de tipo "inclusión de ítem" (nuevo bien/servicio en actividad existente) o "inclusión de actividad" (nueva actividad completa). Admin/Coordinación puede configurar (aprobar) o rechazar la solicitud. |

### Flujo básico (crear)

1. Usuario solicita el formulario de nueva nota modificatoria
2. Sistema carga actividades POI activas para el selector
3. Usuario completa formulario: tipo (inclusión_item / inclusión_actividad), actividad existente, nombre nuevo, justificación, costo estimado, archivo PDF opcional
4. Sistema valida datos obligatorios
5. Sistema genera código de nota (NM-YYYY-NNNN)
6. Sistema crea la nota en estado pendiente
7. Sistema redirige al listado de notas

### Flujo básico (configurar — solo Admin/Coordinación)
1. Usuario abre modal de configuración desde la tabla de notas
2. Usuario selecciona actividad origen (de dónde se transfiere el dinero), monto a transferir, nuevo clasificador, nuevo tipo
3. Sistema valida que la actividad origen tenga saldo disponible suficiente
4. Sistema actualiza saldos: descuenta de actividad origen, acredita en actividad destino (o crea nuevo ítem/actividad)
5. Sistema cambia estado de la nota a "configurada"
6. Sistema crea notificación para el solicitante

### Flujo básico (rechazar — solo Admin/Coordinación)
1. Usuario escribe motivo de rechazo
2. Sistema cambia estado de la nota a "rechazada"
3. Sistema crea notificación para el solicitante

### Flujo alterno — Sin saldo disponible en origen
1. Sistema muestra error "La actividad origen no tiene saldo suficiente para la transferencia"

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Note over Usuario, Sistema: --- CREAR NOTA ---
    Usuario->>:Sistema: GET /notas-modificatorias/nuevo
    activate :Sistema
    :Sistema-->>:Sistema: Consultar actividades POI activas
    :Sistema-->>Usuario: 200 Vista formulario
    deactivate :Sistema

    Usuario->>:Sistema: POST /notas-modificatorias {tipo, actividadExistenteId, nuevoNombre, justificacion, costoEstimado, archivo}
    activate :Sistema

    :Sistema-->>:Sistema: Validar datos obligatorios
    :Sistema-->>:Sistema: Generar código NM-YYYY-NNNN

    alt Validaciones exitosas
        :Sistema-->>:Sistema: Crear NotaModificatoria (estado: pendiente)
        :Sistema-->>Usuario: Redirect /notas-modificatorias (201, éxito)
    else Datos inválidos
        :Sistema-->>Usuario: Redirect /notas-modificatorias/nuevo (error)
    end

    deactivate :Sistema

    Note over Usuario, Sistema: --- CONFIGURAR (ADMIN/COORD) ---
    Usuario->>:Sistema: PUT /notas-modificatorias/{id}/configurar {actividadOrigenId, montoTransferir, nuevoClasificador, nuevoTipo, observacionAdmin}
    activate :Sistema

    :Sistema-->>:Sistema: Validar nota en estado pendiente
    :Sistema-->>:Sistema: Validar actividad origen activa
    :Sistema-->>:Sistema: Validar saldo disponible en origen >= montoTransferir

    alt Saldo suficiente
        :Sistema-->>:Sistema: Actualizar saldos (origen: -monto, destino: +monto)
        :Sistema-->>:Sistema: Cambiar estado nota a "configurada"
        :Sistema-->>:Sistema: Crear Notificación para solicitante (nota_aprobada)
        :Sistema-->>Usuario: Redirect /notas-modificatorias (200, éxito)
    else Saldo insuficiente
        :Sistema-->>Usuario: Redirect /notas-modificatorias (error: saldo insuficiente)
    end

    deactivate :Sistema

    Note over Usuario, Sistema: --- RECHAZAR (ADMIN/COORD) ---
    Usuario->>:Sistema: PUT /notas-modificatorias/{id}/rechazar {observacionAdmin}
    activate :Sistema

    :Sistema-->>:Sistema: Validar nota en estado pendiente
    :Sistema-->>:Sistema: Cambiar estado nota a "rechazada"
    :Sistema-->>:Sistema: Crear Notificación para solicitante (nota_rechazada)
    :Sistema-->>Usuario: Redirect /notas-modificatorias (200)

    deactivate :Sistema
```

### Notas para StarUML

- Este SSD tiene tres sub-flujos claramente diferenciados con notas explicativas
- En StarUML, usar tres diagramas separados o un solo diagrama con separadores visuales
- Las reglas de negocio de transferencia de saldos se modelan como auto-mensajes

---

## SSD-CU10: Ver Reportes

| Campo | Valor |
|---|---|
| **ID** | SSD-CU10 |
| **CU Reference** | CU10: Ver Reportes |
| **Actor** | Administrador, Coordinación, Director, Decanato |
| **Precondición** | Usuario autenticado con permisos de reportes. |
| **Descripción** | El usuario accede al módulo de reportes organizado en 4 pestañas: Expedientes, POI, PAP e Informe Anual. Puede ver datos agregados, tablas detalladas y exportar a CSV. |

### Flujo básico

1. Usuario solicita la página de reportes
2. Sistema verifica permisos del usuario
3. Sistema carga datos de las 4 pestañas:
   - **Expedientes**: total por estado, tabla detallada, vencidos
   - **POI**: actividades con presupuesto, ejecutado y % de ejecución
   - **PAP**: necesidades con cantidades disponibles y ejecutadas
   - **Informe Anual**: techos con montos totales y utilizados
4. Sistema renderiza la vista con tabs
5. Usuario puede navegar entre tabs sin recargar (carga inicial completa)
6. Usuario puede exportar a CSV desde cualquier tab

### Flujo alterno — Exportar CSV
1. Usuario hace clic en "Exportar CSV"
2. Sistema consulta los datos específicos del tab activo
3. Sistema genera archivo CSV con cabeceras
4. Sistema descarga el archivo

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /reportes
    activate :Sistema

    :Sistema-->>:Sistema: Verificar permisos (Admin, Coord, Director, Decanato)

    par Carga de datos de reportes
        :Sistema-->>:Sistema: Consultar expedientes por estado (KPIs)
        :Sistema-->>:Sistema: Consultar tabla detallada de expedientes
        :Sistema-->>:Sistema: Consultar actividades POI con % ejecución
        :Sistema-->>:Sistema: Consultar necesidades PAP (disponible/ejecutado)
        :Sistema-->>:Sistema: Consultar techos para informe anual
    end

    :Sistema-->>Usuario: 200 Vista reportes (4 tabs)

    opt Exportar CSV
        Usuario->>:Sistema: GET /reportes/exportar?tab=expedientes
        activate :Sistema
        :Sistema-->>:Sistema: Consultar datos del tab solicitado
        :Sistema-->>:Sistema: Generar archivo CSV
        :Sistema-->>Usuario: 200 CSV file download
        deactivate :Sistema
    end

    deactivate :Sistema
```

### Notas para StarUML

- El `par` (parallel) fragment indica que las 4 consultas pueden ejecutarse en paralelo
- En StarUML, usar CombinedFragment tipo "par"
- La exportación CSV es opcional (fragmento `opt`)

---

## SSD-CU11: Gestionar Usuarios

| Campo | Valor |
|---|---|
| **ID** | SSD-CU11 |
| **CU Reference** | CU11: Gestionar Usuarios |
| **Actor** | Administrador |
| **Precondición** | Usuario autenticado con rol Administrador. |
| **Descripción** | El administrador gestiona las cuentas de usuario del sistema. Puede listar, crear, editar, activar/desactivar y cambiar contraseñas. Solo el rol Administrador tiene acceso a este módulo. |

### Flujo básico (crear)

1. Admin solicita la lista de usuarios
2. Sistema consulta todos los usuarios (excluyendo contraseñas)
3. Admin hace clic en "Nuevo usuario"
4. Sistema muestra modal con formulario (nombre, email, contraseña, rol, horario)
5. Admin completa el formulario y envía
6. Sistema valida que el email sea único
7. Sistema valida que la contraseña cumpla con la política (mínimo 6 caracteres)
8. Sistema hashea la contraseña con BCrypt
9. Sistema crea el usuario (activo = true, intentosFallidos = 0)
10. Sistema cierra el modal y actualiza la tabla

### Flujo básico (editar)
1. Admin hace clic en editar
2. Sistema muestra modal con datos actuales (sin campo contraseña)
3. Admin modifica nombre, rol o horario
4. Sistema valida y actualiza

### Flujo básico (cambiar contraseña)
1. Admin hace clic en "Cambiar contraseña"
2. Sistema muestra modal solo con campo de nueva contraseña
3. Admin ingresa nueva contraseña
4. Sistema hashea y actualiza

### Flujo básico (activar/desactivar)
1. Admin hace clic en toggle activo/inactivo
2. Sistema cambia el estado `activo` del usuario
3. Si se desactiva, el usuario no puede iniciar sesión

### Flujo alterno — Email duplicado
1. Sistema muestra error "El email ya está registrado"

### Mermaid

```mermaid
sequenceDiagram
    actor Administrador
    participant :Sistema

    Administrador->>:Sistema: GET /usuarios
    activate :Sistema
    :Sistema-->>:Sistema: Verificar rol Administrador
    :Sistema-->>:Sistema: Consultar todos los usuarios (sin passwords)
    :Sistema-->>Administrador: 200 Vista listado usuarios
    deactivate :Sistema

    Administrador->>:Sistema: POST /usuarios {nombre, email, password, rol, horarioRestringido}
    activate :Sistema

    :Sistema-->>:Sistema: Validar email único
    :Sistema-->>:Sistema: Validar password >= 6 caracteres
    :Sistema-->>:Sistema: Hashear password (BCrypt)

    alt Validaciones exitosas
        :Sistema-->>:Sistema: Crear Usuario (activo=true, intentosFallidos=0)
        :Sistema-->>Administrador: Redirect /usuarios (201, éxito)
    else Email duplicado
        :Sistema-->>Administrador: Redirect /usuarios (error: email ya existe)
    end

    deactivate :Sistema

    Administrador->>:Sistema: PUT /usuarios/{id}/toggle-activo
    activate :Sistema
    :Sistema-->>:Sistema: Validar que no sea el propio Admin
    :Sistema-->>:Sistema: Cambiar estado activo (true ↔ false)
    :Sistema-->>Administrador: Redirect /usuarios (200)
    deactivate :Sistema
```

### Notas para StarUML

- La verificación de rol es un paso de seguridad crítico
- La edición y cambio de contraseña son variaciones del mismo flujo

---

## SSD-CU12: Gestionar Notificaciones

| Campo | Valor |
|---|---|
| **ID** | SSD-CU12 |
| **CU Reference** | CU12: Gestionar Notificaciones |
| **Actor** | Usuario (cualquier rol) |
| **Precondición** | Usuario autenticado. |
| **Descripción** | El usuario consulta sus notificaciones generadas automáticamente por cambios de estado en sus expedientes. Puede ver la lista, marcar notificaciones individuales como leídas o marcar todas como leídas. El badge de notificaciones no leídas se actualiza periódicamente vía AJAX. |

### Flujo básico (ver notificaciones)

1. Usuario hace clic en el ícono de campana en la barra superior
2. Sistema redirige a la página de notificaciones
3. Sistema consulta todas las notificaciones del usuario ordenadas por fecha descendente
4. Sistema renderiza la lista con resaltado para no leídas

### Flujo básico (marcar como leída)
1. Usuario hace clic en "Marcar como leída" en una notificación
2. Sistema actualiza `leida = true`
3. Sistema actualiza el badge de conteo

### Flujo básico (marcar todas como leídas)
1. Usuario hace clic en "Marcar todas como leídas"
2. Sistema actualiza todas las notificaciones del usuario a `leida = true`
3. Sistema redirige a la misma página

### Flujo alterno (badge AJAX automático)
1. Cada 60 segundos, el frontend consulta `GET /api/notificaciones/count`
2. Sistema retorna el conteo de no leídas
3. Frontend actualiza el badge rojo en el ícono de campana

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: GET /notificaciones
    activate :Sistema
    :Sistema-->>:Sistema: Consultar notificaciones del usuario (ORDER BY createdAt DESC)
    :Sistema-->>Usuario: 200 Vista notificaciones
    deactivate :Sistema

    Usuario->>:Sistema: PUT /notificaciones/{id}/leer
    activate :Sistema
    :Sistema-->>:Sistema: Actualizar leida = true
    :Sistema-->>Usuario: Redirect /notificaciones (200)
    deactivate :Sistema

    Usuario->>:Sistema: PUT /notificaciones/leer-todas
    activate :Sistema
    :Sistema-->>:Sistema: Actualizar todas las notif. del usuario a leida=true
    :Sistema-->>Usuario: Redirect /notificaciones (200)
    deactivate :Sistema

    Note over Usuario, Sistema: Consulta automática cada 60s
    Usuario->>:Sistema: GET /api/notificaciones/count (AJAX)
    activate :Sistema
    :Sistema-->>:Sistema: Contar notificaciones no leídas
    :Sistema-->>Usuario: 200 {count: N}
    deactivate :Sistema
```

### Notas para StarUML

- La consulta AJAX automática puede representarse como un mensaje etiquetado con `{cada 60s}`
- En StarUML, los bucles temporales no tienen una notación estándar; usar una nota

---

## SSD-CU13: Rastrear Expediente

| Campo | Valor |
|---|---|
| **ID** | SSD-CU13 |
| **CU Reference** | CU13: Rastrear Expediente |
| **Actor** | Visitante (público, sin autenticación) |
| **Precondición** | Ninguna. Ruta pública sin autenticación. |
| **Descripción** | El visitante consulta el estado y ubicación actual de un expediente ingresando su código. El sistema retorna información pública del expediente sin datos sensibles (no muestra montos exactos ni datos del solicitante). |

### Flujo básico

1. Visitante accede a la página pública de rastreo
2. Sistema muestra formulario con campo de código de expediente
3. Visitante ingresa código (formato EXP-YYYY-NNNN)
4. Sistema busca el expediente por código
5. Sistema verifica que el expediente exista
6. Sistema retorna: código, estado (badge de color), actividad POI, ítem PAP, urgencia, fecha límite, última actualización, observaciones (si existen)
7. Sistema no expone: montos, datos del solicitante, información financiera

### Flujo alterno A — Expediente no encontrado
1. Sistema muestra mensaje "No se encontró ningún expediente con el código XXX"

### Flujo alterno B — Código inválido
1. Sistema muestra mensaje "Formato de código inválido. Debe ser EXP-YYYY-NNNN"

### Mermaid

```mermaid
sequenceDiagram
    actor Visitante
    participant :Sistema

    Visitante->>:Sistema: GET /rastreo
    activate :Sistema
    :Sistema-->>Visitante: 200 Vista formulario de búsqueda
    deactivate :Sistema

    Visitante->>:Sistema: GET /rastreo?codigo=EXP-2026-0001
    activate :Sistema

    :Sistema-->>:Sistema: Validar formato EXP-YYYY-NNNN

    alt Formato inválido
        :Sistema-->>Visitante: 400 Vista con error de formato
    else Formato válido
        :Sistema-->>:Sistema: Buscar expediente por código

        alt Expediente encontrado
            :Sistema-->>:Sistema: Ocultar datos sensibles (montos, solicitante)
            :Sistema-->>Visitante: 200 Vista resultado (código, estado, actividad, ítem, urgencia, fecha, obs)
        else No encontrado
            :Sistema-->>Visitante: 404 Vista mensaje "No se encontró expediente"
        end
    end

    deactivate :Sistema
```

### Notas para StarUML

- Este es el único SSD sin autenticación
- La ruta `/rastreo` está exenta de Spring Security y de HorarioLaboralFilter
- El sistema filtra explícitamente datos sensibles antes de responder

---

## SSD-CU14: Cerrar Sesión

| Campo | Valor |
|---|---|
| **ID** | SSD-CU14 |
| **CU Reference** | CU14: Cerrar Sesión |
| **Actor** | Usuario (cualquier rol) |
| **Precondición** | Usuario autenticado con sesión activa. |
| **Descripción** | El usuario finaliza su sesión en el sistema. Se invalida la sesión HTTP, se elimina la cookie Remember Me (si existe) y se redirige al login. |

### Flujo básico

1. Usuario hace clic en "Cerrar sesión" desde la barra superior o menú
2. Sistema invalida la sesión HTTP actual
3. Sistema limpia la autenticación del contexto de seguridad
4. Sistema elimina la cookie JSESSIONID
5. Sistema elimina la cookie "remember-me" (si existe)
6. Sistema redirige al login con mensaje "Sesión cerrada exitosamente"

### Flujo alterno — Sesión ya expirada
1. Si la sesión ya expiró, el usuario es redirigido al login sin mensaje de error

### Mermaid

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema

    Usuario->>:Sistema: POST /logout
    activate :Sistema

    :Sistema-->>:Sistema: Invalidar sesión HTTP
    :Sistema-->>:Sistema: Limpiar SecurityContextHolder
    :Sistema-->>:Sistema: Eliminar cookie JSESSIONID
    :Sistema-->>:Sistema: Eliminar cookie "remember-me"

    :Sistema-->>Usuario: Redirect /login?logout (302)

    deactivate :Sistema
```

### Notas para StarUML

- Diagrama simple: es el CU con menos interacciones
- La invalidación de sesión es una operación del contenedor Servlet (no visible en el código de aplicación)

---

## Trazabilidad

| SSD | CU | Requisito Funcional (ERS) | Diagrama de Robustez |
|---|---|---|---|
| SSD-CU01 | CU01: Iniciar Sesión | RF-1.1, RF-1.2, RF-1.3 | DR-CU01 |
| SSD-CU02 | CU02: Ver Dashboard | RF-2.1, RF-2.2, RF-2.3 | DR-CU02 |
| SSD-CU03 | CU03: Crear Expediente | RF-3.1, RF-3.2, RF-3.3, RF-3.4, RF-3.5 | DR-CU03 |
| SSD-CU04 | CU04: Cambiar Estado | RF-4.1, RF-4.2, RF-4.3, RF-4.4 | DR-CU04 |
| SSD-CU05 | CU05: Adjuntar Documento | RF-5.1, RF-5.2 | DR-CU05 |
| SSD-CU06 | CU06: Gestionar Techo | RF-6.1, RF-6.2 | DR-CU06 |
| SSD-CU07 | CU07: Gestionar POI | RF-7.1, RF-7.2 | DR-CU07 |
| SSD-CU08 | CU08: Gestionar PAP | RF-8.1, RF-8.2 | DR-CU08 |
| SSD-CU09 | CU09: Gestionar Nota Mod. | RF-9.1, RF-9.2, RF-9.3 | DR-CU09 |
| SSD-CU10 | CU10: Ver Reportes | RF-10.1, RF-10.2 | DR-CU10 |
| SSD-CU11 | CU11: Gestionar Usuarios | RF-11.1, RF-11.2 | DR-CU11 |
| SSD-CU12 | CU12: Gestionar Notif. | RF-12.1, RF-12.2 | DR-CU12 |
| SSD-CU13 | CU13: Rastrear Exp. | RF-13.1, RF-13.2 | DR-CU13 |
| SSD-CU14 | CU14: Cerrar Sesión | RF-14.1 | DR-CU14 |

---

## Checklist de Calidad

- [x] Cada lifeline tiene nombre y tipo correcto (actor / :Sistema)
- [x] Los mensajes siguen convención `verbo+Complemento()` en camelCase
- [x] Fragmentos combinados usan etiquetas descriptivas (`alt`, `else`, `loop`, `opt`, `par`)
- [x] Las respuestas tienen código HTTP correcto (200, 201, 302, 400, 401, 404)
- [x] Cada diagrama corresponde al flujo del CU en el ERS
- [x] Trazabilidad: ID del SSD ↔ ID del CU ↔ ID del requisito funcional
- [x] 14 diagramas completos (CU01 – CU14)
- [x] Notas para importación a StarUML incluidas

---

*Documento generado el 23 de junio de 2026.*  
*SISEXP-UPLA — Sistema de Seguimiento y Control de Expedientes*  
*Universidad Peruana Los Andes — Facultad de Ingeniería*  
*Arquitectura de Software — VIII Ciclo*
