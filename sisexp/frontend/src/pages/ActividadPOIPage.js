import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useModals } from '../App';
import { client } from '../api/client';

function formatMoney(n) {
  return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
}

const ESTADO_COLOR = {
  'Pendiente': { bg: '#f1f5f9', fg: '#475569' },
  'En Ejecucion': { bg: '#dbeafe', fg: '#1d4ed8' },
  'Ejecutado': { bg: '#dcfce7', fg: '#166534' },
  'Cerrado': { bg: '#f3e8ff', fg: '#6b21a8' }
};

export default function ActividadPOIPage() {
  const { token, user } = useAuth();
  const modals = useModals();
  const isAdmin = user?.rol === 'Administrador';
  const puedeCerrarPAP = ['Administrador', 'Coordinacion'].includes(user?.rol);
  const [techos, setTechos] = useState([]);
  const [selectedTecho, setSelectedTecho] = useState(null);
  const [actividades, setActividades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({ nombre: '', presupuestoAsignado: '', fechaLimite: '', estado: 'Pendiente' });
  const [expandedId, setExpandedId] = useState(null);
  const [necesidades, setNecesidades] = useState([]);

  const loadTechos = useCallback(async () => {
    try {
      const data = await client.get('/techos-presupuestales', token);
      setTechos(data);
      if (data.length > 0 && !selectedTecho) setSelectedTecho(data[0].id);
    } catch (err) { modals.alerta('Error', err.message); }
  }, [token, selectedTecho]);

  useEffect(() => { loadTechos(); }, []);

  const loadActividades = useCallback(async () => {
    if (!selectedTecho) return;
    setLoading(true);
    try {
      const data = await client.get(`/actividades-poi/techo/${selectedTecho}`, token);
      setActividades(data);
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setLoading(false); }
  }, [selectedTecho, token]);

  useEffect(() => { loadActividades(); }, [loadActividades]);

  const techoActual = techos.find(t => t.id === selectedTecho);
  const techoPlanificado = techoActual?.planificado || false;

  const handleFinalizarPAP = async (id, nombre) => {
    const ok = await modals.confirm(
      'Finalizar planificación del PAP',
      `Se validará que la suma de necesidades de "${nombre}" coincida exactamente con su presupuesto. Una vez cerrado, no se podrán modificar los ítems.`
    );
    if (!ok) return;
    try {
      await client.post(`/actividades-poi/${id}/finalizar-pap`, {}, token);
      modals.alerta('PAP planificado y cerrado', 'Las necesidades quedan congeladas para esta actividad.');
      await loadActividades();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const handleDesbloquearPAP = async (id, nombre) => {
    const ok = await modals.confirm(
      'Desbloquear PAP',
      `Se reabrirá la planificación del PAP de "${nombre}". Podrá volver a modificar los ítems.`
    );
    if (!ok) return;
    try {
      await client.post(`/actividades-poi/${id}/desbloquear-pap`, {}, token);
      modals.alerta('PAP desbloqueado', 'Puede volver a editar los ítems de esta actividad.');
      await loadActividades();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const handleSubmit = async () => {
    if (!form.nombre || !form.presupuestoAsignado) return;
    try {
      if (editId) {
        await client.put(`/actividades-poi/${editId}`, form, token);
      } else {
        await client.post(`/actividades-poi/techo/${selectedTecho}`, form, token);
      }
      setShowForm(false); setEditId(null);
      setForm({ nombre: '', presupuestoAsignado: '', fechaLimite: '', estado: 'Pendiente' });
      await loadActividades(); await loadTechos();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const handleDelete = async (id) => {
    const ok = await modals.confirm('Eliminar actividad', '¿Está seguro de eliminar esta actividad?');
    if (!ok) return;
    try {
      await client.del(`/actividades-poi/${id}`, token);
      await loadActividades(); await loadTechos();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const openEdit = (act) => {
    setForm({ nombre: act.nombre, presupuestoAsignado: act.presupuestoAsignado, fechaLimite: act.fechaLimite || '', estado: act.estado });
    setEditId(act.id);
    setShowForm(true);
  };

  const toggleExpand = async (id) => {
    if (expandedId === id) { setExpandedId(null); return; }
    setExpandedId(id);
    try {
      const data = await client.get(`/necesidades-pap/actividad/${id}`, token);
      setNecesidades(data);
    } catch (err) { modals.alerta('Error', err.message); }
  };

  return (
    <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4 }}>Actividades POI</div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            RF-1.2/2.x — POI + PAP: actividades, necesidades, clasificadores y saldos operativos
            {techoPlanificado && <span style={{ marginLeft: 8, fontSize: 10, padding: '2px 8px', borderRadius: 99, background: '#dcfce7', color: '#166534', fontWeight: 700 }}>POI CERRADO — Solo lectura</span>}
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <select className="input" value={selectedTecho || ''} onChange={e => setSelectedTecho(parseInt(e.target.value))} style={{ width: 180 }}>
            {techos.map(t => <option key={t.id} value={t.id}>Techo {t.año}</option>)}
          </select>
          {isAdmin && !techoPlanificado && (
            <button className="btn btn-primary" onClick={() => { setShowForm(true); setEditId(null); setForm({ nombre: '', presupuestoAsignado: '', fechaLimite: '', estado: 'Pendiente' }); }}>+ Nueva actividad</button>
          )}
          {isAdmin && techoPlanificado && (
            <button className="btn btn-primary" disabled style={{ opacity: 0.5, cursor: 'not-allowed' }}>+ Nueva actividad</button>
          )}
        </div>
      </div>

      {techoActual && (
        <div style={{ fontSize: 13, color: '#64748b', marginBottom: 16 }}>
          Disponible: <strong>{formatMoney(parseFloat(techoActual.montoTotal) - parseFloat(techoActual.montoUtilizado))}</strong> de {formatMoney(techoActual.montoTotal)}
        </div>
      )}

      {showForm && (
        <div className="card" style={{ padding: 24, marginBottom: 24, maxWidth: 480 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: '#0f172a', marginBottom: 16 }}>
            {editId ? 'Editar actividad' : 'Nueva actividad'} — Techo {techos.find(t => t.id === selectedTecho)?.año}
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">Nombre de la actividad *</label>
            <input className="input" value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} placeholder="Ej: Adquisición de equipos" />
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">Presupuesto asignado (S/) *</label>
            <input className="input" type="number" step="0.01" value={form.presupuestoAsignado} onChange={e => setForm({ ...form, presupuestoAsignado: e.target.value })} placeholder="45000" />
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">Fecha límite de tramitación</label>
            <input className="input" type="date" value={form.fechaLimite} onChange={e => setForm({ ...form, fechaLimite: e.target.value })} />
          </div>
          {editId && (
            <div style={{ marginBottom: 16 }}>
              <label className="label">Estado</label>
              <select className="input" value={form.estado} onChange={e => setForm({ ...form, estado: e.target.value })}>
                <option value="Pendiente">Pendiente</option>
                <option value="En Ejecucion">En Ejecución</option>
                <option value="Ejecutado">Ejecutado</option>
                <option value="Cerrado">Cerrado</option>
              </select>
            </div>
          )}
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancelar</button>
            <button className="btn btn-primary" onClick={handleSubmit}>{editId ? 'Actualizar' : 'Crear'}</button>
          </div>
        </div>
      )}

      {loading && <div style={{ color: '#94a3b8' }}>Cargando…</div>}

      {!loading && actividades.length === 0 && (
        <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>No hay actividades para este techo</div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {actividades.map(a => {
          const ec = ESTADO_COLOR[a.estado] || { bg: '#f1f5f9', fg: '#475569' };
          const expirada = a.fechaLimite && new Date(a.fechaLimite) < new Date();
          return (
            <div key={a.id} className="card" style={{ padding: 0, overflow: 'hidden', opacity: a.planificado ? 0.85 : 1 }}>
              <div style={{ padding: 16, cursor: 'pointer' }} onClick={() => toggleExpand(a.id)}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <span style={{ fontSize: 13, fontWeight: 700, color: '#2563eb', fontFamily: 'monospace' }}>{a.codigo}</span>
                    <span style={{ fontSize: 14, fontWeight: 600, color: '#0f172a' }}>{a.nombre}</span>
                    <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 99, background: ec.bg, color: ec.fg, fontWeight: 600 }}>{a.estado}</span>
                    {a.planificado && (
                      <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 99, background: '#dcfce7', color: '#166534', fontWeight: 700 }}>PAP CERRADO</span>
                    )}
                    {expirada && a.estado === 'Pendiente' && (
                      <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 99, background: '#fee2e2', color: '#b91c1c', fontWeight: 700 }}>EXTEMPORÁNEO</span>
                    )}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <span style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>{formatMoney(a.presupuestoAsignado)}</span>
                    <div style={{ display: 'flex', gap: 4 }} onClick={e => e.stopPropagation()}>
                      {puedeCerrarPAP && !a.planificado && (
                        <button className="btn btn-success btn-sm" onClick={() => handleFinalizarPAP(a.id, a.nombre)}>🔒 Finalizar PAP</button>
                      )}
                      {isAdmin && a.planificado && (
                        <button className="btn btn-warning btn-sm" onClick={() => handleDesbloquearPAP(a.id, a.nombre)}>🔓 Desbloquear PAP</button>
                      )}
                      {isAdmin && !a.planificado && !techoPlanificado && (
                        <>
                          <button className="btn btn-secondary btn-sm" onClick={() => openEdit(a)}>✎ Editar</button>
                          <button className="btn btn-danger btn-sm" onClick={() => handleDelete(a.id)}>✗ Eliminar</button>
                        </>
                      )}
                      {isAdmin && !a.planificado && techoPlanificado && (
                        <span style={{ fontSize: 10, color: '#94a3b8', marginLeft: 4 }}>POI cerrado</span>
                      )}
                      {isAdmin && a.planificado && (
                        <span style={{ fontSize: 10, color: '#94a3b8' }}>PAP cerrado</span>
                      )}
                    </div>
                  </div>
                </div>
                {a.fechaLimite && (
                  <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 4 }}>
                    Fecha límite: {new Date(a.fechaLimite).toLocaleDateString('es-PE')}
                  </div>
                )}
              </div>
              {expandedId === a.id && (
                <div style={{ borderTop: '1px solid #f1f5f9', padding: 16, background: '#fafbfc' }}>
                  <NecesidadSubList token={token} actividadId={a.id} actividadNombre={a.nombre} planificado={a.planificado} onLoad={loadActividades} />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function NecesidadSubList({ token, actividadId, actividadNombre, planificado }) {
  const { user } = useAuth();
  const modals = useModals();
  const [items, setItems] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({ nombre: '', cantidad: 1, precioEstimado: '', unidad: 'unidad', oficinaLaboratorio: '', tipo: 'Bien', clasificadorGasto: '' });
  const puede = ['Administrador', 'Coordinacion'].includes(user?.rol) && !planificado;

  const load = useCallback(async () => {
    try { setItems(await client.get(`/necesidades-pap/actividad/${actividadId}`, token)); }
    catch (err) { modals.alerta('Error', err.message); }
  }, [actividadId, token]);

  useEffect(() => { load(); }, [load]);

  const handleSubmit = async () => {
    if (!form.nombre || !form.cantidad || !form.precioEstimado) return;
    try {
      if (editId) {
        await client.put(`/necesidades-pap/${editId}`, form, token);
      } else {
        await client.post(`/necesidades-pap/actividad/${actividadId}`, form, token);
      }
      setShowForm(false); setEditId(null);
      setForm({ nombre: '', cantidad: 1, precioEstimado: '', unidad: 'unidad', oficinaLaboratorio: '', tipo: 'Bien', clasificadorGasto: '' });
      await load();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const handleDelete = async (id) => {
    const ok = await modals.confirm('Eliminar necesidad', '¿Está seguro de eliminar esta necesidad del PAP?');
    if (!ok) return;
    try { await client.del(`/necesidades-pap/${id}`, token); await load(); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const totalItems = items.reduce((s, i) => s + (parseInt(i.cantidad) || 0) * (parseFloat(i.precioEstimado) || 0), 0);

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
        <div>
          <span style={{ fontSize: 13, fontWeight: 700, color: '#0f172a' }}>Necesidades PAP</span>
          <span style={{ fontSize: 12, color: '#64748b', marginLeft: 8 }}>— {items.length} ítems — Total: {formatMoney(totalItems)}</span>
        </div>
        {puede && (
          <button className="btn btn-primary btn-sm" onClick={() => { setShowForm(true); setEditId(null); setForm({ nombre: '', cantidad: 1, precioEstimado: '', unidad: 'unidad', oficinaLaboratorio: '', tipo: 'Bien', clasificadorGasto: '' }); }}>+ Añadir ítem</button>
        )}
      </div>

      {showForm && (
        <div style={{ background: '#fff', border: '1px solid #e2e8f0', borderRadius: 8, padding: 16, marginBottom: 12 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>{editId ? 'Editar ítem' : 'Nuevo ítem'} — {actividadNombre}</div>
          <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: 10, marginBottom: 10 }}>
            <div><label className="label">Bien/Servicio *</label><input className="input" value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} placeholder="Ej: Computadoras" /></div>
            <div><label className="label">Cantidad planificada *</label><input className="input" type="number" value={form.cantidad} onChange={e => setForm({ ...form, cantidad: parseInt(e.target.value) || 1 })} /></div>
            <div><label className="label">Precio unit. estim. *</label><input className="input" type="number" step="0.01" value={form.precioEstimado} onChange={e => setForm({ ...form, precioEstimado: e.target.value })} placeholder="3500" /></div>
            <div><label className="label">Unidad</label><input className="input" value={form.unidad} onChange={e => setForm({ ...form, unidad: e.target.value })} placeholder="unidad" /></div>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 10, marginBottom: 12 }}>
            <div><label className="label">Of./Lab. (RF-2.1)</label><input className="input" value={form.oficinaLaboratorio} onChange={e => setForm({ ...form, oficinaLaboratorio: e.target.value })} placeholder="Lab. Biología" /></div>
            <div><label className="label">Tipo</label><select className="input" value={form.tipo} onChange={e => setForm({ ...form, tipo: e.target.value })}><option value="Bien">Bien</option><option value="Servicio">Servicio</option></select></div>
            <div><label className="label">Clasificador Gasto (RF-2.2) *</label><input className="input" value={form.clasificadorGasto} onChange={e => setForm({ ...form, clasificadorGasto: e.target.value })} placeholder="2.3.1.2.1.1" /></div>
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary btn-sm" onClick={() => setShowForm(false)}>Cancelar</button>
            <button className="btn btn-primary btn-sm" onClick={handleSubmit}>{editId ? 'Actualizar' : 'Agregar'}</button>
          </div>
        </div>
      )}

      {items.length === 0 && <div style={{ fontSize: 12, color: '#94a3b8', padding: '8px 0' }}>No hay necesidades registradas para esta actividad.</div>}

      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
        <thead>
          <tr style={{ borderBottom: '1px solid #e2e8f0' }}>
            <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, color: '#475569' }}>Bien/Servicio</th>
            <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, color: '#475569' }}>Clasif.</th>
            <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>Cant.</th>
            <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>P. Unit.</th>
            <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>Subtotal</th>
            <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>Disp.</th>
            <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>Ejec.</th>
            <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>Monto Disp.</th>
            <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, color: '#475569' }}>Of./Lab.</th>
            <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, color: '#475569' }}>Tipo</th>
            {puede && <th style={{ padding: '6px 10px' }}></th>}
          </tr>
        </thead>
        <tbody>
          {items.map(i => {
            const subtotal = (parseInt(i.cantidad) || 0) * (parseFloat(i.precioEstimado) || 0);
            return (
              <tr key={i.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '6px 10px', fontWeight: 500 }}>{i.nombre}</td>
                <td style={{ padding: '6px 10px' }}><code style={{ fontSize: 10, background: '#f1f5f9', padding: '1px 4px', borderRadius: 3 }}>{i.clasificadorGasto || '—'}</code></td>
                <td style={{ padding: '6px 10px', textAlign: 'right' }}>{i.cantidad}</td>
                <td style={{ padding: '6px 10px', textAlign: 'right' }}>{formatMoney(i.precioEstimado)}</td>
                <td style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600 }}>{formatMoney(subtotal)}</td>
                <td style={{ padding: '6px 10px', textAlign: 'right', color: (i.cantidadDisponible || 0) > 0 ? '#166534' : '#b91c1c', fontWeight: 600 }}>{i.cantidadDisponible ?? '—'}</td>
                <td style={{ padding: '6px 10px', textAlign: 'right', color: (i.cantidadEjecutada || 0) > 0 ? '#d97706' : '#94a3b8', fontWeight: 600 }}>{i.cantidadEjecutada || 0}</td>
                <td style={{ padding: '6px 10px', textAlign: 'right', color: (i.montoDisponible || 0) > 0 ? '#166534' : '#b91c1c', fontWeight: 600 }}>{i.montoDisponible != null ? formatMoney(i.montoDisponible) : '—'}</td>
                <td style={{ padding: '6px 10px', color: '#64748b' }}>{i.oficinaLaboratorio || '—'}</td>
                <td style={{ padding: '6px 10px' }}>
                  <span style={{ fontSize: 10, padding: '1px 6px', borderRadius: 99, background: i.tipo === 'Bien' ? '#dbeafe' : '#fef3c7', color: i.tipo === 'Bien' ? '#1d4ed8' : '#92400e', fontWeight: 600 }}>{i.tipo}</span>
                </td>
                {puede && (
                  <td style={{ padding: '6px 10px' }}>
                    <div style={{ display: 'flex', gap: 4 }}>
                      <button className="btn btn-secondary btn-sm" onClick={() => { setEditId(i.id); setForm({ nombre: i.nombre, cantidad: i.cantidad, precioEstimado: i.precioEstimado, unidad: i.unidad || 'unidad', oficinaLaboratorio: i.oficinaLaboratorio || '', tipo: i.tipo, clasificadorGasto: i.clasificadorGasto || '' }); setShowForm(true); }}>✎ Editar</button>
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(i.id)}>✗ Eliminar</button>
                    </div>
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
