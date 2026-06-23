# Informe de Inconsistencias — SDD vs ERS vs Implementación

**SISEXP-UPLA · Mayo 2026**

---

## Resumen Ejecutivo

Se detectaron **10 inconsistencias** entre la Especificación de Requisitos (ERS — IEEE 830), el Diseño del Sistema (SDD — IEEE 1016) y la implementación actual. De ellas, **4 son de alta criticidad** (afectan la coherencia del proyecto), **4 son de criticidad media** (discrepancias de nomenclatura o diseño), y **2 son de baja criticidad** (diferencias menores de formato).

---

## Tabla Maestra de Inconsistencias

| # | Criticidad | Documento | Elemento | Dice | Debería decir | Acción |
|---|---|---|---|---|---|---|
| 1 | **ALTA** | ERS (RF-07) | Estados de expediente | Pendiente, En proceso, Resuelto | Pendiente, En Proceso, Finalizado, Observado | Corregir ERS |
| 2 | **ALTA** | SDD | Actores (3) | Administrador, Secretaría, Coordinadora | Debe incluir: Asistente, Usuario Externo (4) | Corregir SDD o justificar exclusión |
| 3 | **ALTA** | SDD | `cod_expediente` | `varchar(10)` | `CHAR(14)` (formato EXP-YYYY-NNNN) | Actualizar diccionario SDD |
| 4 | **ALTA** | ERS (RF-15/16) | Autenticación como CU-01 | RF-15 y RF-16 listados en CU-01 | No pertenecen al CU-01. Son capa transversal. | Mover a sección de seguridad en ERS |
| 5 | **MEDIA** | SDD | `estado_expediente` | `varchar(50)` | `ENUM` (Pendiente, En Proceso, Finalizado, Observado) | Actualizar diccionario SDD |
| 6 | **MEDIA** | SDD | `tipo_expediente` | `varchar(50)` | `CHAR(20)` (valores predefinidos) | Actualizar diccionario SDD |
| 7 | **MEDIA** | SDD | `tip_documento` | `text` | `CHAR(20)` | Actualizar diccionario SDD |
| 8 | **MEDIA** | ERS | Actores (4) | Jefe de Oficina, Secretaria/Asistente, Usuario Externo, Sistema | Debe alinearse con el SDD o viceversa | Unificar nomenclatura |
| 9 | **BAJA** | SDD | `nom_documento` | `varchar(50)` | `VARCHAR(200)` (nombre real de archivo) | Actualizar diccionario SDD |
| 10 | **BAJA** | Implementación | Número de expediente | `CHAR(14)` EXP-YYYY-NNNN | El SDD dice 10, la implementación dice 14 | Documentar justificación |

---

## Detalle por inconsistencia

### #1 — Estados de expediente (ERS RF-07) ⚠️ ALTA

```
ERS: "El sistema debe permitir actualizar el estado de un expediente entre los valores: 
      Pendiente, En proceso y Resuelto."

SDD: Define 4 estados implícitamente en el modelo de dominio.
Implementación: ENUM con 4 valores: Pendiente, En Proceso, Finalizado, Observado.
```

**Impacto**: El ERS define 3 estados, pero el sistema implementa 4. El estado "Resuelto" del ERS se llama "Finalizado" en el código. Falta "Observado" en el ERS.

**Corrección**: Actualizar RF-07 en el ERS para que refleje los 4 estados reales.

---

### #2 — Actores del sistema (SDD) ⚠️ ALTA

```
SDD:  Administrador — gestiona todos los módulos
      Secretaría — inicia el flujo registrando expedientes
      Coordinadora — revisa, observa, aprueba o rechaza

ERS:  Jefe de Oficina (control total)
      Secretaria / Asistente (operativo)
      Usuario externo (consulta)
      Sistema (alertas automáticas)

Implementación: Jefe Oficina, Secretaria, Asistente, Externo (4 roles).
```

**Impacto**: La Coordinadora del SDD no existe en el código. El Asistente y Externo del código no existen en el SDD. Las responsabilidades de la Coordinadora (revisar, observar, aprobar, rechazar) están parcialmente asignadas al Jefe de Oficina.

**Corrección**: Agregar Asistente y Externo al SDD, o eliminar del código y usar solo 3 roles. La Coordinadora debe implementarse o fusionarse con otro rol.

---

### #3 — Longitud de código de expediente (SDD) ⚠️ ALTA

```
SDD diccionario:   cod_expediente → varchar(10)
Implementación:    numero → CHAR(14), formato EXP-YYYY-NNNN (ej: EXP-2026-0001)
```

**Impacto**: 10 caracteres no son suficientes para el formato `EXP-YYYY-NNNN` (14 caracteres). El SDD subestima la longitud requerida.

**Corrección**: Actualizar el diccionario de datos del SDD: `cod_expediente CHAR(14)`.

---

### #4 — RF-15 y RF-16 en CU-01 (ERS) ⚠️ ALTA

```
ERS:  RF-15 (Gestionar usuarios y permisos) y RF-16 (Control total para jefe)
      están listados junto con los RF del CU-01.

SDD:  Los 5 casos de uso principales NO incluyen autenticación ni gestión de usuarios.
      La autenticación es una capa transversal de seguridad.
```

**Impacto**: El ERS mezcla requisitos funcionales del sistema con requisitos de seguridad transversales.

**Corrección**: Mover RF-15 y RF-16 a una sección independiente de "Seguridad y control de acceso" en el ERS, fuera del alcance del CU-01.

---

### #5-7 — Tipos de datos en diccionario SDD ⚠️ MEDIA

```
SDD dice:  estado_expediente → varchar(50)
           tipo_expediente   → varchar(50)
           tip_documento     → text

Implementación: ENUM, CHAR(20), CHAR(20)
```

**Impacto**: El uso de `varchar` en el SDD contradice las buenas prácticas aplicadas en la implementación (ENUM para valores fijos, CHAR para longitud fija).

**Corrección**: Actualizar el diccionario del SDD con los tipos correctos.

---

### #8 — Nomenclatura de actores entre ERS y SDD ⚠️ MEDIA

```
ERS:        Jefe de Oficina, Secretaria/Asistente, Usuario externo, Sistema
SDD:        Administrador, Secretaría, Coordinadora
Código:     Jefe Oficina, Secretaria, Asistente, Externo
```

**Impacto**: Tres documentos usan tres nomenclaturas diferentes para referirse a los mismos roles.

**Corrección**: Adoptar una nomenclatura unificada en los tres documentos.

---

### #9-10 — Discrepancias menores ⚠️ BAJA

- `nom_documento varchar(50)` en SDD vs `VARCHAR(200)` en implementación (los nombres de archivo pueden superar 50 chars)
- El SDD define `varchar(10)` para código de expediente pero el formato real usa 14 caracteres

---

## Recomendaciones Prioritarias

1. **URGENTE**: Corregir RF-07 en el ERS (estados de expediente)
2. **URGENTE**: Alinear los actores/roles entre SDD, ERS e implementación
3. **URGENTE**: Actualizar el diccionario de datos del SDD con tipos correctos (CHAR, ENUM)
4. **IMPORTANTE**: Mover RF-15 y RF-16 del ERS a sección de seguridad transversal
5. **IMPORTANTE**: Unificar nomenclatura de actores en todos los documentos

---

**Equipo: Aimituma García · Aquino Espinoza · Mosquera Zevallos**
