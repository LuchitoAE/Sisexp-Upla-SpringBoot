---
name: ux-ui-design
description: Use when the user asks for UI/UX design, components, layouts, styling, responsive design, accessibility, visual patterns, CSS, Thymeleaf templates, Bootstrap components, modals, forms, tables, badges, or design tokens for Spring Boot Thymeleaf apps with Bootstrap 5.3. Use ONLY for frontend/UI concerns — not for Java backend code.
---

# Skill: UX/UI Design — Thymeleaf + Bootstrap 5.3

---

## 1. DESIGN SYSTEM

### Colors

```css
--color-primary: #0ea5e9;
--color-primary-dark: #0284c7;
--color-bg: #f1f5f9;
--color-sidebar: #0f172a;
--color-sidebar-hover: #1e293b;
--color-text-sidebar: #cbd5e1;
--color-card-border: #e2e8f0;
--color-text: #0f172a;
--color-text-muted: #64748b;
--color-success: #10b981;
--color-warning: #f59e0b;
--color-danger: #ef4444;
--color-info: #0ea5e9;
```

### Typography

```css
font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
font-size-base: 0.9rem;
font-size-sm: 0.8rem;
font-size-lg: 1.1rem;
```

### Spacing

```css
--sidebar-w: 250px;
--topbar-h: 56px;
```

| Context | Value |
|---|---|
| Page padding | 1.5rem |
| Card padding | 1.25rem |
| Nav link padding | 0.6rem 1.25rem |
| Sidebar section header | 0.75rem 1.25rem 0.25rem |
| Modal body padding | 1rem 1.25rem |

### Shadows

```css
--shadow-card: 0 1px 3px rgba(0,0,0,.08);
--shadow-card-hover: 0 4px 20px rgba(0,0,0,.12);
--shadow-modal: 0 20px 60px rgba(0,0,0,.3);
--shadow-login: 0 20px 60px rgba(0,0,0,.3);
```

### Border radius

```css
--radius-sm: 6px;
--radius-md: 8px;
--radius-lg: 12px;
--radius-xl: 1rem;
```

---

## 2. COMPONENT LIBRARY

### 2.1 KPI Card

```html
<div class="kpi-card">
  <div class="d-flex align-items-center gap-3">
    <div class="kpi-icon bg-primary bg-opacity-10">
      <i class="bi bi-file-earmark-text text-primary fs-4"></i>
    </div>
    <div>
      <div class="kpi-label">Total Expedientes</div>
      <div class="kpi-value">42</div>
    </div>
  </div>
</div>
```

```css
.kpi-card {
  background: white;
  border-radius: var(--radius-lg);
  padding: 1.25rem;
  box-shadow: var(--shadow-card);
  transition: box-shadow .2s, transform .2s;
}
.kpi-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}
.kpi-icon {
  width: 44px; height: 44px;
  border-radius: var(--radius-md);
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.kpi-label { font-size: 0.8rem; color: var(--color-text-muted); }
.kpi-value { font-size: 1.5rem; font-weight: 700; color: var(--color-text); }
```

Grid: `class="row g-3"` with `col-6 col-md-4 col-lg-2` per card.

### 2.2 Table Card

```html
<div class="table-card">
  <div class="card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
    <h5 class="mb-0"><i class="bi bi-list me-2 text-primary"></i>Listado</h5>
    <div class="d-flex gap-2">
      <a th:href="@{/entidad/nuevo}" class="btn btn-primary btn-sm">
        <i class="bi bi-plus-lg"></i> Nuevo
      </a>
    </div>
  </div>
  <div class="table-responsive">
    <table class="table table-sm table-hover mb-0">
      <thead class="table-light">
        <tr>
          <th>#</th>
          <th>Nombre</th>
          <th class="text-end">Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr th:each="item, iter : ${items}" th:class="${iter.odd} ? 'table-light' : ''">
          <td th:text="${iter.count}"></td>
          <td th:text="${item.nombre}"></td>
          <td class="text-end">
            <button class="btn btn-outline-primary btn-sm" title="Editar">
              <i class="bi bi-pencil"></i>
            </button>
          </td>
        </tr>
        <tr th:if="${#lists.isEmpty(items)}">
          <td colspan="99" class="text-center text-muted py-4">
            <i class="bi bi-inbox fs-3 d-block mb-2"></i>
            No hay registros
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</div>
```

