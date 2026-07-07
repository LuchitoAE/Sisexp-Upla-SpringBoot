# SISEXP-UPLA Frontend — Guía para Desarrolladores

## Requisitos

- Node.js >= 18
- pnpm >= 9 (`npm install -g pnpm`)

## Instalación

```bash
cd sisexp/frontend-next
pnpm install
```

## Scripts disponibles

| Comando | Descripción |
|---------|-------------|
| `pnpm dev` | Inicia servidor de desarrollo (Vite) |
| `pnpm build` | Compila TypeScript + build producción |
| `pnpm lint` | Ejecuta oxlint (0 errores requerido) |
| `pnpm preview` | Previsualiza build producción localmente |

## Estructura del proyecto

```
src/
├── components/
│   ├── layout/        # AppLayout, Sidebar, Header, PageLayout, PageHeader
│   └── ui/            # Componentes reutilizables (12 total)
│       ├── Button.tsx + Button.module.css
│       ├── Card.tsx + Card.module.css
│       ├── Modal.tsx + Modal.module.css
│       └── ...
├── contexts/          # AuthContext (provider + hook)
├── hooks/             # usePageData, useNotifications
├── pages/             # 9 páginas, cada una con su .module.css
│   ├── Dashboard/
│   ├── Login/
│   ├── Expedientes/
│   └── ...
├── services/          # api.ts (fetch + cache), endpoints.ts
├── styles/            # tokens.css, globals.css, reset.css
├── types/             # interfaces y tipos compartidos
└── utils/             # config.ts, format.ts
```

## Cómo crear un componente

1. Crear archivo en `src/components/ui/NuevoComponente.tsx`
2. Crear `src/components/ui/NuevoComponente.module.css`
3. Usar **camelCase** para nombres de clase CSS
4. Importar `styles` y usar `styles.miClase`
5. Usar tokens de `tokens.css` (colores, espaciado, tipografía, etc.)
6. Exportar con `export function`
7. Añadir barrel export en `src/components/ui/index.ts`

**Ejemplo mínimo:**

```tsx
import styles from './NuevoComponente.module.css';

interface Props { titulo: string }

export function NuevoComponente({ titulo }: Props) {
  return <div className={styles.wrapper}><h2>{titulo}</h2></div>;
}
```

## Cómo crear una página

1. Crear carpeta en `src/pages/MiPagina/`
2. Crear `MiPagina.tsx` + `MiPagina.module.css`
3. Usar `PageLayout` + `PageHeader`
4. Exportar con `export default function` (para lazy loading)
5. Añadir lazy import en `AppLayout.tsx`

**Ejemplo mínimo:**

```tsx
import { PageLayout } from '../../components/layout/PageLayout';
import { PageHeader } from '../../components/layout/PageHeader';
import styles from './MiPagina.module.css';

export default function MiPagina() {
  return (
    <PageLayout>
      <PageHeader title="Mi Página" description="Descripción breve" />
      {/* contenido */}
    </PageLayout>
  );
}
```

## Cómo consumir datos

Usar el hook `usePageData`:

```tsx
const { data, loading, error, refetch } = usePageData(
  () => miApi.list(),
  [],
);
```

Manejar estados:

```tsx
if (loading) return <PageSkeleton />;
if (error) return <div className="cardError">⚠ {error}</div>;
if (!data || data.length === 0) return <EmptyState title="Sin datos" />;
```

## Cómo mostrar notificaciones

```tsx
import { useToast } from '../../components/ui/Toast';

function MiComponente() {
  const { toast } = useToast();
  const handleClick = () => {
    toast.success('Operación exitosa');
    // toast.error('Error'); toast.warning('Cuidado'); toast.info('Informativo');
  };
}
```

## Cómo confirmar acciones

