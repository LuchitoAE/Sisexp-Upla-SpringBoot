# SKILL: Diagramas SSD (System Sequence Diagrams) — ICONIX Fase 4

## Propósito
Generar **Diagramas de Secuencia del Sistema (SSD)** para SISEXP-UPLA siguiendo la metodología **ICONIX (Fase 4)** y el estándar **ISO/IEC 19505 (OMG UML 2.5)**. Los SSD modelan la interacción entre actores externos y el sistema como una caja negra, documentando los eventos del sistema y sus respuestas.

## Metodología ICONIX — Fase 4: Diagramas de Secuencia
En ICONIX, los SSD se construyen a partir de los **Casos de Uso** (ERS) y los **Diagramas de Robustez** (BCE). Cada CU produce uno o más SSD que detallan:
1. **Actor** que inicia la interacción
2. **Mensajes** de entrada (eventos del sistema)
3. **Respuestas** del sistema (salidas)
4. **Bucles** y **alternativas** (fragmentos combinados `loop`, `alt`, `opt`)

## Estándares ISO aplicables
| Estándar | Aplicación |
|----------|------------|
| ISO/IEC 19505-1:2012 (UML 2.5 Superstructure) | Notación de diagramas de secuencia |
| ISO/IEC 29148:2018 | Trazabilidad requisitos ↔ diseño |
| IEEE 830-1998 | Estructura de la especificación |

## Convenciones de nomenclatura StarUML
- **Lifelines**: `Actor: <nombre>` (boundary), `:Sistema` (único)
- **Mensajes**: `verbo+Complemento()` estilo camelCase (ej. `crearExpediente(datos)`)
- **Fragmentos**: `loop` para repeticiones, `alt` para alternativas, `opt` para opcionales
- **Respuestas**: flecha punteada `-->` con etiqueta descriptiva

## Plantilla StarUML (Mermaid → StarUML manual)

### Reglas de mapping Mermaid → StarUML
| Mermaid | StarUML | Compatible |
|---------|---------|:--------:|
| `sequenceDiagram` | `UMLSequenceDiagram` | ✅ |
| `participant` | Lifeline (Boundary) | ✅ |
| `actor` | Actor lifeline | ✅ |
| `->>` | Message (sincrónico) | ✅ |
| `-->>` | Message (retorno) | ✅ |
| `loop` | CombinedFragment (loop) | ✅ |
| `alt/else` | CombinedFragment (alt) | ✅ |
| `opt` | CombinedFragment (opt) | ✅ |
| `Note over` | Note | ✅ |
| `activate/deactivate` | ExecutionSpecification | ✅ |

### Generación en Mermaid
Siempre generar primero el código Mermaid para cada SSD. El código Mermaid se renderiza en GitHub y puede importarse a StarUML manualmente.

## Casos de Uso del Sistema (14 total)

### CU01: Iniciar Sesión
**Actor**: Usuario (cualquier rol)
**Precondición**: Usuario registrado y activo
**Flujo básico**:
1. Usuario envía credenciales (email + password)
2. Sistema valida credenciales
3. Sistema retorna datos del usuario + sesión activa
**Flujo alterno**: Credenciales inválidas → mensaje error

Mermaid:
```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema
    Usuario->>:Sistema: POST /api/auth/login {email, password}
    activate :Sistema
    :Sistema-->>:Sistema: Validar credenciales
    alt Credenciales válidas
        :Sistema-->>Usuario: 200 {usuario, sesión}
    else Credenciales inválidas
        :Sistema-->>Usuario: 401 {error: "Credenciales inválidas"}
    end
    deactivate :Sistema
```

### CU02: Ver Dashboard
**Actor**: Usuario (cualquier rol)
**Flujo**:
1. Usuario solicita dashboard
2. Sistema calcula KPIs (total expedientes, por estado, saldos, alertas)
3. Sistema retorna datos agregados

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema
    Usuario->>:Sistema: GET /api/dashboard/kpis
    activate :Sistema
    :Sistema-->>:Sistema: Consultar expedientes
    :Sistema-->>:Sistema: Agrupar por estado
    :Sistema-->>Usuario: 200 {total, estados, vencidos}
    deactivate :Sistema
    Usuario->>:Sistema: GET /api/dashboard/saldos
    activate :Sistema
    :Sistema-->>:Sistema: Consultar saldos POI
    :Sistema-->>Usuario: 200 [{codigo, presupuesto, ejecutado, disponible}]
    deactivate :Sistema
```

### CU03: Gestionar Expediente (CRUD + Cambio Estado)
**Actor**: Laboratorio, Secretaria, Director, Administrador
**Flujo básico (crear)**:
1. Usuario completa formulario con datos del expediente
2. Sistema valida datos (saldo disponible, actividad activa, etc.)
3. Sistema reserva saldo en POI/PAP
4. Sistema retorna expediente creado (Estado: Borrador)

```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema
    Usuario->>:Sistema: POST /api/expedientes {necesidadId, urgencia, ...}
    activate :Sistema
    :Sistema-->>:Sistema: Validar saldo disponible
    :Sistema-->>:Sistema: Validar actividad activa
    :Sistema-->>:Sistema: Reservar saldo en POI
    :Sistema-->>:Sistema: Crear expediente (Borrador)
    :Sistema-->>:Sistema: Crear log de seguimiento
    :Sistema-->>:Sistema: Crear notificación
    :Sistema-->>Usuario: 200 {expediente creado}
    deactivate :Sistema
