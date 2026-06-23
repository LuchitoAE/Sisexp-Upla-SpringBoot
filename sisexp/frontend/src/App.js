import React, { useState, createContext, useContext, useCallback, Suspense, lazy, memo } from 'react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import Login from './components/Auth/Login';
import Sidebar from './components/Layout/Sidebar';
import Header from './components/Layout/Header';
import Placeholder from './components/Common/Placeholder';
import { NAV_PERMISSIONS } from './utils/config';
import './App.css';

// ─── Lazy loading de paginas (code splitting automatico) ───
// Cada pagina se descarga bajo demanda cuando el usuario navega a ella
const Dashboard          = lazy(() => import('./pages/Dashboard'));
const UsuariosPage       = lazy(() => import('./pages/UsuariosPage'));
const TechoPresupuestalPage = lazy(() => import('./pages/TechoPresupuestalPage'));
const ActividadPOIPage   = lazy(() => import('./pages/ActividadPOIPage'));
const NecesidadPAPPage   = lazy(() => import('./pages/NecesidadPAPPage'));
const ReportesPage       = lazy(() => import('./pages/ReportesPage'));
const NotaModificatoriaPage = lazy(() => import('./pages/NotaModificatoriaPage'));
const ExpedientePage     = lazy(() => import('./pages/ExpedientePage'));

// ─── Skeleton de carga (se muestra mientras la pagina se descarga) ───
function PageSkeleton() {
  return (
    <div style={{ padding: '24px 28px', maxWidth: 1300 }}>
      <div style={{ display: 'flex', gap: 12, marginBottom: 18 }}>
        <div className="skeleton" style={{ width: 200, height: 28, borderRadius: 6 }} />
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))', gap: 12 }}>
        {[1,2,3,4,5,6].map(i => (
          <div key={i} className="skeleton" style={{ height: 100, borderRadius: 12 }} />
        ))}
      </div>
    </div>
  );
}

// ─── Modal Context ───
const ModalContext = createContext(null);
export function useModals() { return useContext(ModalContext); }

function ModalProvider({ children }) {
  const [modal, setModal] = useState({ open: false, type: '', title: '', message: '', resolve: null, input: false, label: '' });
  const [inputValue, setInputValue] = useState('');

  const confirm = useCallback((title, message) => new Promise(resolve => {
    setModal({ open: true, type: 'confirm', title, message, resolve, input: false, label: '' });
  }), []);

  const promptText = useCallback((title, message, label) => new Promise(resolve => {
    setModal({ open: true, type: 'confirm', title, message, resolve, input: true, label: label || '' });
    setInputValue('');
  }), []);

  const alerta = useCallback((title, message) => {
    setModal({ open: true, type: 'alert', title, message, resolve: null, input: false, label: '' });
  }, []);

  const close = (result) => {
    if (modal.resolve) modal.resolve(result);
    setModal({ ...modal, open: false });
  };

  return (
    <ModalContext.Provider value={{ confirm, promptText, alerta }}>
      {children}
      {modal.open && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(15,23,42,0.5)', backdropFilter: 'blur(4px)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999 }}>
          <div style={{ background: '#fff', borderRadius: 18, padding: '28px 32px 22px', width: '100%', maxWidth: 440, boxShadow: '0 25px 50px rgba(0,0,0,0.15)' }} onClick={e => e.stopPropagation()}>
            {modal.type === 'confirm' && (
              <>
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
                  <span style={{ fontSize: 28, flexShrink: 0 }}>{modal.input ? '✏' : '⚠'}</span>
                  <div>
                    <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>{modal.title}</div>
                    <div style={{ fontSize: 13, color: '#475569', lineHeight: 1.5, marginTop: 4 }}>{modal.message}</div>
                  </div>
                </div>
                {modal.input && (
                  <>
                    {modal.label && <div style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>{modal.label}</div>}
                    <input style={{ width: '100%', padding: '10px 14px', borderRadius: 10, border: '1.5px solid #cbd5e1', fontSize: 13, fontFamily: 'Inter, sans-serif', outline: 'none', marginBottom: 16, color: '#1e293b' }}
                      value={inputValue} onChange={e => setInputValue(e.target.value)}
                      placeholder="Escriba aquí..." autoFocus
                      onFocus={e => { e.target.style.borderColor = '#3b82f6'; e.target.style.boxShadow = '0 0 0 3px rgba(59,130,246,0.10)'; }}
                    />
                  </>
                )}
                <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
                  <button className="btn btn-secondary btn-sm" onClick={() => close(modal.input ? null : false)}>Cancelar</button>
                  <button className="btn btn-primary btn-sm" onClick={() => close(modal.input ? inputValue : true)} disabled={modal.input && !inputValue}>
                    {modal.input ? 'Confirmar' : 'Aceptar'}
                  </button>
                </div>
              </>
            )}
            {modal.type === 'alert' && (
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 36, marginBottom: 8 }}>✓</div>
                <div style={{ fontSize: 16, fontWeight: 700, color: '#16a34a', marginBottom: 4 }}>{modal.title}</div>
                <div style={{ fontSize: 13, color: '#64748b', lineHeight: 1.5, marginBottom: 16 }}>{modal.message}</div>
                <button className="btn btn-primary btn-sm" onClick={() => close()}>Entendido</button>
              </div>
            )}
          </div>
        </div>
      )}
    </ModalContext.Provider>
  );
}

function AppContent() {
  const { user, login, logout, isAuth } = useAuth();
  const [active, setActive] = useState('dashboard');
  const [collapsed, setCollapsed] = useState(false);

  if (!isAuth) {
    return <Login onLogin={login} />;
  }

  const allowed = NAV_PERMISSIONS[user?.rol] || [];
  const canAccess = (m) => allowed.includes(m);

  const renderContent = () => {
    if (!canAccess(active)) {
      return <Placeholder title="Acceso restringido" description="No tiene permisos para este módulo." />;
    }
    switch (active) {
      case 'dashboard': return <Dashboard />;
      case 'expedientes': return <ExpedientePage />;
      case 'poi': return <ActividadPOIPage />;
      case 'pap': return <NecesidadPAPPage />;
      case 'techos': return <TechoPresupuestalPage />;
      case 'reportes': return <ReportesPage />;
      case 'notas': return <NotaModificatoriaPage />;
      case 'usuarios': return <UsuariosPage />;
      default: return <Dashboard />;
    }
  };

  return (
    <div style={{ display: 'flex', height: '100vh', overflow: 'hidden' }}>
      <Sidebar
        active={active}
        onNavigate={setActive}
        user={user}
        onLogout={logout}
        collapsed={collapsed}
        onToggle={() => setCollapsed(!collapsed)}
      />
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
        <Header user={user} />
        <main style={{ flex: 1, overflowY: 'auto', background: '#f8fafc' }}>
          <div className="animate-in" key={active}>
            <Suspense fallback={<PageSkeleton />}>
              {renderContent()}
            </Suspense>
          </div>
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <ModalProvider>
        <AppContent />
      </ModalProvider>
    </AuthProvider>
  );
}
