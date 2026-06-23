# SKILL: Diagramas BCE (Boundary-Control-Entity) — Robustez ICONIX Fase 3

## Propósito
Generar **Diagramas de Robustez (Boundary-Control-Entity)** para SISEXP-UPLA siguiendo la metodología **ICONIX (Fase 3)**. Los diagramas BCE son el puente entre los Casos de Uso (Fase 1-2) y los Diagramas de Secuencia (Fase 4), validando que la estructura del CU sea realizable.

## Metodología ICONIX — Fase 3: Diagramas de Robustez

Los diagramas de robustez utilizan 3 tipos de objetos:

| Símbolo | Tipo | Propósito | Color StarUML |
|:-------:|:----:|:----------|:-------------:|
| `<<boundary>>` | Boundary | Interfaz con el actor (formularios, vistas) | Azul claro |
| `<<control>>` | Control | Lógica de negocio, orquestación (Controllers + Services) | Amarillo |
| `<<entity>>` | Entity | Objetos de dominio persistidos (JPA Entities) | Verde claro |

### Reglas ICONIX para Diagramas de Robustez
1. Los **actores** solo se conectan a **Boundary** objects
2. Los **Boundary** objects solo se conectan a **Control** objects
3. Los **Control** objects se conectan a **Entity** objects y otros **Control** objects
4. Los **Entity** objects nunca se conectan directamente a **Boundary** objects
5. Cada CU produce al menos un diagrama BCE
6. Los diagramas BCE deben validar que el flujo del CU sea implementable

## Convenciones StarUML
- Usar `UMLClassDiagram` como tipo de diagrama
- Estereotipos: `<<boundary>>`, `<<control>>`, `<<entity>>`
- Asociaciones: línea simple con flecha abierta (→)
- Dependencias: línea punteada con flecha (⤍)

## Reglas de mapping Mermaid → StarUML para BCE

| Mermaid | StarUML |
|---------|---------|
| `classDiagram` | `UMLClassDiagram` |
| `<<Boundary>>` class | Class con estereotipo `<<boundary>>` |
| `<<Control>>` class | Class con estereotipo `<<control>>` |
| `<<Entity>>` class | Class con estereotipo `<<entity>>` |
| `..>` dependency | Association (dirigida) |
| `-->` association | Association (simple) |

## Diagramas BCE por Caso de Uso

### BCE01: Iniciar Sesión
```mermaid
classDiagram
    class LoginForm <<Boundary>> {
        +mostrarFormulario()
        +capturarCredenciales()
        +mostrarError()
    }
    class AuthController <<Control>> {
        +login(email, password)
        +logout()
    }
    class AuthService <<Control>> {
        +autenticar(email, password)
    }
    class Usuario <<Entity>> {
        +email
        +password
        +activo
    }
    class CustomUserDetailsService <<Control>> {
        +loadUserByUsername(email)
    }

    LoginForm ..> AuthController : submit
    AuthController ..> AuthService : authenticate
    AuthService ..> CustomUserDetailsService : loadUser
    AuthService ..> Usuario : validate
```

### BCE02: Ver Dashboard
```mermaid
classDiagram
    class DashboardView <<Boundary>> {
        +mostrarKPIs()
        +mostrarSaldos()
        +mostrarAlertas()
    }
    class DashboardController <<Control>> {
        +obtenerKPIs()
        +obtenerSaldos()
        +obtenerAlertas()
    }
    class DashboardService <<Control>> {
        +calcularKPIs()
        +calcularSaldos()
        +calcularAlertas()
    }
    class Expediente <<Entity>>
    class ActividadPOI <<Entity>>

    DashboardView ..> DashboardController : request
    DashboardController ..> DashboardService : query
    DashboardService ..> Expediente : count
    DashboardService ..> ActividadPOI : sum
```

### BCE03: Crear Expediente
```mermaid
classDiagram
    class ExpedienteForm <<Boundary>> {
        +mostrarFormulario()
        +capturarDatos()
        +mostrarConfirmacion()
        +mostrarErrorValidacion()
    }
    class ExpedienteController <<Control>> {
        +crear(datos)
        +listar()
        +obtener(id)
    }
    class ExpedienteService <<Control>> {
        +crearExpediente()
        +reservarSaldo()
        +validarTransicion()
    }
    class BusinessValidationsService <<Control>> {
        +validarSaldoDisponible()
        +validarActividadActiva()
    }
    class BusinessRulesService <<Control>> {
        +reservarSaldoPOI()
    }
    class Expediente <<Entity>>
    class NecesidadPAP <<Entity>>
    class ActividadPOI <<Entity>>
    class SeguimientoLog <<Entity>>
    class Notificacion <<Entity>>

    ExpedienteForm ..> ExpedienteController : submit
    ExpedienteController ..> ExpedienteService : create
    ExpedienteService ..> BusinessValidationsService : validate
    ExpedienteService ..> BusinessRulesService : reserve
    ExpedienteService ..> Expediente : save
    ExpedienteService ..> NecesidadPAP : update
    ExpedienteService ..> ActividadPOI : update
    ExpedienteService ..> SeguimientoLog : create
    ExpedienteService ..> Notificacion : create
```

