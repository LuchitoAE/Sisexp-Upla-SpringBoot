import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useModals } from '../App';
import { client } from '../api/client';

function formatMoney(n) {
  return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
}

const PAGE_SIZE = 10;

export default function TechoPresupuestalPage() {
  const { token, user } = useAuth();
  const modals = useModals();
  const [techos, setTechos] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [page, setPage] = useState(1);
  const [form, setForm] = useState({ año: new Date().getFullYear(), montoTotal: '' });
  const [editId, setEditId] = useState(null);

  const isAdmin = user?.rol === 'Administrador';
  const maxAño = techos.length > 0 ? Math.max(...techos.map(t => t.año)) : new Date().getFullYear() - 1;
  const nextAño = maxAño + 1;

  const sortedTechos = [...techos].sort((a, b) => b.año - a.año);
  const totalPages = Math.max(1, Math.ceil(sortedTechos.length / PAGE_SIZE));
  const paginatedTechos = sortedTechos.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await client.get('/techos-presupuestales', token);
      setTechos(data);
      setPage(1);
    }     catch (err) { modals.alerta('Error', err.message); }
    finally { setLoading(false); }
  }, [token]);

  useEffect(() => { load(); }, [load]);

  const handleSubmit = async () => {
    if (!form.año || !form.montoTotal) return;
    try {
      if (editId) {
        await client.put(`/techos-presupuestales/${editId}`, { montoTotal: form.montoTotal }, token);
      } else {
        await client.post('/techos-presupuestales', form, token);
      }
      setShowForm(false);
      setEditId(null);
      setForm({ año: nextAño, montoTotal: '' });
      await load();
    }     catch (err) { modals.alerta('Error', err.message); }
  };

  const handleToggleActivo = async (t) => {
    const ok = await modals.confirm(
      t.activo ? 'Desactivar techo' : 'Activar techo',
      `${t.activo ? '¿Desactivar' : '¿Activar'} el techo ${t.año}? ${t.activo ? 'No podrá usarse en nuevas operaciones.' : 'Volverá a estar disponible.'}`
    );
    if (!ok) return;
    try { await client.patch(`/techos-presupuestales/${t.id}/toggle-activo`, {}, token); await load(); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const handleFinalizarPOI = async (id) => {
    const ok = await modals.confirm(
      'Finalizar planificación del POI',
      'Se validará que la suma de las actividades coincida exactamente con el techo presupuestal. Una vez cerrado, no se podrán modificar actividades ni crear nuevas.'
    );
    if (!ok) return;
    try {
      await client.post(`/techos-presupuestales/${id}/finalizar-poi`, {}, token);
      modals.alerta('POI planificado y cerrado', 'Las actividades quedan congeladas.');
      await load();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const handleDesbloquearPOI = async (id) => {
    const ok = await modals.confirm(
      'Desbloquear POI',
      'Se reabrirá la planificación del POI. Podrá volver a modificar actividades. Si ya hay expedientes en curso, podrían verse afectados.'
    );
    if (!ok) return;
    try {
      await client.post(`/techos-presupuestales/${id}/desbloquear-poi`, {}, token);
      modals.alerta('POI desbloqueado', 'Puede volver a editar las actividades.');
      await load();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const openEdit = (t) => {
    setForm({ año: t.año, montoTotal: t.montoTotal });
    setEditId(t.id);
    setShowForm(true);
  };

  return (
    <div style={{ padding: 28, maxWidth: 900, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4 }}>
            Techo Presupuestal
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            RF-1.1 — Registro del presupuesto total anual de la facultad
          </div>
        </div>
        {isAdmin && !techos.some(t => t.año === nextAño) && (
          <button className="btn btn-primary" onClick={() => { setShowForm(true); setEditId(null); setForm({ año: nextAño, montoTotal: '' }); }}>
            + Nuevo techo {nextAño}
          </button>
        )}
      </div>

      {showForm && (
        <div className="card" style={{ padding: 24, marginBottom: 24, maxWidth: 480 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: '#0f172a', marginBottom: 16 }}>
            {editId ? 'Editar techo presupuestal' : 'Nuevo techo presupuestal'}
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">Año</label>
            <input className="input" type="number" value={form.año} disabled readOnly />
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">Monto total (S/)</label>
            <input className="input" type="number" step="0.01" value={form.montoTotal} onChange={e => setForm({ ...form, montoTotal: e.target.value })} placeholder="115000" />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancelar</button>
            <button className="btn btn-primary" onClick={handleSubmit}>{editId ? 'Actualizar' : 'Crear'}</button>
          </div>
        </div>
      )}

      {loading && <div style={{ color: '#94a3b8' }}>Cargando…</div>}

      {!loading && techos.length === 0 && (
        <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>No hay techos presupuestales registrados</div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
        {paginatedTechos.map(t => {
          const usado = parseFloat(t.montoUtilizado);
          const total = parseFloat(t.montoTotal);
          const pct = total > 0 ? Math.round((usado / total) * 100) : 0;
          const disponible = total - usado;
          const inactive = t.activo === false;
          return (
            <div key={t.id} className="card" style={{ padding: 20, opacity: inactive ? 0.5 : t.planificado ? 0.85 : 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a' }}>
                    Techo {t.año}
                  </div>
                  {inactive && (
                    <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 99, background: '#fee2e2', color: '#b91c1c', fontWeight: 700 }}>INACTIVO</span>
                  )}
                  {t.planificado && (
                    <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 99, background: '#dcfce7', color: '#166534', fontWeight: 700 }}>POI CERRADO</span>
                  )}
                  {!t.planificado && !inactive && (
                    <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 99, background: '#fef3c7', color: '#92400e', fontWeight: 700 }}>EN PLANIFICACIÓN</span>
                  )}
                </div>
                <div style={{ display: 'flex', gap: 6 }}>
                  {isAdmin && !inactive && (
                    <button onClick={() => handleToggleActivo(t)}
                      className="btn btn-sm"
                      style={{ background: '#dcfce7', color: '#166534', border: 'none', fontSize: 11 }}>
                      Activo
                    </button>
                  )}
                  {isAdmin && inactive && (
                    <button onClick={() => handleToggleActivo(t)}
                      className="btn btn-sm"
                      style={{ background: '#fee2e2', color: '#b91c1c', border: 'none', fontSize: 11 }}>
                      Inactivo
                    </button>
                  )}
                  {isAdmin && !t.planificado && !inactive && (
                    <button className="btn btn-secondary btn-sm" onClick={() => openEdit(t)}>✎ Editar</button>
                  )}
                  {isAdmin && !t.planificado && !inactive && (
                    <button className="btn btn-success btn-sm" onClick={() => handleFinalizarPOI(t.id)}>
                      🔒 Finalizar POI
                    </button>
                  )}
                  {isAdmin && t.planificado && !inactive && (
                    <button className="btn btn-warning btn-sm" onClick={() => handleDesbloquearPOI(t.id)}>
                      🔓 Desbloquear POI
                    </button>
                  )}
                </div>
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 16, marginBottom: 12 }}>
                <div>
                  <div style={{ fontSize: 11, color: '#64748b', fontWeight: 600 }}>TOTAL</div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: '#0f172a' }}>{formatMoney(total)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#64748b', fontWeight: 600 }}>UTILIZADO</div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: usado > total ? '#dc2626' : '#2563eb' }}>{formatMoney(usado)}</div>
                </div>
                <div>
                  <div style={{ fontSize: 11, color: '#64748b', fontWeight: 600 }}>DISPONIBLE</div>
                  <div style={{ fontSize: 18, fontWeight: 800, color: disponible < 0 ? '#dc2626' : '#16a34a' }}>{formatMoney(disponible)}</div>
                </div>
              </div>
              <div style={{ background: '#f1f5f9', borderRadius: 8, height: 8, overflow: 'hidden' }}>
                <div style={{ width: pct + '%', height: '100%', background: pct > 90 ? '#dc2626' : pct > 70 ? '#d97706' : '#2563eb', borderRadius: 8, transition: 'width 0.3s' }} />
              </div>
              <div style={{ fontSize: 11, color: '#64748b', marginTop: 4, textAlign: 'right' }}>{pct}% utilizado</div>
            </div>
          );
        })}
      </div>

      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 20 }}>
          <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page <= 1}>
            ← Anterior
          </button>
          <span style={{ fontSize: 12, color: '#64748b' }}>
            Pág {page} de {totalPages}
          </span>
          <button className="btn btn-secondary btn-sm" onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page >= totalPages}>
            Siguiente →
          </button>
        </div>
      )}
    </div>
  );
}
