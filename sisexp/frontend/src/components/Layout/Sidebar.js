import React, { memo } from 'react';
import { NAV_MODULES, NAV_PERMISSIONS, ROL_COLOR, ROL_LABEL, ROL_PROFILE } from '../../utils/config';
import { client } from '../../api/client';

const MODULE_ICONS = {
  dashboard: '📊', expedientes: '📁', techos: '💰', poi: '📋', pap: '📦', reportes: '📈', usuarios: '👥'
};

export default memo(function Sidebar({ active, onNavigate, user, onLogout, collapsed, onToggle }) {
  const allowed = NAV_PERMISSIONS[user?.rol] || [];
  const visible = NAV_MODULES.filter(m => allowed.includes(m.id));
  const profile = ROL_PROFILE[user?.rol] || {};

  return (
    <aside style={{
      width: collapsed ? 72 : 240,
      background: 'linear-gradient(180deg, #0f172a 0%, #1e293b 100%)',
      color: '#cbd5e1',
      display: 'flex', flexDirection: 'column', flexShrink: 0,
      transition: 'width 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
      overflow: 'hidden', position: 'relative'
    }}>
      {/* Brand */}
      <div style={{
        padding: collapsed ? '20px 12px' : '20px 20px',
        borderBottom: '1px solid rgba(255,255,255,0.06)',
        display: 'flex', alignItems: 'center', gap: 12
      }}>
        <div style={{
          width: 38, height: 38, borderRadius: 12, flexShrink: 0,
          background: 'linear-gradient(135deg, #3b82f6, #6366f1)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 15, fontWeight: 900, color: '#fff',
          boxShadow: '0 4px 12px rgba(59,130,246,0.35)'
        }}>S</div>
        {!collapsed && (
          <div>
            <div style={{ fontSize: 14, fontWeight: 800, color: '#f8fafc', letterSpacing: -0.3, lineHeight: 1.1 }}>
              SISEXP
            </div>
            <div style={{ fontSize: 10, fontWeight: 500, color: '#64748b', letterSpacing: 0.04 }}>
              UPLA v2
            </div>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav style={{ flex: 1, padding: '12px 8px', overflowY: 'auto' }}>
        <div style={{ fontSize: collapsed ? 0 : 9, fontWeight: 700, color: '#475569', letterSpacing: 0.08, textTransform: 'uppercase', padding: collapsed ? 0 : '6px 12px 8px', marginBottom: 4 }}>
          {!collapsed && 'Navegación'}
        </div>
        {visible.map(m => {
          const isActive = active === m.id;
          return (
            <button key={m.id} onClick={() => onNavigate(m.id)}
              title={collapsed ? m.label : undefined}
              style={{
                width: '100%', display: 'flex', alignItems: 'center', gap: 10,
                padding: collapsed ? '12px 0' : '10px 14px', marginBottom: 2,
                borderRadius: 10, border: 'none', cursor: 'pointer', textAlign: 'left',
                background: isActive ? 'rgba(59,130,246,0.18)' : 'transparent',
                color: isActive ? '#93c5fd' : '#94a3b8',
                fontSize: 13, fontWeight: isActive ? 600 : 500,
                transition: 'all 0.15s',
                position: 'relative',
                fontFamily: 'Inter, sans-serif'
              }}
              onMouseEnter={e => {
                if (!isActive) { e.currentTarget.style.background = 'rgba(255,255,255,0.04)'; e.currentTarget.style.color = '#e2e8f0'; }
              }}
              onMouseLeave={e => {
                if (!isActive) { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#94a3b8'; }
              }}
            >
              {/* Active indicator */}
              {isActive && !collapsed && (
                <div style={{ position: 'absolute', left: 0, top: '20%', height: '60%', width: 3, background: '#3b82f6', borderRadius: '0 3px 3px 0' }} />
              )}
              {isActive && collapsed && (
                <div style={{ position: 'absolute', left: 0, top: '20%', height: '60%', width: 3, background: '#3b82f6', borderRadius: '0 3px 3px 0' }} />
              )}
              <span style={{ fontSize: collapsed ? 16 : 14, width: collapsed ? '100%' : 'auto', textAlign: 'center', flexShrink: 0 }}>
                {MODULE_ICONS[m.id] || '·'}
              </span>
              {!collapsed && <span>{m.label}</span>}
            </button>
          );
        })}
      </nav>

      {/* Bottom section */}
      <div style={{
        padding: collapsed ? 8 : '14px 16px',
        borderTop: '1px solid rgba(255,255,255,0.06)',
        background: 'rgba(0,0,0,0.15)'
      }}>
        {!collapsed && (
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10, padding: '2px 0' }}>
            <div style={{
              width: 32, height: 32, borderRadius: '50%', flexShrink: 0,
              background: ROL_COLOR[user?.rol] || '#3b82f6',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 11, fontWeight: 800, color: '#fff',
              boxShadow: `0 2px 8px ${ROL_COLOR[user?.rol] || '#3b82f6'}40`
            }}>
              {(user?.nombre || '?').split(' ').map(s => s[0]).slice(0, 2).join('')}
            </div>
            <div style={{ minWidth: 0, flex: 1 }}>
              <div className="truncate" style={{ fontSize: 12, fontWeight: 700, color: '#f1f5f9' }}>
                {user?.nombre || 'Usuario'}
              </div>
              <div style={{ fontSize: 10, color: '#64748b', fontWeight: 500 }}>
                {profile.label || ROL_LABEL[user?.rol]}
              </div>
            </div>
          </div>
        )}
        {collapsed && (
          <div style={{ textAlign: 'center', marginBottom: 8 }}>
            <div style={{
              width: 30, height: 30, borderRadius: '50%', margin: '0 auto',
              background: ROL_COLOR[user?.rol] || '#3b82f6',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 10, fontWeight: 800, color: '#fff'
            }}>{(user?.nombre || '?')[0]}</div>
          </div>
        )}
        <button onClick={onLogout} style={{
          width: '100%', padding: collapsed ? '8px 4px' : '8px 14px', borderRadius: 8,
          border: 'none', background: 'rgba(239,68,68,0.10)', color: '#fca5a5',
          cursor: 'pointer', fontSize: 12, fontWeight: 600,
          transition: 'all 0.15s', fontFamily: 'Inter, sans-serif',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6
        }}
        onMouseEnter={e => { e.currentTarget.style.background = 'rgba(239,68,68,0.18)'; }}
        onMouseLeave={e => { e.currentTarget.style.background = 'rgba(239,68,68,0.10)'; }}
        >
          <span style={{ fontSize: 13 }}>{collapsed ? '✕' : '🚪'}</span>
          {!collapsed && 'Cerrar sesión'}
        </button>
        {user?.rol === 'Administrador' && !collapsed && (
          <div style={{ marginTop: 4, borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: 4 }}>
            <button onClick={async () => {
              try {
                const blob = await fetch('/api/admin/backup', { credentials: 'include' }).then(r => r.blob());
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url; a.download = 'sisexp_backup.sql'; a.click();
                URL.revokeObjectURL(url);
              } catch (e) { alert('Error al descargar backup: ' + e.message); }
            }} style={{
              width: '100%', padding: '6px 14px', borderRadius: 8, border: 'none',
              background: 'rgba(14,165,233,0.10)', color: '#7dd3fc',
              cursor: 'pointer', fontSize: 11, fontWeight: 600, marginBottom: 3,
              transition: 'all 0.15s', fontFamily: 'Inter, sans-serif',
              display: 'flex', alignItems: 'center', gap: 6
            }}
            onMouseEnter={e => { e.currentTarget.style.background = 'rgba(14,165,233,0.18)'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'rgba(14,165,233,0.10)'; }}
            ><span>💾</span> Descargar backup</button>
            <button onClick={() => {
              const input = document.createElement('input');
              input.type = 'file'; input.accept = '.sql';
              input.onchange = async () => {
                const file = input.files[0];
                if (!file) return;
                if (!window.confirm('Esto REEMPLAZARA toda la base de datos. ¿Continuar?')) return;
                try {
                  const fd = new FormData();
                  fd.append('archivo', file);
                  const r = await fetch('/api/admin/restore', { method: 'POST', credentials: 'include', body: fd });
                  const text = await r.text();
                  if (r.ok) alert('Restauracion completada. La pagina se recargara.');
                  else alert('Error: ' + text);
                  window.location.reload();
                } catch (e) { alert('Error al restaurar: ' + e.message); }
              };
              input.click();
            }} style={{
              width: '100%', padding: '6px 14px', borderRadius: 8, border: 'none',
              background: 'rgba(245,158,11,0.10)', color: '#fcd34d',
              cursor: 'pointer', fontSize: 11, fontWeight: 600,
              transition: 'all 0.15s', fontFamily: 'Inter, sans-serif',
              display: 'flex', alignItems: 'center', gap: 6
            }}
            onMouseEnter={e => { e.currentTarget.style.background = 'rgba(245,158,11,0.18)'; }}
            onMouseLeave={e => { e.currentTarget.style.background = 'rgba(245,158,11,0.10)'; }}
            ><span>📥</span> Cargar backup</button>
          </div>
        )}
        {user?.rol === 'Administrador' && collapsed && (
          <div style={{ marginTop: 4, borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: 4, textAlign: 'center' }}>
            <button onClick={async () => {
              try {
                const blob = await fetch('/api/admin/backup', { credentials: 'include' }).then(r => r.blob());
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url; a.download = 'sisexp_backup.sql'; a.click();
                URL.revokeObjectURL(url);
              } catch (e) { alert('Error'); }
            }} style={{
              background: 'rgba(14,165,233,0.10)', color: '#7dd3fc', border: 'none',
              cursor: 'pointer', fontSize: 13, padding: '4px 0', width: '100%',
              borderRadius: 6, fontFamily: 'Inter, sans-serif', marginBottom: 2
            }} title="Descargar backup">💾</button>
            <button onClick={() => {
              const input = document.createElement('input');
              input.type = 'file'; input.accept = '.sql';
              input.onchange = async () => {
                const file = input.files[0];
                if (!file || !window.confirm('¿Reemplazar base de datos?')) return;
                const fd = new FormData(); fd.append('archivo', file);
                const r = await fetch('/api/admin/restore', { method: 'POST', credentials: 'include', body: fd });
                if (r.ok) { alert('Restaurado.'); window.location.reload(); }
                else { alert('Error: ' + await r.text()); }
              };
              input.click();
            }} style={{
              background: 'rgba(245,158,11,0.10)', color: '#fcd34d', border: 'none',
              cursor: 'pointer', fontSize: 13, padding: '4px 0', width: '100%',
              borderRadius: 6, fontFamily: 'Inter, sans-serif'
            }} title="Cargar backup">📥</button>
          </div>
        )}
        {onToggle && (
          <button onClick={onToggle} style={{
            width: '100%', marginTop: 6, padding: '4px 0', borderRadius: 8,
            border: 'none', background: 'transparent', color: '#475569',
            cursor: 'pointer', fontSize: 13, transition: 'color 0.15s', fontFamily: 'Inter, sans-serif'
          }}
          onMouseEnter={e => { e.currentTarget.style.color = '#94a3b8'; }}
          onMouseLeave={e => { e.currentTarget.style.color = '#475569'; }}
          >
            {collapsed ? '▸' : '◂'}
          </button>
        )}
      </div>
    </aside>
  );
});