```

**Flujo (cambiar estado)**:
```mermaid
sequenceDiagram
    actor Usuario
    participant :Sistema
    Usuario->>:Sistema: PUT /api/expedientes/{id}/estado {estado}
    activate :Sistema
    :Sistema-->>:Sistema: Validar transición permitida
    alt Transición válida
        :Sistema-->>:Sistema: Actualizar estado
        :Sistema-->>:Sistema: Crear log de seguimiento
        :Sistema-->>:Sistema: Crear notificación
        :Sistema-->>Usuario: 200 {expediente actualizado}
    else Transición inválida
        :Sistema-->>Usuario: 400 {error: "Transición no permitida"}
    end
    deactivate :Sistema
```

### CU04: Gestionar Techo Presupuestal (CRUD + Planificar)
**Actor**: Administrador, Coordinacion
**Flujo**:
1. Usuario crea/edita techo (año, monto total)
2. Sistema valida año único, monto positivo
3. Sistema retorna techo creado/actualizado

```mermaid
sequenceDiagram
    actor Admin
    participant :Sistema
    Admin->>:Sistema: POST /api/techos-presupuestales {año, montoTotal}
    activate :Sistema
    :Sistema-->>:Sistema: Validar año único
    :Sistema-->>:Sistema: Validar monto > 0
    :Sistema-->>:Sistema: Crear techo
    :Sistema-->>Admin: 200 {techo creado}
    deactivate :Sistema
```

### CU05: Gestionar Actividad POI (CRUD)
**Actor**: Administrador, Coordinacion
**Flujo**:
1. Usuario crea actividad POI dentro de un techo
2. Sistema valida presupuesto ≤ techo disponible
3. Sistema retorna actividad creada

### CU06: Gestionar Necesidad PAP (CRUD)
**Actor**: Administrador, Coordinacion
**Flujo**:
1. Usuario crea necesidad PAP dentro de una actividad POI
2. Sistema valida presupuesto ≤ actividad disponible
3. Sistema retorna necesidad creada

### CU07: Adjuntar Documento
**Actor**: Laboratorio, Secretaria
**Flujo**:
1. Usuario sube documento a expediente
2. Sistema guarda metadatos
3. Sistema retorna documento adjunto

### CU08: Gestionar Nota Modificatoria (Crear + Configurar + Rechazar)
**Actor**: Administrador, Coordinacion, Laboratorio, Secretaria, Director
**Flujo**:
1. Usuario crea nota modificatoria (incremento/decremento/modificación)
2. Director/Admin configura o rechaza
3. Sistema actualiza saldos si es aceptada

### CU09: Ver Reportes
**Actor**: Administrador, Coordinacion, Director, Decanato
**Flujo**:
1. Usuario solicita reporte (anual, expedientes, POI, PAP)
2. Sistema consulta datos agregados
3. Sistema retorna datos del reporte

### CU10: Gestionar Usuarios
**Actor**: Administrador
**Flujo**:
1. Admin crea/edita/desactiva usuarios
2. Sistema valida email único
3. Sistema retorna usuario actualizado

### CU11: Ver Notificaciones
**Actor**: Usuario (cualquier rol)
**Flujo**:
1. Usuario consulta notificaciones no leídas
2. Sistema retorna lista de notificaciones

### CU12: Marcar Notificación como Leída
**Actor**: Usuario (cualquier rol)
**Flujo**:
1. Usuario marca notificación como leída
2. Sistema actualiza estado

### CU13: Rastrear Expediente (Público)
**Actor**: Visitante (sin autenticación)
**Flujo**:
1. Visitante ingresa código de expediente
2. Sistema retorna estado y ubicación actual

### CU14: Cerrar Sesión
**Actor**: Usuario (cualquier rol)
**Flujo**:
1. Usuario solicita cerrar sesión
2. Sistema invalida sesión
3. Sistema redirige al login

## Entregables
Para cada CU (1-14), generar:

1. **Código Mermaid** del diagrama de secuencia
2. **Archivo StarUML** `.mdj` con el diagrama importado
3. **Documento DOCX** (vía pandoc) con:
   - Narrativa del CU (precondición, flujo básico, flujo alterno)
   - Diagrama de secuencia renderizado
   - Trazabilidad a requisitos del ERS
   - Referencia al diagrama de robustez correspondiente

## Verificación de calidad
- [ ] Cada lifeline tiene nombre y tipo correcto
- [ ] Los mensajes siguen convención `verbo+Complemento()`
- [ ] Fragmentos combinados usan etiquetas descriptivas
- [ ] Las respuestas tienen tipo de dato correcto (200, 400, 401, 404)
- [ ] El diagrama corresponde al flujo del CU en el ERS
- [ ] Trazabilidad: ID del SSD ↔ ID del CU ↔ ID del requisito
