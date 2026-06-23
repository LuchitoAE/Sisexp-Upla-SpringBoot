# SISEXP-UPLA — Sistema de Diseño UI/UX v3

## Documento de Diseño Aplicado

---

## 1. Stack Tecnológico Frontend

| Librería | Versión | Propósito |
|:---------|:--------|:----------|
| **React** | 19.2.7 | Framework SPA — componentes, hooks, estado, routing cliente |
| **React DOM** | 19.2.7 | Renderizado en el DOM del navegador |
| **React Scripts** | 5.0.1 | Build tooling (Webpack, Babel, DevServer) de Create React App |
| **Inter (Google Fonts)** | — | Tipografía principal — legibilidad, pesos 400-900, variable |
| **CSS Variables** | Nativo CSS3 | Design tokens — colores, sombras, radios, espaciados |

**Sin dependencias externas de UI**: Sin Bootstrap, sin Tailwind, sin Material UI. Todo el CSS es propio, liviano (~5KB gzip), y usa solo variables CSS nativas.

---

## 2. Design Tokens (CSS Variables)

```css
:root {
  --color-primary: #0ea5e9;       /* Azul cielo — botones, links, activos */
  --color-primary-dark: #0284c7;  /* Azul oscuro — hover states */
  --color-primary-light: #e0f2fe; /* Azul claro — fondos suaves */
  --color-bg: #f1f5f9;           /* Gris fondo — cuerpo de página */
  --color-sidebar: #0f172a;       /* Azul noche — sidebar */
  --color-sidebar-hover: #1e293b; /* Azul medianoche — hover sidebar */
  --color-text-sidebar: #cbd5e1; /* Gris claro — texto sidebar */
  --color-card-border: #e2e8f0;  /* Gris borde — tarjetas y tablas */
  --color-text: #0f172a;         /* Negro azulado — texto principal */
  --color-text-muted: #64748b;   /* Gris medio — texto secundario */
  --color-success: #10b981;       /* Verde — éxito, aprobado */
  --color-warning: #f59e0b;       /* Ámbar — alertas, pendiente */
  --color-danger: #ef4444;        /* Rojo — error, rechazado */
  --color-info: #0ea5e9;         /* Azul — información */

  --shadow-card: 0 1px 3px rgba(0,0,0,.08);
  --shadow-card-hover: 0 4px 20px rgba(0,0,0,.12);
  --shadow-modal: 0 20px 60px rgba(0,0,0,.3);
  --shadow-login: 0 20px 60px rgba(0,0,0,.3);

  --radius-sm: 6px;    /* Botones pequeños, chips */
  --radius-md: 8px;    /* Inputs, botones, modales */
  --radius-lg: 12px;   /* Cards, paneles */
  --radius-xl: 1rem;   /* Cards grandes, login */

  --sidebar-w: 250px;   /* Sidebar expandido */
  --topbar-h: 56px;     /* Barra superior */
}
```

---

## 3. Paleta de Colores por Contexto

| Contexto | Color | Uso |
|:---------|:------|:----|
| **Primario** | `#0ea5e9` (Sky 500) | Botones principales, enlaces activos, indicadores |
| **Sidebar** | `#0f172a` → `#1e293b` (gradiente) | Navegación lateral, fondo oscuro |
| **Fondo** | `#f1f5f9` (Slate 100) | Fondo de todas las páginas |
| **Texto** | `#0f172a` (Slate 900) | Texto principal, títulos |
| **Texto muted** | `#64748b` (Slate 500) | Etiquetas, descripciones, metadatos |
| **Éxito** | `#10b981` (Emerald 500) | Aprobado, finalizado, exportar, KPIs positivos |
| **Advertencia** | `#f59e0b` (Amber 500) | Pendiente, observado, alertas amarillas |
| **Error** | `#ef4444` (Red 500) | Rechazado, expirado, alertas rojas, logout |
| **Admin** | `#dc2626` (Red 600) | Rol Administrador |
| **Coord** | `#2563eb` (Blue 600) | Rol Coordinación |
| **Secretaria** | `#7c3aed` (Violet 600) | Rol Secretaría |
| **Director** | `#0891b2` (Cyan 600) | Rol Director |
| **Lab** | `#d97706` (Amber 600) | Rol Laboratorio |
| **Decanato** | `#64748b` (Slate 500) | Rol Decanato |

---

## 4. Sistema de Componentes

