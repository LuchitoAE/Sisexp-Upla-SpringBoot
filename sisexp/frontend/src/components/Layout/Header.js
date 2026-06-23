import React, { useEffect, useState, memo } from 'react';
import { ROL_LABEL, ROL_PROFILE, ROL_COLOR } from '../../utils/config';
import { client } from '../../api/client';
import { useAuth } from '../../contexts/AuthContext';

export default memo(function Header({ user }) {
  const { token } = useAuth();
  const profile = ROL_PROFILE[user?.rol] || {};
  const color = profile.color || ROL_COLOR[user?.rol] || '#2563eb';
  const [notifCount, setNotifCount] = useState(0);
  const [notifs, setNotifs] = useState([]);
  const [showNotifs, setShowNotifs] = useState(false);

  useEffect(() => {
    const load = () => {
      client.get('/notificaciones/count', token).then(r => setNotifCount(r.count || 0)).catch(() => {});
    };
    load();
    const interval = setInterval(load, 30000);
    return () => clearInterval(interval);
  }, [token]);

  const openNotifs = async () => {
    setShowNotifs(!showNotifs);
    if (!showNotifs) {
      try {
        const data = await client.get('/notificaciones', token);
        setNotifs(data);
      } catch (err) { /* ignore */ }
    }
  };

  const markAll = async () => {
    try { await client.put('/notificaciones/read-all', {}, token); setNotifCount(0); setNotifs(n => n.map(x => ({ ...x, leida: true }))); }
    catch (err) { /* ignore */ }
  };

  const markOne = async (id) => {
    try {
      await client.put(`/notificaciones/${id}/read`, {}, token);
      setNotifCount(c => Math.max(0, c - 1));
      setNotifs(n => n.map(x => x.id === id ? { ...x, leida: true } : x));
    } catch (err) { /* ignore */ }
  };

  const timeAgo = (date) => {
    const mins = Math.floor((Date.now() - new Date(date).getTime()) / 60000);
    if (mins < 1) return 'ahora';
    if (mins < 60) return `${mins}m`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h`;
    return Math.floor(hrs / 24) + 'd';
  };

  const TIPO_ICON = { observacion: '↩', rechazo: '✗', aprobacion: '✓', alerta_fecha: '📅', nota_aprobada: '✅', nota_rechazada: '❌', info: 'ℹ' };

  return (
    <header style={{
      padding: '14px 28px', background: '#fff', borderBottom: '1px solid #f1f5f9',
      display: 'flex', justifyContent: 'space-between', alignItems: 'center',
      flexShrink: 0, boxShadow: '0 1px 2px rgba(0,0,0,0.02)'
    }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 6, height: 6, borderRadius: '50%', background: '#16a34a', boxShadow: '0 0 0 3px rgba(22,163,74,0.12)' }} />
        <span style={{ fontSize: 11, fontWeight: 500, color: '#94a3b8' }}>
          {new Date().toLocaleDateString('es-PE', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })}
        </span>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
        {/* Bell */}
        <div style={{ position: 'relative' }}>
          <button onClick={openNotifs} style={{
            background: 'none', border: 'none', cursor: 'pointer', fontSize: 18, padding: 4,
            position: 'relative', lineHeight: 1
          }}>
            🔔
            {notifCount > 0 && (
              <span style={{
                position: 'absolute', top: -2, right: -4,
                background: '#dc2626', color: '#fff', fontSize: 10, fontWeight: 700,
                borderRadius: '50%', minWidth: 17, height: 17, display: 'flex',
                alignItems: 'center', justifyContent: 'center', padding: '0 3px'
              }}>{notifCount > 9 ? '9+' : notifCount}</span>
            )}
          </button>

          {showNotifs && (
            <div style={{
              position: 'absolute', top: 36, right: 0, width: 380, maxHeight: 450,
              background: '#fff', borderRadius: 14, boxShadow: '0 10px 40px rgba(0,0,0,0.12)',
              border: '1px solid #f1f5f9', zIndex: 100, overflow: 'hidden'
            }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', padding: '12px 16px', borderBottom: '1px solid #f1f5f9' }}>
                <span style={{ fontSize: 13, fontWeight: 700, color: '#0f172a' }}>Notificaciones</span>
                {notifCount > 0 && (
                  <button onClick={markAll} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: 11, fontWeight: 600 }}>Marcar todas leídas</button>
                )}
              </div>
              <div style={{ overflowY: 'auto', maxHeight: 390 }}>
                {notifs.length === 0 && (
                  <div style={{ padding: 24, textAlign: 'center', color: '#94a3b8', fontSize: 12 }}>Sin notificaciones</div>
                )}
                {notifs.map(n => (
                  <div key={n.id} onClick={() => !n.leida && markOne(n.id)} style={{
                    padding: '10px 16px', cursor: n.leida ? 'default' : 'pointer',
                    borderBottom: '1px solid #f1f5f9',
                    background: n.leida ? '#fff' : '#f8fafc',
                    opacity: n.leida ? 0.75 : 1, transition: 'background 0.15s'
                  }}>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start' }}>
                      <span style={{ fontSize: 14, flexShrink: 0 }}>{TIPO_ICON[n.tipo] || '•'}</span>
                      <div style={{ flex: 1 }}>
                        <div style={{ fontSize: 12, color: '#334155', lineHeight: 1.5 }}>{n.mensaje}</div>
                        <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 3 }}>{timeAgo(n.createdAt)}</div>
                      </div>
                      {!n.leida && <div style={{ width: 7, height: 7, borderRadius: '50%', background: '#3b82f6', flexShrink: 0, marginTop: 4 }} />}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>

        {/* User */}
        <div style={{ textAlign: 'right' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a' }}>{user?.nombre || 'SISEXP-UPLA'}</div>
          <div style={{ fontSize: 11, color: '#94a3b8', fontWeight: 500 }}>{ROL_LABEL[user?.rol] || user?.rol}</div>
        </div>
        <div style={{
          width: 34, height: 34, borderRadius: '50%',
          background: `linear-gradient(135deg, ${color}, ${color}dd)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 12, fontWeight: 800, color: '#fff',
          boxShadow: `0 2px 8px ${color}40`
        }}>
          {(user?.nombre || '?').split(' ').map(s => s[0]).slice(0, 2).join('')}
        </div>
      </div>
    </header>
  );
});