```css
.table-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.table-card .card-header {
  padding: 1rem 1.25rem;
  border-bottom: 1px solid var(--color-card-border);
  background: transparent;
}
```

### 2.3 Form Card

```html
<div class="form-card">
  <h5 class="mb-4"><i class="bi bi-pencil-square me-2 text-primary"></i>Título</h5>
  <form th:action="@{/entidad}" method="post" class="row g-3">
    <div class="col-md-6">
      <label class="form-label small fw-medium text-muted">Campo</label>
      <input type="text" class="form-control" required>
    </div>
    <div class="col-12 d-flex gap-2 justify-content-end mt-4">
      <a th:href="@{/entidad}" class="btn btn-outline-secondary">
        <i class="bi bi-x-lg"></i> Cancelar
      </a>
      <button type="submit" class="btn btn-primary">
        <i class="bi bi-save"></i> Guardar
      </button>
    </div>
  </form>
</div>
```

```css
.form-card {
  background: white;
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  padding: 1.5rem;
}
.form-control, .form-select {
  border-radius: var(--radius-md);
  padding: 0.65rem 1rem;
  font-size: 0.9rem;
}
.form-control:focus, .form-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(14,165,233,.15);
}
```

### 2.4 State Badges (7 estados expediente)

```css
.badge-Borrador   { background: #e2e8f0; color: #475569; }
.badge-En_revision { background: #dbeafe; color: #1d4ed8; }
.badge-Aprobado   { background: #d1fae5; color: #059669; }
.badge-Rechazado  { background: #fee2e2; color: #dc2626; }
.badge-Finalizado { background: #e0e7ff; color: #4338ca; }
.badge-Observado  { background: #fef3c7; color: #d97706; }
.badge-Derivado   { background: #f3e8ff; color: #7c3aed; }
```

```html
<span th:classappend="|badge-${exp.estado.name()}|"
      th:text="${exp.estado.name().replace('_',' ')}">
</span>
```

Style helper: `.fs-85 { font-size: .85em; }`

### 2.5 Modal

```html
<div th:id="|modal-${id}|" class="modal fade" tabindex="-1"
     data-bs-backdrop="static" data-bs-keyboard="false"
     aria-labelledby="|modalLabel-${id}|" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title" th:id="|modalLabel-${id}|">Título</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
      </div>
      <div class="modal-body">
        <!-- form content -->
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
        <button type="submit" form="form-id" class="btn btn-primary"><i class="bi bi-save"></i> Guardar</button>
      </div>
    </div>
  </div>
</div>
```

### 2.6 Timeline

```html
<div class="timeline">
  <div th:each="log : ${historial}" class="timeline-item">
    <div class="timeline-date" th:text="${#temporals.format(log.createdAt, 'dd/MM/yyyy HH:mm')}"></div>
    <div class="fw-medium" th:text="${log.estadoNuevo.name().replace('_',' ')}"></div>
    <small class="text-muted" th:text="${log.observacion} ?: 'Sin observación'"></small>
  </div>
</div>
```

```css
.timeline {
  position: relative;
  padding-left: 2rem;
}
.timeline::before {
  content: '';
  position: absolute;
  left: 6px; top: 0; bottom: 0;
  width: 2px; background: #e2e8f0;
}
.timeline-item {
  position: relative;
  padding-bottom: 1.5rem;
}
.timeline-item::before {
  content: '';
  position: absolute;
  left: -1.65rem; top: 4px;
  width: 12px; height: 12px;
  background: var(--color-primary);
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 0 0 2px var(--color-primary);
}
.timeline-date { font-size: 0.75rem; color: var(--color-text-muted); }
```

### 2.7 Progress Bar

```html
<div class="progress progress-presupuesto">
  <div class="progress-bar bg-success" role="progressbar"
       th:style="|width: ${pct}%|"
       th:aria-valuenow="${pct}" aria-valuemin="0" aria-valuemax="100">
  </div>
</div>
```

