import { useState, Suspense, lazy } from 'react';
import { AuthProvider } from '../../contexts/AuthContext';
import { Sidebar } from './Sidebar';
import { Header } from './Header';
import { PageLayout } from './PageLayout';
import { Card } from '../ui/Card';

// Lazy load pages
const Login = lazy(() => import('../../pages/Login/Login'));
const Dashboard = lazy(() => import('../../pages/Dashboard/Dashboard'));
const ExpedientePage = lazy(() => import('../../pages/Expedientes/ExpedientePage'));
const TechoPresupuestalPage = lazy(() => import('../../pages/Techos/TechoPresupuestalPage'));
const ActividadPOIPage = lazy(() => import('../../pages/ActividadesPOI/ActividadPOIPage'));
const NecesidadPAPPage = lazy(() => import('../../pages/NecesidadesPAP/NecesidadPAPPage'));
const NotaModificatoriaPage = lazy(() => import('../../pages/NotasModificatorias/NotaModificatoriaPage'));
const ReportesPage = lazy(() => import('../../pages/Reportes/ReportesPage'));
const UsuariosPage = lazy(() => import('../../pages/Usuarios/UsuariosPage'));

import { PageSkeleton } from '../ui/Skeleton';
import { ToastProvider } from '../ui/Toast';
import { useAuth } from '../../contexts/AuthContext';
import { NAV_MODULES, NAV_PERMISSIONS } from '../../utils/config';
import styles from './AppLayout.module.css';

const MODULE_LABEL: Record<string, string> = {};
for (const m of NAV_MODULES) {
  MODULE_LABEL[m.id] = m.label;
}

function AppContent() {
  const { user, login, logout, isAuth, loading } = useAuth();
  const [active, setActive] = useState('dashboard');
  const [collapsed, setCollapsed] = useState(false);
  const rol = user?.rol ?? '';

  if (loading) {
    return <PageSkeleton />;
  }

  if (!isAuth) {
    return (
      <Suspense fallback={<PageSkeleton />}>
        <Login onLogin={login} />
      </Suspense>
    );
  }

  const allowed = NAV_PERMISSIONS[rol] ?? [];
  const canAccess = (m: string) => allowed.includes(m);
  const moduleLabel = MODULE_LABEL[active] ?? active;
  const breadcrumbs = [{ label: moduleLabel }];

  const renderContent = () => {
    if (!canAccess(active)) {
      return (
        <PageLayout>
          <Card>
            <h2 className={styles.restrictedTitle}>Acceso restringido</h2>
            <p className={styles.restrictedDesc}>
              No tiene permisos para este módulo.
            </p>
          </Card>
        </PageLayout>
      );
    }
    const key = active;
    const Comp = getPageComponent(active);
    return Comp ? <Comp key={key} /> : <Dashboard key="dashboard" />;
  };

  return (
    <div className={styles.layout}>
      <Sidebar
        active={active}
        onNavigate={setActive}
        user={user}
        onLogout={logout}
        collapsed={collapsed}
        onToggle={() => setCollapsed(!collapsed)}
      />
      <div className={styles.main}>
        <Header user={user} breadcrumbs={breadcrumbs} />
        <main className={styles.content}>
          <div className="page-enter" key={active}>
            <Suspense fallback={<PageSkeleton />}>
              {renderContent()}
            </Suspense>
          </div>
        </main>
      </div>
    </div>
  );
}

function getPageComponent(active: string) {
  switch (active) {
    case 'dashboard': return Dashboard;
    case 'expedientes': return ExpedientePage;
    case 'techos': return TechoPresupuestalPage;
    case 'poi': return ActividadPOIPage;
    case 'pap': return NecesidadPAPPage;
    case 'reportes': return ReportesPage;
    case 'notas': return NotaModificatoriaPage;
    case 'usuarios': return UsuariosPage;
    default: return null;
  }
}

export function AppLayout() {
  return (
    <AuthProvider>
      <ToastProvider>
        <AppContent />
      </ToastProvider>
    </AuthProvider>
  );
}