### BCE04: Cambiar Estado Expediente
```mermaid
classDiagram
    class ExpedienteDetalle <<Boundary>> {
        +mostrarEstado()
        +mostrarOpcionesTransicion()
        +capturarComentario()
        +mostrarConfirmacion()
    }
    class ExpedienteController <<Control>> {
        +cambiarEstado(id, estado, comentario)
    }
    class ExpedienteService <<Control>> {
        +cambiarEstado()
        +crearLog()
        +notificarCambio()
    }
    class BusinessValidationsService <<Control>> {
        +validarTransicionEstado()
    }
    class Expediente <<Entity>>
    class SeguimientoLog <<Entity>>
    class Notificacion <<Entity>>

    ExpedienteDetalle ..> ExpedienteController : changeState
    ExpedienteController ..> ExpedienteService : change
    ExpedienteService ..> BusinessValidationsService : validate
    ExpedienteService ..> Expediente : updateState
    ExpedienteService ..> SeguimientoLog : create
    ExpedienteService ..> Notificacion : notify
```

### BCE05: Adjuntar Documento
```mermaid
classDiagram
    class DocumentoForm <<Boundary>> {
        +mostrarSubida()
        +capturarArchivo()
        +mostrarProgreso()
    }
    class ExpedienteController <<Control>> {
        +adjuntarDocumento(id, archivo)
    }
    class ExpedienteService <<Control>> {
        +adjuntarDocumento()
    }
    class DocumentoAdjunto <<Entity>>
    class Expediente <<Entity>>

    DocumentoForm ..> ExpedienteController : upload
    ExpedienteController ..> ExpedienteService : attach
    ExpedienteService ..> DocumentoAdjunto : save
    ExpedienteService ..> Expediente : update
```

### BCE06: Gestionar Techo Presupuestal
```mermaid
classDiagram
    class TechoForm <<Boundary>> {
        +mostrarFormulario()
        +capturarDatos()
    }
    class TechoPresupuestalController <<Control>> {
        +crear()
        +editar()
        +toggleActivo()
    }
    class TechoPresupuestalService <<Control>> {
        +crear()
        +editar()
        +toggleActivo()
    }
    class TechoPresupuestal <<Entity>>

    TechoForm ..> TechoPresupuestalController : submit
    TechoPresupuestalController ..> TechoPresupuestalService : process
    TechoPresupuestalService ..> TechoPresupuestal : save
```

### BCE07: Gestionar Actividad POI
```mermaid
classDiagram
    class ActividadForm <<Boundary>> {
        +mostrarFormulario()
        +capturarDatos()
    }
    class ActividadPOIController <<Control>> {
        +crear()
        +editar()
        +eliminar()
    }
    class ActividadPOIService <<Control>> {
        +crear()
        +editar()
        +eliminar()
    }
    class ActividadPOI <<Entity>>
    class TechoPresupuestal <<Entity>>

    ActividadForm ..> ActividadPOIController : submit
    ActividadPOIController ..> ActividadPOIService : process
    ActividadPOIService ..> ActividadPOI : save
    ActividadPOIService ..> TechoPresupuestal : validateBudget
```

### BCE08: Gestionar Necesidad PAP
```mermaid
classDiagram
    class NecesidadForm <<Boundary>> {
        +mostrarFormulario()
        +capturarDatos()
    }
    class NecesidadPAPController <<Control>> {
        +crear()
        +editar()
        +eliminar()
    }
    class NecesidadPAPService <<Control>> {
        +crear()
        +editar()
        +eliminar()
    }
    class NecesidadPAP <<Entity>>
    class ActividadPOI <<Entity>>

    NecesidadForm ..> NecesidadPAPController : submit
    NecesidadPAPController ..> NecesidadPAPService : process
    NecesidadPAPService ..> NecesidadPAP : save
    NecesidadPAPService ..> ActividadPOI : validateBudget
```