```css
.progress-presupuesto {
  height: 8px;
  border-radius: 4px;
  background: #e2e8f0;
}
```

### 2.8 Toast Notification

```javascript
function showToast(msg, type) {
  type = type || 'info';
  const colors = {success:'#10b981',error:'#ef4444',info:'#0ea5e9',warning:'#f59e0b'};
  const icons = {success:'check-circle',error:'x-circle',info:'info-circle',warning:'exclamation-triangle'};
  const el = document.createElement('div');
  el.style.cssText = 'position:fixed;top:1rem;right:1rem;z-index:9999;background:white;' +
    'border-left:4px solid ' + colors[type] + ';padding:1rem 1.5rem;border-radius:.5rem;' +
    'box-shadow:0 4px 20px rgba(0,0,0,.15);max-width:400px;animation:slideIn .3s ease;font-size:.9rem';
  el.innerHTML = `<i class="bi bi-${icons[type]}" style="color:${colors[type]};margin-right:.5rem"></i> ${msg}`;
  document.body.appendChild(el);
  setTimeout(() => { el.style.opacity='0'; el.style.transition='opacity .3s';
    setTimeout(()=>el.remove(),300); }, 3500);
}
```

```css
@keyframes slideIn {
  from { transform: translateX(100%); opacity: 0; }
  to { transform: translateX(0); opacity: 1; }
}
```

### 2.9 Confirm Action

```javascript
function confirmAction(msg, cb) {
  if (confirm(msg)) cb();
}
```

Usage: `onclick="confirmAction('¿Eliminar?', ()=>document.getElementById('form').submit())"`

### 2.10 Search + Filter (client-side)

```javascript
function filtrarTabla(inputId, tableId, colIndex) {
  const q = document.getElementById(inputId).value.toLowerCase();
  const rows = document.getElementById(tableId).querySelectorAll('tbody tr');
  rows.forEach(r => {
    r.style.display = r.cells[colIndex].textContent.toLowerCase().includes(q) ? '' : 'none';
  });
}
function filtrarEstado(selectId, tableId, colIndex) {
  const val = document.getElementById(selectId).value;
  const rows = document.getElementById(tableId).querySelectorAll('tbody tr');
  rows.forEach(r => {
    r.style.display = !val || r.cells[colIndex].textContent.trim() === val ? '' : 'none';
  });
}
```

```html
<input type="text" id="searchInput" class="form-control form-control-sm"
       placeholder="Buscar..." oninput="filtrarTabla('searchInput','mainTable',1)">
<select id="estadoFilter" class="form-select form-select-sm"
        onchange="filtrarEstado('estadoFilter','mainTable',5)">
  <option value="">Todos</option>
  <option th:each="e : ${estados}" th:value="${e.name()}" th:text="${e.name().replace('_',' ')}"></option>
</select>
```

### 2.11 Empty State

```html
<tr th:if="${#lists.isEmpty(items)}">
  <td colspan="99" class="text-center text-muted py-5">
    <i class="bi bi-inbox fs-1 d-block mb-2"></i>
    <p class="mb-2">No hay registros</p>
    <a th:href="@{/entidad/nuevo}" class="btn btn-primary btn-sm">
      <i class="bi bi-plus-lg"></i> Crear primero
    </a>
  </td>
</tr>
```

### 2.12 Sidebar Navigation

```html
<div class="sidebar" id="sidebar">
  <div class="logo"><i class="bi bi-building text-primary"></i> SISEXP-UPLA</div>
  <div class="section">Principal</div>
  <a href="/dashboard" data-path="/dashboard">
    <i class="bi bi-speedometer2"></i> Dashboard
  </a>
  <div class="section">Sección</div>
  <a href="/entidad" data-path="/entidad">
    <i class="bi bi-icono"></i> Entidad
  </a>
  <div class="mt-auto p-3 border-top border-secondary">
    <form th:action="@{/logout}" method="post">
      <input type="hidden" th:name="${_csrf?.parameterName}" th:value="${_csrf?.token}" />
      <button class="btn btn-sm btn-outline-light w-100">
        <i class="bi bi-box-arrow-right"></i> Salir
      </button>
    </form>
  </div>
</div>
```

