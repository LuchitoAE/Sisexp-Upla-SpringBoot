---
name: frontend-sisexp
description: Use when working on the React SPA frontend, Thymeleaf templates, login page, UI components, authentication flow, navigation, role-based permissions, forms, modals, or Bootstrap styling for SISEXP-UPLA. Covers both the embedded React app (sisexp/frontend/) and Thymeleaf templates (templates/).
---

# Skill: Frontend SISEXP-UPLA — React SPA + Thymeleaf

---

## 1. ARCHITECTURE

Two frontend systems coexist:

| System | Path | Auth | When used |
|--------|------|------|-----------|
| React SPA | `sisexp/frontend/src/` | Cookie/session (`credentials: 'include'`) | Embedded in JAR, served from `/static/` |
| Thymeleaf | `sisexp/src/main/resources/templates/` | Spring Security form login + CSRF | Fallback for error pages, login, static pages |

**SPA fallback**: `WebConfig.java` registers `/**` resource handler that serves `index.html` for non-API routes, enabling React client-side routing.

---

## 2. REACT SPA STRUCTURE

```
frontend/src/
├── api/
│   ├── client.js          # HTTP client: fetch + cache + handleResponse + credentials
│   └── index.js           # API wrappers: authApi, usuarioApi
├── components/
│   ├── Auth/Login.js      # Login page with seeds, horario check
│   ├── Common/
│   │   ├── Modals.js      # ModalProvider, useConfirm, useAlert hooks
│   │   └── Placeholder.js # "Acceso restringido" page
│   └── Layout/
│       ├── Header.js      # Top bar: date, notifications bell, user avatar
│       └── Sidebar.js     # Collapsible nav, role-filtered modules
├── contexts/
│   └── AuthContext.js     # Auth state: user, login, logout, loading, isAuth
├── pages/
│   ├── Dashboard.js       # KPI cards per year, alertas semaforo, saldos tiempo real
│   ├── ExpedientePage.js  # 3 views: list → detail → form, cascading selects, upload
│   ├── TechoPresupuestalPage.js  # CRUD techos, pagination, progress bars
│   ├── ActividadPOIPage.js       # POI CRUD + inline NeedadPAP CRUD
│   ├── NecesidadPAPPage.js       # Read-only hierarchical view
│   ├── NotaModificatoriaPage.js  # Solicitud + bandeja aprobacion (dual-role)
│   ├── ReportesPage.js    # 4 tabs, CSV/PDF export, drill-down
│   └── UsuariosPage.js    # User CRUD, toggle activo/horario
├── utils/
│   └── config.js          # ROL_LABEL, ROL_COLOR, ROL_PROFILE, PUEDE, NAV_MODULES, NAV_PERMISSIONS
├── App.js                 # Root: AuthProvider → ModalProvider → AppContent
├── App.css
└── index.js               # ReactDOM.createRoot
```

---

## 3. AUTH FLOW (Cookie/Session)

1. **Login**: `POST /api/auth/login {email, password}` → sets JSESSIONID cookie → returns `{ usuario: {...} }`
2. **Session check**: on mount, `GET /api/auth/me` verifies existing session
3. **API calls**: `client.js` uses `credentials: 'include'` (cookies sent automatically)
4. **401 handling**: redirect to `/login` UNLESS already on `/login` (prevents redirect loop)
5. **Logout**: `POST /api/auth/logout` invalidates session, clears cookies
6. **Remember-me**: 30-day cookie set by Spring Security

### Auth Debugging

**Loading loop on /login**: caused by `client.js` 401 handler redirecting to `/login` when already there. Fix: add guard `if (window.location.pathname !== '/login')`.

---

## 4. ROLES & PERMISSIONS

Defined in `utils/config.js`:

### NAV_PERMISSIONS (sidebar modules per role)
```javascript
'Administrador': ['dashboard','expedientes','techos','poi','pap','reportes','notas','usuarios']
'Coordinacion':  ['dashboard','expedientes','techos','poi','pap','reportes','notas']
'Secretaria':    ['dashboard','expedientes','techos','poi','pap','notas']
'Director':      ['dashboard','expedientes','techos','poi','pap','reportes','notas']
'Laboratorio':   ['dashboard','expedientes','poi','pap','notas']
'Decanato':      ['dashboard','pap','reportes','notas']
```