### BCE09: Gestionar Nota Modificatoria
```mermaid
classDiagram
    class NotaForm <<Boundary>> {
        +mostrarFormulario()
        +capturarDatos()
    }
    class NotaModificatoriaController <<Control>> {
        +crear()
        +configurar()
        +rechazar()
    }
    class NotaModificatoriaService <<Control>> {
        +crear()
        +configurar()
        +rechazar()
    }
    class NotaModificatoria <<Entity>>
    class ActividadPOI <<Entity>>
    class TechoPresupuestal <<Entity>>

    NotaForm ..> NotaModificatoriaController : submit
    NotaModificatoriaController ..> NotaModificatoriaService : process
    NotaModificatoriaService ..> NotaModificatoria : save
    NotaModificatoriaService ..> ActividadPOI : update
    NotaModificatoriaService ..> TechoPresupuestal : update
```

### BCE10: Ver Reportes
```mermaid
classDiagram
    class ReporteView <<Boundary>> {
        +mostrarFiltros()
        +mostrarTabla()
        +mostrarGrafico()
    }
    class ReporteController <<Control>> {
        +reporteAnual()
        +reporteExpedientes()
        +reportePOI()
    }
    class ReporteService <<Control>> {
        +generarReporteAnual()
        +generarReporteExpedientes()
        +generarReportePOI()
        +generarReportePAP()
    }
    class Expediente <<Entity>>
    class ActividadPOI <<Entity>>
    class NecesidadPAP <<Entity>>
    class TechoPresupuestal <<Entity>>

    ReporteView ..> ReporteController : request
    ReporteController ..> ReporteService : generate
    ReporteService ..> Expediente : query
    ReporteService ..> ActividadPOI : query
    ReporteService ..> NecesidadPAP : query
    ReporteService ..> TechoPresupuestal : query
```

### BCE11: Gestionar Usuarios
```mermaid
classDiagram
    class UsuarioForm <<Boundary>> {
        +mostrarFormulario()
        +capturarDatos()
    }
    class UsuarioController <<Control>> {
        +crear()
        +editar()
        +toggleActivo()
    }
    class UsuarioService <<Control>> {
        +crear()
        +editar()
        +toggleActivo()
    }
    class Usuario <<Entity>>
    class Rol <<Entity>>

    UsuarioForm ..> UsuarioController : submit
    UsuarioController ..> UsuarioService : process
    UsuarioService ..> Usuario : save
    UsuarioService ..> Rol : assign
```

### BCE12: Gestionar Notificaciones
```mermaid
classDiagram
    class NotificacionView <<Boundary>> {
        +mostrarLista()
        +mostrarContador()
        +marcarLeida()
    }
    class NotificacionController <<Control>> {
        +listar()
        +contarNoLeidas()
        +marcarLeida()
        +marcarTodas()
    }
    class NotificacionService <<Control>> {
        +listarPorUsuario()
        +contarNoLeidas()
        +marcarLeida()
        +marcarTodasLeidas()
    }
    class Notificacion <<Entity>>

    NotificacionView ..> NotificacionController : request
    NotificacionController ..> NotificacionService : process
    NotificacionService ..> Notificacion : update
```

### BCE13: Rastrear Expediente (Público)
```mermaid
classDiagram
    class RastreoView <<Boundary>> {
        +mostrarFormulario()
        +capturarCodigo()
        +mostrarResultado()
    }
    class RastreoController <<Control>> {
        +rastrear(codigo)
    }
    class ExpedienteService <<Control>> {
        +buscarPorCodigo()
    }
    class Expediente <<Entity>>
    class SeguimientoLog <<Entity>>

    RastreoView ..> RastreoController : search
    RastreoController ..> ExpedienteService : findByCode
    ExpedienteService ..> Expediente : query
    ExpedienteService ..> SeguimientoLog : history
```

### BCE14: Cerrar Sesión
```mermaid
classDiagram
    class HeaderView <<Boundary>> {
        +mostrarOpcionSalir()
    }
    class AuthController <<Control>> {
        +logout()
    }
    class HttpSession <<Entity>> {
        +invalidar()
    }

    HeaderView ..> AuthController : click
    AuthController ..> HttpSession : invalidate
```

## Entregables
1. **BCE-{CU}.md** — Documento fuente por CU con diagrama BCE en Mermaid
2. **BCE-completo.md** — Documento agregado con los 14 diagramas
3. **BCE-completo.docx** — Documento Word generado con pandoc
4. **Archivos StarUML** — `.mdj` con diagramas de clase con estereotipos

## Verificación de calidad ICONIX
- [ ] Cada diagrama tiene al menos 1 Boundary, 1 Control, 1 Entity
- [ ] Actores solo se conectan a Boundary objects
- [ ] Boundary solo se conecta a Control objects
- [ ] Control se conecta a Entity y otros Control
- [ ] Entity nunca se conecta directamente a Boundary
- [ ] El diagrama cubre el flujo completo del CU (básico + alterno)
- [ ] Los nombres de los objetos reflejan su implementación real
- [ ] Existe trazabilidad: BCE-ID ↔ CU-ID ↔ RF-ID ↔ SSD-ID