```css
.sidebar {
  width: var(--sidebar-w);
  background: var(--color-sidebar);
  color: white;
  min-height: 100vh;
  position: fixed;
  left: 0; top: 0;
  z-index: 1021;
  display: flex;
  flex-direction: column;
  transition: transform .3s;
}
.sidebar .logo {
  padding: 1.25rem;
  border-bottom: 1px solid var(--color-sidebar-hover);
  font-weight: 700;
  font-size: 1.05rem;
}
.sidebar .section {
  padding: 0.75rem 1.25rem 0.25rem;
  font-size: 0.65rem;
  text-transform: uppercase;
  color: #64748b;
  letter-spacing: .05em;
}
.sidebar a {
  color: var(--color-text-sidebar);
  text-decoration: none;
  display: flex;
  align-items: center;
  padding: 0.6rem 1.25rem;
  transition: .15s;
  font-size: 0.88rem;
  gap: .75rem;
}
.sidebar a:hover, .sidebar a.active {
  background: var(--color-sidebar-hover);
  color: white;
  border-left: 3px solid var(--color-primary);
}
.sidebar a i { width: 20px; text-align: center; }
.sidebar .logo i { color: var(--color-primary); }
```

Active link JS:
```javascript
function setActiveSidebarLink() {
  const path = window.location.pathname;
  document.querySelectorAll('.sidebar a').forEach(a => {
    a.classList.toggle('active', a.getAttribute('data-path') === path);
  });
}
```

---

## 3. LAYOUT RECIPES

### 3.1 Authenticated Layout

`templates/layout.html`:
```html
<!DOCTYPE html>
<html lang="es" xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head(title)">
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title th:text="${title}">App</title>
  <th:block th:replace="~{fragments/scripts :: styles}"></th:block>
</head>
<body th:fragment="layout(content)">
<button class="hamburger" onclick="toggleSidebar()" aria-label="Toggle sidebar">
  <i class="bi bi-list"></i>
</button>
<div class="overlay" id="overlay" onclick="toggleSidebar()" aria-hidden="true"></div>
<th:block th:replace="~{fragments/sidebar :: sidebar}" />
<div class="main">
  <th:block th:replace="~{fragments/header :: header}" />
  <div class="container-fluid px-0 pt-3" th:replace="${content}"></div>
</div>
<th:block th:replace="~{fragments/scripts :: scripts}" />
</body>
</html>
```

Page usage:
```html
<head th:replace="~{layout :: head('Título — SISEXP-UPLA')}">
<body th:replace="~{layout :: layout(~{::#main-content})}">
<div id="main-content" th:fragment="content">
  <!-- page content -->
</div>
```

### 3.2 Standalone Layout (Login, Error)

```html
<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Título — SISEXP-UPLA</title>
  <link href="/vendor/bootstrap.min.css" rel="stylesheet">
  <link href="/vendor/bootstrap-icons.css" rel="stylesheet">
  <style>
    body { background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
           min-height: 100vh; display: flex; align-items: center;
           justify-content: center; }
    .card { background: white; border-radius: var(--radius-xl);
            padding: 2.5rem; width: 100%; max-width: 420px;
            box-shadow: var(--shadow-login); }
  </style>
</head>
<body>
  <div class="card"><!-- content --></div>
</body>
</html>
```

---

## 4. UX PATTERNS

### 4.1 Loading State (AJAX selects)

```javascript
async function cargarDependencias(selectId, url, parentValue) {
  const sel = document.getElementById(selectId);
  sel.disabled = true;
  sel.innerHTML = '<option>Cargando...</option>';
  try {
    const res = await fetch(url + '/' + parentValue);
    const data = await res.json();
    sel.innerHTML = '<option value="">Seleccione...</option>'
      + data.map(i => `<option value="${i.id}">${i.nombre}</option>`).join('');
  } catch(e) {
    sel.innerHTML = '<option value="">Error al cargar</option>';
    showToast('Error al cargar datos', 'error');
  } finally {
    sel.disabled = false;
  }
}
```