### PUEDE (action-level permissions)
```javascript
crearExpediente:   Admin, Coord, Secretaria, Director, Lab
aprobarObservar:   Admin, Coord
rechazar:          Admin, Coord
finalizar:         Admin, Coord, Secretaria
derivar:           Admin, Coord, Secretaria
cambiarEstado:     Admin, Coord
verDerivacion:     Admin, Coord, Secretaria
subirDocumento:    Admin, Coord, Secretaria, Director, Lab
eliminarDocumento: Admin (only)
verTodosExpedientes: Admin, Coord, Secretaria
verReportes:       Admin, Coord, Director, Decanato
```

### Horario Laboral
- Client-side check in `Login.js`: 8am-8pm Peru
- Server-side filter: `HorarioLaboralFilter.java`
- Admin (`horarioRestringido=false`) bypasses both

---

## 5. THYMELEAF TEMPLATES

```
templates/
├── layout.html              # Authenticated layout (sidebar + header + content)
├── login.html               # Login form (rediseñado: card, gradiente, seeds)
├── dashboard.html           # Dashboard (Thymeleaf version)
├── error.html               # Custom error page (404, 403, 500)
├── horario-cerrado.html     # Out-of-hours page
├── expedientes/
│   ├── lista.html           # Expediente list
│   ├── detalle.html         # Expediente detail + timeline
│   └── formulario.html      # New expediente form
├── techos/lista.html        # Techo list
├── poi/lista.html           # POI list
├── pap/lista.html           # PAP list
├── notas/lista.html         # Notas list
├── notificaciones/lista.html
├── usuarios/lista.html
├── reportes/index.html
├── rastreo.html             # Public tracking (no auth)
└── fragments/
    ├── header.html          # Top bar
    ├── sidebar.html         # Side nav
    └── scripts.html         # Bootstrap 5.3 + Bootstrap Icons + sisexp.js
```

### Key Thymeleaf Patterns

- **CSRF**: `<input type="hidden" th:name="${_csrf?.parameterName}" th:value="${_csrf?.token}" />`
- **Role visibility**: `<div sec:authorize="hasRole('Administrador')">` or `sec:authorize="!isAuthenticated()"`
- **Badges**: `<span th:classappend="'badge-' + ${obj.estado.name()}" th:text="${obj.estado.name().replace('_',' ')}">`
- **Null-safe**: `${obj?.propiedad ?: '—'}`
- **Loops with index**: `th:each="item, iter : ${items}" th:class="${iter.odd} ? 'table-light' : ''"`

### Thymeleaf Gotchas
- `.name()` only works on enums, NOT on String fields. For Strings, use `${obj.prop}` directly.
- Nested ternaries in `th:text` must be chained with `? :`, NOT nested `${}`.
- `@Transactional(readOnly = true)` on controller level breaks POST/PUT on PostgreSQL. Use only on `@GetMapping` methods.

---

## 6. CSS / BOOTSTRAP

- **Bootstrap 5.3**: served from `/vendor/bootstrap.min.css`
- **Bootstrap Icons**: `/vendor/bootstrap-icons.css`
- **Custom CSS**: `/css/sisexp.css` — badges, alerts, timeline, KPI cards, sidebar, responsive
- **Design tokens**: `--color-primary: #0ea5e9`, `--sidebar-w: 250px`, `--radius-lg: 12px`
- **Badges**: `.badge-Borrador`, `.badge-En_revision`, `.badge-Aprobado`, `.badge-Rechazado`, `.badge-Finalizado`, `.badge-Observado`, `.badge-Derivado`
- **Urgency badges**: `.badge-Urgente`, `.badge-No_tan_urgente`, `.badge-Puede_esperar`

---

## 7. COMMON ISSUES & FIXES

| Symptom | Cause | Fix |
|---------|-------|-----|
| Page keeps reloading on /login | `client.js` 401 handler redirects to `/login` in loop | Add guard: `if (pathname !== '/login')` |
| Login page without styles | `/vendor` not in `HorarioLaboralFilter.RUTAS_EXENTAS` | Add `/vendor` to exempt routes |
| `.name()` throws SpelEvaluationException | Calling `.name()` on a String instead of enum | Use `${obj.prop}` directly |
| Select sends empty string to @RequestParam Long | `<select>` without `required` | Add `required` on `<select>` or `@RequestParam(required = false)` |
| Chart.js bloating every page | Included in `scripts.html` | Remove canvas + script if not used |
| 404 for API routes served as index.html | `WebConfig` SPA fallback intercepts `/api/**` | Add `if (resourcePath.startsWith("api/")) return null;` |
