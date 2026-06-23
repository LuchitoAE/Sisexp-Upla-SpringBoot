# SISEXP-UPLA — Análisis de Alineación SDD vs ERS vs Implementación

**Versión: 1.0 · Mayo 2026**

---

## 1. Alcance real del proyecto (SDD — IEEE 1016)

### 5 Casos de Uso principales

| CU | Nombre | Módulos secundarios | RFs del ERS |
|---|---|---|---|
| **CU-01** | Gestionar expediente y documentos | Registrar, Buscar, Clasificar, Visualizar, Adjuntar, Guardar, Eliminar, Visualizar doc (8) | RF-01 a RF-09, RF-18 |
| **CU-02** | Gestionar proyectos POI/PAP | — | — |
| **CU-03** | Gestionar observaciones y seguimiento | Buscar obs, Adjuntar obs, Revisar evento, Aprobar, Rechazar, Visualizar evento (6) | — |
| **CU-04** | Generar reportes institucionales | Buscar, Generar, Exportar PDF, Exportar Excel (4) | RF-13, RF-14 |
| **CU-05** | Recibir notificaciones automáticas | Generar notif, Enviar panel, Enviar correo, WhatsApp (4) | RF-11, RF-12 |

### 3 Actores (SDD)

| Actor | Módulos | Implementado como |
|---|---|---|
| **Administrador** | Todos | Jefe Oficina |
| **Secretaría** | Expedientes, Documentos | Secretaria |
| **Coordinadora** | Expedientes, Observaciones | *No implementado* |

### 8 Entidades del modelo de dominio (SDD)

| Entidad | En Sequelize | Tabla en BD |
|---|---|---|
| Expediente | Sí | `expedientes` |
| Documento | Sí | `documentos` |
| Observacion | No | `observaciones` (vacía) |
| Proyecto_POI_PAP | No | `proyectos` (vacía) |
| Reportes | No | — |
| Notificacion | No | `notificaciones` (vacía) |
| Historial | Sí (Seguimiento) | `seguimientos` |
| Usuario | Sí | `usuarios` |

---

## 2. % REAL del proyecto

| CU | % Completado | Justificación |
|---|---|---|
| **CU-01** Expedientes | **90%** | 8/8 CUs secundarios implementados. Falta: RF-04 (cargo recepción), RF-05 (derivación completa), RF-07 (estado "Resuelto" → "Finalizado") |
| **CU-02** Proyectos POI/PAP | **0%** | Sin modelo, sin endpoints, sin frontend. Placeholder. |
| **CU-03** Observaciones | **0%** | Sin modelo Sequelize, sin rutas, sin UI. Tabla vacía en BD. |
| **CU-04** Reportes | **50%** | Vista web de estadísticas lista. Falta: exportar PDF, exportar Excel, filtros por periodo de tiempo y responsable. |
| **CU-05** Notificaciones | **0%** | Sin modelo, sin scheduler, sin envíos. Tabla vacía. |
| **Transversal**: Dashboard | **100%** | Completo |
| **Transversal**: Gestión usuarios | **100%** | CRUD completo |
| **Transversal**: Consulta pública | **100%** | Completo |
| **Transversal**: Auth/login | **100%** | JWT + RoleSelector |

### Cálculo ponderado

| Área | Peso | % |
|---|---|---|
| CU-01 | 40% | × 90% = 36% |
| CU-02 | 15% | × 0% = 0% |
| CU-03 | 15% | × 0% = 0% |
| CU-04 | 15% | × 50% = 7.5% |
| CU-05 | 5% | × 0% = 0% |
| Transversal | 10% | × 100% = 10% |
| **Total** | **100%** | **≈ 35-40%** |

---

## 3. Inconsistencias ERS vs SDD vs Implementación

### 3.1 Estados de expediente (RF-07)

| Documento | Valores | Correcto |
|---|---|---|
| **ERS** (RF-07) | Pendiente, En proceso, Resuelto | ❌ No incluye "Observado" ni "Finalizado" |
| **SDD** (diccionario) | (implícito en modelo) | — |
| **Implementación** | Pendiente, En Proceso, Finalizado, Observado | ✅ Usado en todo el sistema |
| **Acción** | Corregir ERS: RF-07 debe decir Pendiente, En Proceso, Finalizado, Observado | |

### 3.2 Actores / Roles

| Documento | Actores | 
|---|---|
| **ERS** | Jefe de Oficina, Secretaria/Asistente, Usuario externo, Sistema (4) |
| **SDD** | Administrador, Secretaría, Coordinadora (3) |
| **Implementación** | Jefe Oficina, Secretaria, Asistente, Externo (4) |
| **Acción** | Alinear SDD y ERS. El SDD define 3 actores pero la implementación tiene 4. La Coordinadora (SDD) no existe en el código. El Asistente y Externo (código) no existen en el SDD. |

### 3.3 Diccionario de datos (SDD)

| Campo SDD | Tipo SDD | Tipo implementado | Diferencia |
|---|---|---|---|
| `cod_expediente` | `varchar(10)` | `CHAR(14)` | SDD dice 10, implementación usa EXP-YYYY-NNNN (14) |
| `estado_expediente` | `varchar(50)` | `ENUM` | SDD usa varchar, implementación usa ENUM |
| `tipo_expediente` | `varchar(50)` | `CHAR(20)` | SDD usa 50, implementación usa 20 |
| `nom_documento` | `varchar(50)` | `VARCHAR(200)` | Longitud no coincide |
| `tip_documento` | `text` | `CHAR(20)` | Tipo diferente |
| `comen_observacion` | `varchar(50)` | No implementado | — |

### 3.4 RFs no implementados del ERS

| RF | Descripción | Estado |
|---|---|---|
| RF-04 | Cargo de recepción imprimible | ❌ No implementado |
| RF-05 | Derivar a área/oficina (completo) | ⚠️ Parcial (solo cambia responsable) |
| RF-11 | Alertas por demora con tiempo configurable | ⚠️ Parcial (solo vencidos, sin scheduler) |
| RF-13 | Reportes en formato imprimible/exportable | ⚠️ Parcial (solo vista web) |
| RF-14 | Filtros: periodo de tiempo, responsable | ❌ No implementado |
| RF-17 | Registro rápido en ≤3 pasos | ❌ No evaluado |

---

## 4. Recomendaciones

### Documentación
1. **ERS**: Corregir RF-07 (estados), alinear actores con SDD
2. **SDD**: Actualizar diccionario de datos (CHAR, longitudes correctas)
3. **SDD**: Agregar actores Asistente y Externo o justificar su exclusión

### Implementación pendiente (próxima iteración)
1. CU-02: Modelo `Proyecto`, endpoints CRUD, frontend
2. CU-03: Modelo `Observacion`, endpoints CRUD, frontend
3. CU-04: Exportación PDF y Excel (usar librería `jspdf` + `xlsx`)
4. CU-05: Sistema de notificaciones con scheduler
5. RF-04: Generación de cargo de recepción PDF