```tsx
import { ConfirmDialog } from '../../components/ui/ConfirmDialog';

function MiComponente() {
  const [confirmOpen, setConfirmOpen] = useState(false);
  return (
    <>
      <Button onClick={() => setConfirmOpen(true)}>Eliminar</Button>
      <ConfirmDialog
        open={confirmOpen}
        variant="danger"
        message="¿Está seguro de eliminar?"
        onConfirm={() => { /* acción */; setConfirmOpen(false); }}
        onCancel={() => setConfirmOpen(false)}
      />
    </>
  );
}
```

## Buenas prácticas

### CSS
- Usar siempre variables de `tokens.css` para colores, tamaños, espaciados
- Nombres de clase en **camelCase** (CSS Modules)
- Sin estilos inline para valores estáticos
- Sin `!important` (salvo casos excepcionales documentados)
- Animaciones con `prefers-reduced-motion: reduce`

### TypeScript
- Preferir `type` para uniones, `interface` para objetos
- Nunca usar `any` — usar `unknown` con narrowing
- Importar tipos con `import type { ... }`
- Usar genéricos para componentes reutilizables

### React
- Componentes puros (sin estado cuando sea posible)
- `React.memo` solo si profiling muestra beneficio
- Hooks personalizados para lógica compartida
- No mutar props ni estado
- `key` prop estable en `.map()` (nunca índice)

### Accesibilidad
- `role="status"` en badges
- `role="progressbar"` + `aria-valuenow/min/max` en progresos
- `role="button"` + `tabIndex={0}` + `onKeyDown` en divs clickeables
- Focus trapping en modales
- Contraste WCAG AA (mínimo 4.5:1)
- `aria-label` en botones sin texto visible

### Convenciones de nomenclatura
- **Archivos**: PascalCase para componentes (`Button.tsx`), camelCase para hooks/utils (`useAuth.ts`)
- **Exportaciones**: named exports para componentes, default exports para páginas
- **Interfaces**: PascalCase, Props sufijo (`ButtonProps`)
- **Tipos**: type alias (`RolUsuario`, `EstadoExpediente`)
- **CSS Modules**: camelCase (`statCard`, `navItemActive`)
- **Variables CSS**: kebab-case (`--color-brand-500`)

## Flujo de datos

```
Página → usePageData(fetcher) → services/endpoints.ts → api.ts → Backend API (/api/*)
                                                                       ↓
AuthContext (JWT) ← services/api.ts ← 401/403 response
                                                                       ↓
Estado: loading → PageSkeleton
        error   → cardError
        data    → Componentes UI (Card, Table, Badge, etc.)
        empty   → EmptyState
```

## Roles y permisos

- `puede(rol, accion)` — verifica permiso específico
- `esGestionPresupuestal(rol)` — Admin o Coordinación
- `NAV_PERMISSIONS[rol]` — módulos accesibles por rol
- Configuración completa en `src/utils/config.ts`

## Integración con backend

- Base URL: `/api` (proxy NGINX)
- Auth: cookies JWT (credentials: 'include')
- Cache: automática 30s en GET, invalida en POST/PUT/DELETE
- Errores 401 → redirect a login
- Errores 403 → muestra mensaje del servidor (horario laboral, permisos)
- CORS: manejado por API Gateway

## Preguntas frecuentes

**¿Por qué no se usa react-router?**
La navegación es state-based gracias a la SPA con Sidebar + AppLayout. No hay rutas URL públicas que necesiten enrutamiento del lado del cliente.

**¿Cómo agrego un nuevo módulo?**
1. Añadir entrada en `NAV_MODULES` en `config.ts`
2. Añadir permisos en `NAV_PERMISSIONS`
3. Crear página en `pages/MiModulo/`
4. Añadir lazy import + case en `AppLayout.tsx`
5. Añadir icono en `MODULE_ICONS` en `Sidebar.tsx`

**¿Cómo cambio el diseño?**
Modificar tokens en `tokens.css` — colores, tipografía, espaciado, radios, sombras. Los cambios se propagan automáticamente a todos los componentes.