### 4.1 Botones

```css
.btn           → Base (inline-flex, gap, transiciones)
.btn-primary   → Azul (#0ea5e9) — acción principal
.btn-secondary → Gris claro — cancelar, secundario
.btn-success   → Verde (#10b981) — confirmar, exportar
.btn-danger    → Rojo (#ef4444) — eliminar, rechazar
.btn-warning   → Ámbar (#f59e0b)
.btn-info      → Azul
.btn-outline   → Borde azul, fondo blanco
.btn-ghost     → Sin borde, transparente
.btn-sm / .btn-xs / .btn-lg → Tamaños
.btn-login     → Gradiente azul, especial login
```

### 4.2 Badges de Estado

Sistema automático: `badge-{EstadoExpediente}`

| Clase CSS | Color fondo | Color texto | Significado |
|:----------|:------------|:------------|:------------|
| `.badge-Borrador` | `#e2e8f0` | `#475569` | Recién creado |
| `.badge-En_revision` | `#dbeafe` | `#1d4ed8` | En revisión por Coord |
| `.badge-Aprobado` | `#d1fae5` | `#059669` | Aprobado |
| `.badge-Rechazado` | `#fee2e2` | `#dc2626` | Rechazado |
| `.badge-Finalizado` | `#e0e7ff` | `#4338ca` | Completado |
| `.badge-Observado` | `#fef3c7` | `#d97706` | Con observaciones |
| `.badge-Derivado` | `#f3e8ff` | `#7c3aed` | Derivado a DGA |

### 4.3 Badges de Urgencia

| Clase CSS | Color fondo | Color texto |
|:----------|:------------|:------------|
| `.badge-Urgente` | `#fee2e2` | `#b91c1c` |
| `.badge-No_tan_urgente` | `#fef3c7` | `#92400e` |
| `.badge-Puede_esperar` | `#f1f5f9` | `#475569` |

### 4.4 Badges de Rol

| Clase CSS | Color fondo | Color texto |
|:----------|:------------|:------------|
| `.badge-Administrador` | `#fee2e2` | `#dc2626` |
| `.badge-Coordinacion` | `#dbeafe` | `#1d4ed8` |
| `.badge-Secretaria` | `#ede9fe` | `#7c3aed` |
| `.badge-Director` | `#cffafe` | `#0891b2` |
| `.badge-Laboratorio` | `#fffbeb` | `#d97706` |
| `.badge-Decanato` | `#f1f5f9` | `#475569` |

---

## 5. Layout Principal

```
┌──────────────────────────────────────────────────┐
│ Sidebar (250px)    │ Topbar (56px altura)        │
│                    │──────────────────────────────│
│ ┌──────────────┐   │                             │
│ │ SISEXP UPLA  │   │  Contenido de página        │
│ │ v2           │   │  (fondo #f1f5f9)            │
│ └──────────────┘   │                             │
│ ─────────────────  │  Cards blancos              │
│ NAVEGACIÓN         │  con sombra sutil           │
│                    │  border-radius 12px         │
│ ▸ Dashboard        │                             │
│ ▸ Expedientes      │  Tablas con headers         │
│ ▸ Techo            │  uppercase gris             │
│ ▸ POI              │                             │
│ ▸ PAP              │  Botones con                │
│ ▸ Reportes         │  transiciones suaves        │
│ ▸ Notas            │                             │
│ ▸ Usuarios         │                             │
│                    │                             │
│ ─────────────────  │                             │
│ 👤 Usuario         │                             │
│ 🚪 Cerrar sesión   │                             │
└──────────────────────────────────────────────────┘
```

---

## 6. Componentes Clave

### 6.1 Login

- Fondo gradiente oscuro: `#0f172a` → `#1e293b`
- Card blanca centrada, `border-radius: 24px`, sombra profunda
- Logo "S" con gradiente azul
- Inputs con focus ring azul (`0 0 0 3px`)
- Botón submit con gradiente y sombra
- 4 botones de acceso rápido demo (semilla)
- Horario laboral: pantalla de fuera de horario con reloj

### 6.2 Sidebar

- Colapsable (250px ↔ 72px)
- Gradiente oscuro vertical
- Indicador activo: barra azul izquierda + fondo semitransparente
- Avatar del usuario con iniciales y color de rol
- Botón logout: fondo rojo semitransparente, hover más intenso
- Toggle colapso: flecha en bottom