### 4.2 Auto-Dismiss Alerts

```javascript
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.alert-auto-close').forEach(el => {
    setTimeout(() => {
      el.style.opacity = '0';
      el.style.transition = 'opacity .3s';
      setTimeout(() => el.remove(), 300);
    }, 4000);
  });
});
```

### 4.3 Live Polling (Notification badge)

```javascript
async function loadNotifCount() {
  try {
    const resp = await fetch('/api/notificaciones/count');
    if (!resp.ok) return;
    const data = await resp.json();
    const badge = document.getElementById('notif-badge');
    if (badge) {
      badge.textContent = data.count || 0;
      badge.style.display = data.count > 0 ? '' : 'none';
    }
  } catch(e) { /* ignore */ }
}
setInterval(loadNotifCount, 60000);
loadNotifCount();
```

### 4.4 Cascading Selects

```javascript
document.getElementById('actividadSelect').addEventListener('change', function() {
  const id = this.value;
  if (!id) return;
  cargarDependencias('necesidadSelect',
    '/expedientes/api/necesidades/por-actividad', id);
});
```

### 4.5 Modal Setup via JS

```javascript
document.addEventListener('DOMContentLoaded', () => {
  const modal = document.getElementById('myModal');
  modal.addEventListener('show.bs.modal', function(e) {
    const btn = e.relatedTarget;
    const id = btn.dataset.id;
    const form = this.querySelector('form');
    form.action = id ? '/entidad/' + id : '/entidad/nuevo';
    form.querySelector('[name="nombre"]').value = btn.dataset.nombre || '';
  });
  modal.addEventListener('hidden.bs.modal', function() {
    this.querySelector('form').reset();
  });
});
```

---

## 5. ACCESSIBILITY CHECKLIST

- [ ] All icon-only buttons have `aria-label`
- [ ] Sidebar links: `aria-current="page"` on active
- [ ] Modal: `aria-labelledby="modalLabel-${id}"` + `aria-hidden="true"` + `tabindex="-1"`
- [ ] Progress bars: `role="progressbar"` + `aria-valuenow/min/max`
- [ ] Form inputs: associated `<label>` or `aria-label`
- [ ] Color contrast: text meets WCAG AA (4.5:1) on its background
- [ ] Focus visible: `:focus-visible` outline on interactive elements
- [ ] Reduced motion: `@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation-duration: 0.01ms !important; } }`
- [ ] Error messages: `role="alert"` on dynamic error containers

---

## 6. RESPONSIVE PATTERNS

```css
/* Tablet and mobile */
@media (max-width: 768px) {
  .sidebar { transform: translateX(-100%); }
  .sidebar.open { transform: translateX(0); }
  .main { margin-left: 0 !important; padding: 1rem; }
  .hamburger { display: block; }
  .overlay.show { display: block; }
}

/* Mobile */
@media (max-width: 576px) {
  .kpi-card { padding: 1rem; }
  .table-card .card-header { flex-direction: column; align-items: stretch !important; }
  .modal-dialog { margin: 0.5rem; }
}
```

Always use `<div class="table-responsive">` around tables.

---

## 7. THYMELEAF-SPECIFIC PATTERNS

### 7.1 Dynamic Class from Enum
```html
<span th:classappend="'badge-' + ${entity.state.name()}"></span>
```

### 7.2 Role-Based Visibility
```html
<div sec:authorize="hasRole('Administrador')">
  <!-- admin-only -->
</div>
<div sec:authorize="!isAuthenticated()">
  <!-- anonymous -->
</div>
```

### 7.3 Ternarios seguros (sin ${} anidados)
```html
<!-- CORRECTO -->
<span th:text="${entity.tipo.name() == 'ALTA' ? 'Alta' : (entity.tipo.name() == 'BAJA' ? 'Baja' : 'Media')}"></span>

<!-- INCORRECTO (Thymeleaf 3.1 no lo soporta) -->
<span th:text="${entity.tipo.name() == 'ALTA'} ? 'Alta' : ${entity.tipo.name() == 'BAJA'} ? 'Baja' : 'Media'"></span>
```

