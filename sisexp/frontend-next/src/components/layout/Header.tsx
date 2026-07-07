import { memo } from 'react';
import { Bell, Reply, XCircle, CheckCircle2, Calendar, CheckCircle, Info, Circle, ChevronRight } from 'lucide-react';
import { ROL_LABEL, ROL_COLOR } from '../../utils/config';
import { timeAgo, getInitials } from '../../utils/format';
import { useNotifications } from '../../hooks/useNotifications';
import type { Usuario } from '../../types';
import styles from './Header.module.css';

interface HeaderProps {
  user: Usuario | null;
  breadcrumbs?: Array<{ label: string }>;
}

const TIPO_ICON: Record<string, React.ReactNode> = {
  observacion: <Reply size={14} strokeWidth={1.5} />,
  rechazo: <XCircle size={14} strokeWidth={1.5} />,
  aprobacion: <CheckCircle2 size={14} strokeWidth={1.5} />,
  alerta_fecha: <Calendar size={14} strokeWidth={1.5} />,
  nota_aprobada: <CheckCircle size={14} strokeWidth={1.5} />,
  nota_rechazada: <XCircle size={14} strokeWidth={1.5} />,
  info: <Info size={14} strokeWidth={1.5} />,
};

export const Header = memo(function Header({ user, breadcrumbs }: HeaderProps) {
  const color = ROL_COLOR[user?.rol ?? ''] ?? 'var(--color-brand-500)';
  const { count, notifs, showNotifs, openNotifs, markAll, markOne } = useNotifications();

  return (
    <header className={styles.header}>
      <div className={styles.left}>
        <div className={styles.statusDot} />
        <span className={styles.date}>
          {new Date().toLocaleDateString('es-PE', {
            weekday: 'long',
            day: 'numeric',
            month: 'long',
            year: 'numeric',
          })}
        </span>
        {breadcrumbs && breadcrumbs.length > 0 && (
          <>
            <span className={styles.breadcrumbSep}>
              <ChevronRight size={10} strokeWidth={2} />
            </span>
            {breadcrumbs.map((crumb, i) => (
              <span key={i} className={styles.breadcrumbs}>
                <span className={i === breadcrumbs.length - 1 ? styles.breadcrumbActive : ''}>
                  {crumb.label}
                </span>
                {i < breadcrumbs.length - 1 && (
                  <span className={styles.breadcrumbSep}>
                    <ChevronRight size={10} strokeWidth={2} />
                  </span>
                )}
              </span>
            ))}
          </>
        )}
      </div>

      <div className={styles.right}>
        {/* Notification bell */}
        <div className={styles.notifWrapper}>
          <button
            onClick={openNotifs}
            className={styles.notifButton}
            aria-label={`Notificaciones${count > 0 ? `, ${count} sin leer` : ''}`}
          >
            <Bell size={18} strokeWidth={1.5} />
            {count > 0 && (
              <span className={styles.notifBadge}>
                {count > 9 ? '9+' : count}
              </span>
            )}
          </button>

          {showNotifs && (
            <div className={styles.dropdown}>
              <div className={styles.dropdownHeader}>
                <span className={styles.dropdownTitle}>Notificaciones</span>
                {count > 0 && (
                  <button onClick={markAll} className={styles.markAllBtn}>
                    Marcar todas leídas
                  </button>
                )}
              </div>
              <div className={styles.notifList}>
                {notifs.length === 0 && (
                  <div className={styles.notifEmpty}>Sin notificaciones</div>
                )}
                {notifs.map((n) => {
                  const itemClass = `${styles.notifItem} ${n.leida ? styles.notifItemRead : styles.notifItemUnread}`;
                  return (
                    <div
                      key={n.id}
                      onClick={() => !n.leida && markOne(n.id)}
                      onKeyDown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); if (!n.leida) markOne(n.id); } }}
                      role="button"
                      tabIndex={0}
                      className={itemClass}
                    >
                      <span className={styles.notifIcon}>
                        {TIPO_ICON[n.tipo] ?? <Circle size={8} strokeWidth={1.5} />}
                      </span>
                      <div className={styles.notifContent}>
                        <div className={styles.notifMessage}>{n.mensaje}</div>
                        <div className={styles.notifTime}>{timeAgo(n.createdAt)}</div>
                      </div>
                      {!n.leida && <div className={styles.notifUnreadDot} />}
                    </div>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* User info */}
        <div className={styles.userInfo}>
          <div className={styles.userName}>{user?.nombre ?? 'SISEXP-UPLA'}</div>
          <div className={styles.userRole}>{ROL_LABEL[user?.rol ?? ''] ?? user?.rol}</div>
        </div>
        <div
          className={styles.avatar}
          style={{ background: color }}
        >
          {getInitials(user?.nombre ?? '')}
        </div>
      </div>
    </header>
  );
});