### 6.3 Header (Topbar)

- Barra blanca con sombra sutil
- Fecha formateada en español (Perú locale)
- Campana de notificaciones con badge de conteo rojo
- Polling cada 30 segundos
- Dropdown de notificaciones: marcar una / marcar todas leídas
- Avatar + nombre + rol del usuario

### 6.4 Dashboard

- KPIs por año: 4 cards (Asignado, Ejecutado, Comprometido, Disponible)
- Semáforo de alertas: 3 cards (Rojas, Amarillas, Verdes)
- Actividades POI próximas a vencer
- Expedientes sin movimiento (+7 días)
- Saldos en tiempo real con barra de progreso tri-color
- Filtro por año + expandir/colapsar detalle por actividad

### 6.5 Tablas

- Headers: uppercase, gris, fondo `#f8fafc`, borde inferior 2px
- Filas: zebra (alternancia opcional), hover `#f8fafc`
- Sin borde en última fila
- Responsive: `overflow-x: auto` en contenedor

### 6.6 Timeline (Historial de Expedientes)

- Línea vertical gris a la izquierda
- Círculos azules con borde blanco en cada evento
- Fecha pequeña arriba, estado en negrita, observación abajo
- Sin padding en el último item

### 6.7 Modales

- Centrados, backdrop con blur, sombra profunda
- Título + botón cerrar
- Cuerpo con padding 1.25rem
- Footer: cancelar (btn-secondary) + confirmar (btn-primary)

---

## 7. Patrones UX

| Patrón | Implementación |
|:-------|:---------------|
| **Carga inicial** | Skeleton loading (animación shimmer) mientras carga React lazy |
| **Cache API** | `client.js` cachea GETs 30 segundos, invalida en POST/PUT/DELETE |
| **Polling** | Notificaciones cada 30s, Header.js |
| **Cascading selects** | Techo → Actividad POI → Necesidad PAP (ExpedientePage) |
| **Confirmación** | `confirm()` nativo antes de eliminar/toggle/finalizar |
| **Toast** | Alertas modales reutilizables via ModalContext |
| **Auto-dismiss** | Alertas flash desaparecen a los 4 segundos |
| **Redirect loop guard** | `client.js` no redirige a `/login` si ya está en `/login` |
| **Reduced motion** | `prefers-reduced-motion` desactiva animaciones |
| **Focus visible** | `:focus-visible` rings en inputs y botones |

---

## 8. Responsive Design

| Breakpoint | Comportamiento |
|:-----------|:---------------|
| > 768px | Sidebar fijo 250px, contenido con scroll |
| ≤ 768px | Sidebar oculto (translateX -100%), se abre con toggle |
| ≤ 576px | Cards KPI reducen padding, headers de tabla stack vertical |
| Todos | Tablas con `overflow-x: auto` |

---

## 9. Optimizaciones

| Técnica | Impacto |
|:--------|:--------|
| CSS puro (sin frameworks) | ~5KB gzip vs 30KB+ de Bootstrap/Tailwind |
| Lazy loading (React.lazy) | Code splitting automático por página |
| Cache en memoria (client.js) | 0 llamadas repetidas en 30s |
| CSS Variables nativas | 0 dependencias, tema modificable sin recompilar |
| Inter font (Google Fonts) | Carga async, variable font (1 archivo para todos los pesos) |
| `memo()` en componentes | Sidebar y Header no re-renderizan innecesariamente |

---

## 10. Comparación Antes vs Después

| Aspecto | Antes (v2) | Ahora (v3) |
|:--------|:-----------|:-----------|
| Colores | Hardcodeados (#2563eb, etc.) | Variables CSS (`var(--color-primary)`) |
| Badges | Sin sistema unificado | 30+ clases badge por estado/rol/urgencia |
| Cards | Sin hover effect | Hover con sombra + translateY |
| Sidebar | Inline styles | Inline + clases CSS |
| Login | Inline styles | Gradiente, sombras, transiciones |
| Tablas | Estilos básicos | Headers uppercase, hover rows, borders |
| Timeline | No existía | Implementado con CSS puro |
| Progress | Colores fijos | Sistema tri-color (ejec/comp/disp) |
| Responsive | Parcial | 2 breakpoints + reduced motion |
| Carga inicial | Sin skeleton | Animación shimmer en lazy pages |

---

*Documento generado para SISEXP-UPLA — Arquitectura de Software — 2026*