### 7.4 Null-safe navigation
```html
<span th:text="${entity?.propiedad ?: '—'}"></span>
```

### 7.5 CSRF en formularios
```html
<input type="hidden" th:name="${_csrf?.parameterName}" th:value="${_csrf?.token}" />
```

### 7.6 Zebra striping en tablas
```html
<tr th:each="item, iter : ${items}" th:class="${iter.odd} ? 'table-light' : ''">
```

### 7.7 Badge via classappend + enum
```html
<span th:classappend="|badge-${exp.estado.name()}|"
      th:text="${exp.estado.name().replace('_',' ')}">
</span>
```

---

## 8. PAGE TEMPLATE SCAFFOLD

```html
<head th:replace="~{layout :: head('Entidades — SISEXP-UPLA')}">
<body th:replace="~{layout :: layout(~{::#main-content})}">
<div id="main-content" th:fragment="content">
  <div class="d-flex justify-content-between align-items-center mb-3">
    <h4 class="mb-0"><i class="bi bi-icon me-2 text-primary"></i>Entidades</h4>
    <button class="btn btn-primary btn-sm" data-bs-toggle="modal" data-bs-target="#modalCrear">
      <i class="bi bi-plus-lg"></i> Nueva
    </button>
  </div>
  <div class="table-card">
    <div class="table-responsive">
      <table class="table table-sm table-hover mb-0" id="mainTable">
        <thead class="table-light">
          <tr>
            <th>#</th>
            <th>Nombre</th>
            <th>Estado</th>
            <th class="text-end">Acciones</th>
          </tr>
        </thead>
        <tbody>
          <tr th:each="item, iter : ${items}" th:class="${iter.odd} ? 'table-light' : ''">
            <td th:text="${iter.count}"></td>
            <td th:text="${item.nombre}"></td>
            <td><span th:classappend="'badge-' + ${item.estado.name()}" th:text="${item.estado.name()}"></span></td>
            <td class="text-end">
              <button class="btn btn-outline-primary btn-sm" title="Editar"
                      data-bs-toggle="modal" data-bs-target="#modalEditar"
                      th:data-id="${item.id}" th:data-nombre="${item.nombre}">
                <i class="bi bi-pencil"></i>
              </button>
            </td>
          </tr>
          <tr th:if="${#lists.isEmpty(items)}">
            <td colspan="99" class="text-center text-muted py-5">
              <i class="bi bi-inbox fs-1 d-block mb-2"></i>
              <p class="mb-2">No hay registros</p>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</div>

<!-- Create Modal -->
<div class="modal fade" id="modalCrear" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <form th:action="@{/entidad}" method="post">
        <div class="modal-header">
          <h5 class="modal-title">Nueva Entidad</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Cerrar"></button>
        </div>
        <div class="modal-body">
          <div class="mb-3">
            <label class="form-label small fw-medium text-muted">Nombre</label>
            <input type="text" name="nombre" class="form-control" required>
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">Cancelar</button>
          <button type="submit" class="btn btn-primary"><i class="bi bi-save"></i> Guardar</button>
        </div>
      </form>
    </div>
  </div>
</div>

<script th:src="@{/js/sisexp.js}"></script>
```

---

## 9. RECOMENDACIONES GENERALES

1. **Siempre `table-responsive`** en tablas — evita desborde horizontal en mobile.
2. **Modales con `tabindex="-1"` y `aria-hidden="true"`** — sin eso, el foco se rompe.
3. **Preferir `th:classappend` sobre `th:class`** — no pierdes clases base.
4. **`th:replace` para layouts, `th:insert` para fragments** repetidos.
5. **CSRF en todo formulario POST** con `th:name="${_csrf?.parameterName}"`.
6. **Ternarios**: `${cond ? val1 : val2}` — NUNCA `${cond} ? ${val1} : ${val2}`.
7. **Badges usar nombre exacto del enum** (`En_revision`, no `En revisión`).
8. **Preferir iconos semánticos** (`bi-check-circle` éxito, `bi-exclamation-triangle` warning).
9. **Sidebar es la navegación principal** — no duplicar nav en topbar.
10. **Polling mínimo 30s** — no saturar el servidor.
