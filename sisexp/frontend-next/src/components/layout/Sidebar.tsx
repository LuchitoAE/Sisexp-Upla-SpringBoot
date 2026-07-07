import { memo } from 'react';
import {
  LayoutDashboard, FileText, Wallet, ClipboardList, Package,
  BarChart3, FileEdit, Users, Download, LogOut, ChevronLeft, ChevronRight,
} from 'lucide-react';
import { NAV_MODULES, NAV_PERMISSIONS, ROL_LABEL, ROL_PROFILE } from '../../utils/config';
import { getInitials } from '../../utils/format';
import type { Usuario } from '../../types';
import styles from './Sidebar.module.css';

interface SidebarProps {
  active: string;
  onNavigate: (id: string) => void;
  user: Usuario | null;
  onLogout: () => void;
  collapsed: boolean;
  onToggle: () => void;
}

const MODULE_ICONS: Record<string, React.ReactNode> = {
  dashboard: <LayoutDashboard size={18} strokeWidth={1.5} />,
  expedientes: <FileText size={18} strokeWidth={1.5} />,
  techos: <Wallet size={18} strokeWidth={1.5} />,
  poi: <ClipboardList size={18} strokeWidth={1.5} />,
  pap: <Package size={18} strokeWidth={1.5} />,
  reportes: <BarChart3 size={18} strokeWidth={1.5} />,
  notas: <FileEdit size={18} strokeWidth={1.5} />,
  usuarios: <Users size={18} strokeWidth={1.5} />,
};

export const Sidebar = memo(function Sidebar({
  active,
  onNavigate,
  user,
  onLogout,
  collapsed,
  onToggle,
}: SidebarProps) {
  const allowed = NAV_PERMISSIONS[user?.rol ?? ''] ?? [];
  const visible = NAV_MODULES.filter((m) => allowed.includes(m.id));
  const profile = ROL_PROFILE[user?.rol ?? ''] ?? { label: '', color: 'var(--color-neutral-500)' };

  const sidebarClass = `${styles.sidebar} ${collapsed ? styles.collapsed : styles.expanded}`;
  const brandClass = `${styles.brand} ${collapsed ? styles.brandCollapsed : styles.brandExpanded}`;
  const footerClass = `${styles.footer} ${collapsed ? styles.footerCollapsed : styles.footerExpanded}`;

  return (
    <aside className={sidebarClass} role="navigation" aria-label="Navegación principal">
      {/* Brand */}
      <div className={brandClass}>
        <div className={styles.logo}>S</div>
        {!collapsed && (
          <div>
            <div className={styles.brandName}>SISEXP</div>
            <div className={styles.brandVersion}>UPLA v2</div>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className={styles.nav}>
        {!collapsed && <div className={styles.navLabel}>Navegación</div>}
        {visible.map((m) => {
          const isActive = active === m.id;
          const itemClass = `${styles.navItem} ${isActive ? styles.navItemActive : ''} ${collapsed ? styles.navItemCollapsed : ''}`;
          const iconClass = `${styles.navItemIcon} ${collapsed ? styles.navItemIconCentered : ''}`;
          return (
            <button
              key={m.id}
              onClick={() => onNavigate(m.id)}
              title={collapsed ? m.label : undefined}
              aria-current={isActive ? 'page' : undefined}
              className={itemClass}
            >
              {isActive && <div className={styles.activeIndicator} />}
              <span className={iconClass}>{MODULE_ICONS[m.id]}</span>
              {!collapsed && <span>{m.label}</span>}
            </button>
          );
        })}
      </nav>

      {/* Admin section */}
      {user?.rol === 'Administrador' && (
        <div className={`${styles.section} ${collapsed ? styles.sectionCollapsed : ''}`}>
          {!collapsed && <div className={styles.sectionLabel}>Administración</div>}
          <button
            onClick={async () => {
              try {
                const blob = await fetch('/api/admin/backup', { credentials: 'include' }).then((r) => {
                  if (!r.ok) throw new Error('Error al descargar');
                  return r.blob();
                });
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = 'sisexp_backup.sql';
                a.click();
                URL.revokeObjectURL(url);
              } catch { console.error('Error al descargar backup'); }
            }}
            title={collapsed ? 'Descargar backup' : undefined}
            className={styles.navItem}
          >
            <span className={`${styles.navItemIcon} ${collapsed ? styles.navItemIconCentered : ''}`}>
              <Download size={collapsed ? 18 : 15} strokeWidth={1.5} />
            </span>
            {!collapsed && 'Descargar backup'}
          </button>
        </div>
      )}

      {/* Footer */}
      <div className={footerClass}>
        {!collapsed ? (
          <div className={styles.userInfo}>
            <div className={styles.avatar}>
              {getInitials(user?.nombre ?? '')}
            </div>
            <div className={styles.userInfoText}>
              <div className={`${styles.userName} truncate`}>{user?.nombre ?? 'Usuario'}</div>
              <div className={styles.userRole}>{profile.label || ROL_LABEL[user?.rol ?? '']}</div>
            </div>
          </div>
        ) : (
          <div className={styles.userInfoCollapsed}>
            <div className={`${styles.avatar} ${styles.avatarCollapsed}`}>
              {(user?.nombre ?? '?')[0]}
            </div>
          </div>
        )}
        <button
          onClick={onLogout}
          className={`${styles.logoutBtn} ${collapsed ? styles.logoutBtnCollapsed : ''}`}
        >
          <LogOut size={collapsed ? 16 : 14} strokeWidth={1.5} />
          {!collapsed && 'Cerrar sesión'}
        </button>
        <button onClick={onToggle} className={styles.toggleBtn}>
          {collapsed ? <ChevronRight size={14} strokeWidth={1.5} /> : <ChevronLeft size={14} strokeWidth={1.5} />}
        </button>
      </div>
    </aside>
  );
});
