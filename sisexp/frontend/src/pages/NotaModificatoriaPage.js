import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useModals } from '../App';
import { client, API_URL } from '../api/client';

function formatMoney(n) { return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 }); }

const ESTADO_COLOR = { pendiente: { bg: '#fef3c7', fg: '#92400e' }, configurada: { bg: '#dcfce7', fg: '#166534' }, rechazada: { bg: '#fee2e2', fg: '#b91c1c' } };

export default function NotaModificatoriaPage({ embedded, onSuccess }) {
  const { token, user } = useAuth();
  const modals = useModals();
  const esAdmin = ['Administrador', 'Coordinacion'].includes(user?.rol);
  const [notas, setNotas] = useState([]);
  const [actividades, setActividades] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(embedded || false);
  const [archivo, setArchivo] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  // Config state for admin
  const [configId, setConfigId] = useState(null);
  const [configForm, setConfigForm] = useState({ actividadOrigenId: '', montoTransferir: '', nuevoClasificadorGasto: '', nuevoTipo: 'Servicio' });
  const [configNotaTipo, setConfigNotaTipo] = useState('');
  const necesitaOrigen = configNotaTipo === 'inclusion_actividad';
  const [configSubmitting, setConfigSubmitting] = useState(false);

  const [form, setForm] = useState({
    tipo: 'inclusion_item', actividadExistenteId: '', origen: '',
    nuevoNombre: '', justificacion: '', costoEstimadoReferencial: ''
  });

  const loadActividades = useCallback(async () => {
    try {
      const techos = await client.get('/techos-presupuestales', token);
      let all = [];
      for (const t of techos) {
        const acts = await client.get(`/actividades-poi/techo/${t.id}`, token);
        all = [...all, ...acts];
      }
      setActividades(all);
    } catch (err) { /* ignore */ }
  }, [token]);

  const loadNotas = useCallback(async () => {
    setLoading(true);
    try { setNotas(await client.get('/notas-modificatorias', token)); }
    catch (err) { modals.alerta('Error', err.message); }
    finally { setLoading(false); }
  }, [token]);

  useEffect(() => { loadNotas(); loadActividades(); }, []); // eslint-disable-line

  const resetForm = () => setForm({ tipo: 'inclusion_item', actividadExistenteId: '', origen: '', nuevoNombre: '', justificacion: '', costoEstimadoReferencial: '' });

  const handleSubmitLab = async () => {
    if (!form.nuevoNombre || !form.justificacion) return;
    setSubmitting(true);
    try {
      const fd = new FormData();
      fd.append('tipo', form.tipo);
      fd.append('origen', form.origen || '');
      if (form.tipo === 'inclusion_item') fd.append('actividadExistenteId', form.actividadExistenteId || '');
      fd.append('nuevoNombre', form.nuevoNombre);
      fd.append('justificacion', form.justificacion);
      fd.append('costoEstimadoReferencial', form.costoEstimadoReferencial || '0');
      if (archivo) fd.append('archivo', archivo);
      const res = await fetch(API_URL + '/notas-modificatorias', {
        method: 'POST', credentials: 'include', body: fd
      });
      if (!res.ok) throw new Error((await res.json()).error || 'Error al enviar');
      modals.alerta('Solicitud enviada', 'Su solicitud ha sido registrada. Será revisada por Planificación.');
      setShowForm(false); resetForm(); setArchivo(null); loadNotas();
      if (onSuccess) onSuccess();
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setSubmitting(false); }
  };

  const handleConfigurar = async () => {
    if (!configForm.montoTransferir || parseFloat(configForm.montoTransferir) <= 0) return;
    if (necesitaOrigen && !configForm.actividadOrigenId) return;
    setConfigSubmitting(true);
    try {
      await client.put(`/notas-modificatorias/${configId}/configurar`, {
        actividadOrigenId: necesitaOrigen ? parseInt(configForm.actividadOrigenId) : null,
        montoTransferir: parseFloat(configForm.montoTransferir),
        nuevoClasificadorGasto: configForm.nuevoClasificadorGasto,
        nuevoTipo: configForm.nuevoTipo
      }, token);
      modals.alerta('Solicitud configurada y aprobada', 'Los cambios se aplicaron en las tablas POI/PAP.');
      setConfigId(null); setConfigForm({ actividadOrigenId: '', montoTransferir: '', nuevoClasificadorGasto: '', nuevoTipo: 'Servicio' });
      loadNotas(); loadActividades();
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setConfigSubmitting(false); }
  };

  const handleRechazar = async (id) => {
    const obs = await modals.promptText('Rechazar solicitud', 'Describa el motivo del rechazo para que el solicitante pueda corregir su petición.', 'Motivo:');
    if (!obs) return;
    try { await client.put(`/notas-modificatorias/${id}/rechazar`, { observacion: obs }, token); loadNotas(); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const notaActual = notas.find(n => n.id === configId);
  const actividadConfigDestino = actividades.find(a => a.id === parseInt(notaActual?.actividadExistenteId));
  const actividadConfigOrigen = actividades.find(a => a.id === parseInt(configForm.actividadOrigenId));
  const dispConfig = configNotaTipo === 'inclusion_actividad' ? (actividadConfigOrigen
    ? parseFloat(actividadConfigOrigen.presupuestoAsignado) - parseFloat(actividadConfigOrigen.saldoEjecutado || 0) - parseFloat(actividadConfigOrigen.saldoComprometido || 0)
    : 0) : (actividadConfigDestino
    ? parseFloat(actividadConfigDestino.presupuestoAsignado) - parseFloat(actividadConfigDestino.saldoEjecutado || 0) - parseFloat(actividadConfigDestino.saldoComprometido || 0)
    : 0);

  return (
    <div style={{ padding: 28, maxWidth: 1000, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4 }}>
            Notas Modificatorias
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            Solicitudes de inclusión de actividades e ítems al POI/PAP
          </div>
        </div>
        {!embedded && !esAdmin && (
          <button className="btn btn-primary" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Ver solicitudes' : '+ Nueva solicitud'}
          </button>
        )}
        {!embedded && esAdmin && (
          <span style={{ fontSize: 12, color: '#64748b', fontWeight: 500 }}>Bandeja de aprobación</span>
        )}
      </div>

      {/* ─── Formulario del Laboratorio ─── */}
      {showForm && !esAdmin && (
        <div className="card animate-in" style={{ padding: 24, marginBottom: 24 }}>
          <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 20 }}>
            📝 Solicitud de Inclusión / Modificación
          </div>

          <div style={{ marginBottom: 16 }}>
            <label className="label">Tipo de inclusión *</label>
            <div style={{ display: 'flex', gap: 10 }}>
              {[
                { id: 'inclusion_item', label: 'Ítem Nuevo en Actividad Existente', desc: 'La actividad existe, agrego un bien/servicio al PAP' },
                { id: 'inclusion_actividad', label: 'Actividad Nueva (desde cero)', desc: 'No existe la actividad ni el ítem en el POI' }
              ].map(op => (
                <label key={op.id} onClick={() => setForm({ ...form, tipo: op.id })} style={{
                  flex: 1, padding: '12px 14px', borderRadius: 10, cursor: 'pointer',
                  border: `2px solid ${form.tipo === op.id ? '#2563eb' : '#e2e8f0'}`,
                  background: form.tipo === op.id ? '#eff6ff' : '#fff'
                }}>
                  <div style={{ fontSize: 13, fontWeight: 600, color: form.tipo === op.id ? '#2563eb' : '#334155' }}>{op.label}</div>
                  <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>{op.desc}</div>
                </label>
              ))}
            </div>
          </div>

          <div style={{ marginBottom: 14 }}>
            <label className="label">Origen (laboratorio u oficina solicitante)</label>
            <input className="input" value={form.origen} onChange={e => setForm({ ...form, origen: e.target.value })}
              placeholder="Ej: Laboratorio de Mecánica de Fluidos" />
          </div>

          {form.tipo === 'inclusion_item' && (
            <div style={{ marginBottom: 14 }}>
              <label className="label">Actividad POI donde se añade el ítem *</label>
              <select className="input" value={form.actividadExistenteId} onChange={e => setForm({ ...form, actividadExistenteId: e.target.value })}>
                <option value="">Seleccionar actividad...</option>
                {actividades.map(a => <option key={a.id} value={a.id}>{a.codigo} — {a.nombre}</option>)}
              </select>
            </div>
          )}

          <div style={{ marginBottom: 14 }}>
            <label className="label">Nombre de la {form.tipo === 'inclusion_actividad' ? 'actividad' : 'necesidad'} a crear *</label>
            <input className="input" value={form.nuevoNombre} onChange={e => setForm({ ...form, nuevoNombre: e.target.value })}
              placeholder="Ej: Mantenimiento de urgencia del compresor de aire hidráulico" />
          </div>

          <div style={{ marginBottom: 16 }}>
            <label className="label">Justificación de la urgencia *</label>
            <textarea className="input" rows={4} value={form.justificacion} onChange={e => setForm({ ...form, justificacion: e.target.value })}
              placeholder="Explique por qué esto es indispensable y por qué no se previó a inicio de año..." />
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 16 }}>
            <div>
              <label className="label">Costo estimado referencial (S/)</label>
              <input className="input" type="number" step="0.01" value={form.costoEstimadoReferencial}
                onChange={e => setForm({ ...form, costoEstimadoReferencial: e.target.value })}
                placeholder="4500" />
              <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 2 }}>Solo referencia. Planificación asigna el presupuesto real.</div>
            </div>
            <div>
              <label className="label">Archivo de sustento (PDF)</label>
              <input type="file" accept=".pdf" onChange={e => setArchivo(e.target.files[0] || null)}
                style={{ width: '100%', padding: '9px 12px', borderRadius: 10, border: '1.5px solid #e2e8f0', fontSize: 12, fontFamily: 'Inter, sans-serif' }} />
              <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 2 }}>Informe técnico o cotización preliminar (opcional).</div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => { setShowForm(false); if (onSuccess) onSuccess(); }}>Cancelar</button>
            <button className="btn btn-primary" onClick={handleSubmitLab}
              disabled={!form.nuevoNombre || !form.justificacion || submitting}>
              {submitting ? 'Enviando...' : 'Enviar Solicitud'}
            </button>
          </div>
        </div>
      )}

      {/* ─── Bandeja ─── */}
      {!showForm && (
        <>
          {loading && <div style={{ color: '#94a3b8' }}>Cargando...</div>}
          {!loading && notas.length === 0 && (
            <div className="card" style={{ padding: 32, textAlign: 'center', color: '#94a3b8' }}>No hay solicitudes registradas</div>
          )}

          <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
            {notas.map(n => {
              const ec = ESTADO_COLOR[n.estado] || { bg: '#f1f5f9', fg: '#475569' };
              return (
                <div key={n.id} className="card animate-in" style={{ padding: 18 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 10 }}>
                    <div style={{ flex: 1 }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginBottom: 4 }}>
                        <span style={{ fontSize: 12, fontFamily: 'monospace', color: '#2563eb', fontWeight: 700 }}>{n.codigo}</span>
                        <span style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
                          {n.tipo === 'inclusion_actividad' ? '🏗' : '📦'} {n.nuevoNombre}
                        </span>
                        <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 20, background: ec.bg, color: ec.fg, fontWeight: 600 }}>
                          {n.estado === 'configurada' ? 'Aprobada y configurada' : n.estado === 'rechazada' ? 'Rechazada' : 'Pendiente'}
                        </span>
                      </div>
                      <div style={{ fontSize: 12, color: '#475569', lineHeight: 1.5, marginBottom: 6 }}>{n.justificacion}</div>
                      <div style={{ display: 'flex', gap: 16, fontSize: 11, color: '#64748b', flexWrap: 'wrap' }}>
                        <span>📌 {n.origen || n.solicitante?.nombre || '—'}</span>
                        {n.actividadExistente && <span>🎯 Destino: <strong>{n.actividadExistente.codigo}</strong></span>}
                        <span>💰 Ref: {formatMoney(n.costoEstimadoReferencial)}</span>
                        {n.actividadOrigen && <span>💸 Origen: <strong>{n.actividadOrigen.codigo}</strong> ({formatMoney(n.montoTransferir)})</span>}
                        {n.nombreArchivo && (
                          <a href={`${API_URL}/notas-modificatorias/${n.id}/archivo`} target="_blank" rel="noreferrer"
                            style={{ color: '#2563eb', textDecoration: 'underline' }}>
                            📄 Ver PDF
                          </a>
                        )}
                      </div>
                    </div>
                    {n.estado === 'configurada' && (
                      <div style={{ fontSize: 14, fontWeight: 800, color: '#16a34a', textAlign: 'right', flexShrink: 0, marginLeft: 16 }}>
                        {formatMoney(n.montoTransferir)}
                      </div>
                    )}
                  </div>

                  {/* Admin actions */}
                  {esAdmin && n.estado === 'pendiente' && configId !== n.id && (
                    <div style={{ display: 'flex', gap: 6 }}>
                      <button className="btn btn-success btn-sm" onClick={() => { setConfigId(n.id); setConfigNotaTipo(n.tipo); setConfigForm({ actividadOrigenId: '', montoTransferir: n.costoEstimadoReferencial || '', nuevoClasificadorGasto: '', nuevoTipo: 'Servicio' }); }}>
                        ⚙ Configurar y aprobar
                      </button>
                      <button className="btn btn-danger btn-sm" onClick={() => handleRechazar(n.id)}>✗ Rechazar</button>
                    </div>
                  )}

                  {/* Config panel for admin */}
                  {esAdmin && n.estado === 'pendiente' && configId === n.id && (
                    <div style={{ marginTop: 14, padding: 16, background: '#f8fafc', borderRadius: 12, border: '1px solid #e2e8f0' }}>
                      <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>
                        ⚙ Configurar — {n.codigo} ({(n.tipo === 'inclusion_actividad' ? 'Actividad nueva' : 'Ítem nuevo')})
                      </div>
                      <div style={{ fontSize: 12, color: '#64748b', marginBottom: 14 }}>
                        {n.tipo === 'inclusion_actividad' ? 'Seleccione la actividad de la cual quitar presupuesto y asigne el monto.' : 'Asigne el monto del nuevo ítem. Se validará que la actividad destino tenga espacio presupuestal suficiente. No se resta dinero de otras actividades.'}
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: n.tipo === 'inclusion_actividad' ? '1fr 1fr' : '1fr', gap: 12, marginBottom: 12 }}>
                        {n.tipo === 'inclusion_actividad' && (
                        <div>
                          <label className="label">Actividad de origen (quitar presupuesto) *</label>
                          <select className="input" value={configForm.actividadOrigenId} onChange={e => setConfigForm({ ...configForm, actividadOrigenId: e.target.value })}>
                            <option value="">Seleccionar...</option>
                            {actividades.map(a => {
                              const d = parseFloat(a.presupuestoAsignado) - parseFloat(a.saldoEjecutado || 0) - parseFloat(a.saldoComprometido || 0);
                              return <option key={a.id} value={a.id}>{a.codigo} — {a.nombre} (disp. {formatMoney(d)})</option>;
                            })}
                          </select>
                        </div>
                        )}
                        <div>
                          <label className="label">{n.tipo === 'inclusion_actividad' ? 'Monto a transferir (S/) *' : 'Monto del nuevo ítem (S/) *'}</label>
                          <input className="input" type="number" step="0.01" value={configForm.montoTransferir}
                            onChange={e => setConfigForm({ ...configForm, montoTransferir: e.target.value })} />
                          {(configNotaTipo === 'inclusion_actividad' ? actividadConfigOrigen : actividadConfigDestino) && (
                            <div style={{ fontSize: 11, marginTop: 4, color: parseFloat(configForm.montoTransferir) > dispConfig ? '#b91c1c' : '#16a34a', fontWeight: 600 }}>
                              {parseFloat(configForm.montoTransferir) > dispConfig
                                ? `⚠ Excede disponible (${formatMoney(dispConfig)})`
                                : `✓ Disponible en {configNotaTipo === 'inclusion_actividad' ? 'origen' : 'destino'}: ${formatMoney(dispConfig)}`}
                            </div>
                          )}
                        </div>
                      </div>

                      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12, marginBottom: 14 }}>
                        <div>
                          <label className="label">Clasificador de gasto</label>
                          <input className="input" value={configForm.nuevoClasificadorGasto} onChange={e => setConfigForm({ ...configForm, nuevoClasificadorGasto: e.target.value })} placeholder="2.3.1.x.x.x" />
                        </div>
                        <div>
                          <label className="label">Tipo</label>
                          <select className="input" value={configForm.nuevoTipo} onChange={e => setConfigForm({ ...configForm, nuevoTipo: e.target.value })}>
                            <option value="Bien">Bien</option><option value="Servicio">Servicio</option>
                          </select>
                        </div>
                      </div>

                      <div style={{ display: 'flex', gap: 8 }}>
                        <button className="btn btn-secondary btn-sm" onClick={() => setConfigId(null)}>Cancelar</button>
                        <button className="btn btn-success btn-sm" onClick={handleConfigurar}
                          disabled={!configForm.montoTransferir || parseFloat(configForm.montoTransferir) <= 0 || (necesitaOrigen && !configForm.actividadOrigenId) || parseFloat(configForm.montoTransferir) > dispConfig || configSubmitting}>
                          {configSubmitting ? 'Aplicando...' : '✓ Aprobar y ejecutar cambios'}
                        </button>
                      </div>
                    </div>
                  )}

                  {n.estado === 'rechazada' && n.observacionAdmin && (
                    <div style={{ fontSize: 11, color: '#b91c1c', background: '#fef2f2', padding: '6px 10px', borderRadius: 8, marginTop: 6 }}>
                      Rechazada: {n.observacionAdmin}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        </>
      )}
    </div>
  );
}
