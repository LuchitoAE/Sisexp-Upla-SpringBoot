import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { client, API_URL } from '../api/client';
import { useModals } from '../App';
import NotaModificatoriaPage from './NotaModificatoriaPage';
import { puede } from '../utils/config';

const TIPOS_DOC = ['TDR', 'Especificaciones_Tecnicas', 'Cotizacion', 'Informe_Tecnico'];
const TIPOS_LABEL = { 'TDR': 'TDR', 'Especificaciones_Tecnicas': 'Esp. Técnicas', 'Cotizacion': 'Cotización', 'Informe_Tecnico': 'Informe Técnico' };

const ESTADO_COLOR = {
  'Borrador': { bg: '#f1f5f9', fg: '#475569' },
  'En revision': { bg: '#fef3c7', fg: '#92400e' },
  'Aprobado': { bg: '#dcfce7', fg: '#166534' },
  'Rechazado': { bg: '#fee2e2', fg: '#b91c1c' },
  'Finalizado': { bg: '#f3e8ff', fg: '#6b21a8' },
  'Observado': { bg: '#fff7ed', fg: '#c2410c' },
  'Derivado': { bg: '#e0f2fe', fg: '#0369a1' }
};

function formatMoney(n) {
  return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
}

export default function ExpedientePage() {
  const { token, user } = useAuth();
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [detalle, setDetalle] = useState(null);
  const [techos, setTechos] = useState([]);
  const [selectedTecho, setSelectedTecho] = useState('');
  const [actividades, setActividades] = useState([]);
  const [necesidades, setNecesidades] = useState([]);
  const [form, setForm] = useState({
    actividadPoiId: '', necesidadPapId: '', urgencia: '', naturaleza: '', descripcion: '', cantidadSolicitada: 1
  });
  const [uploading, setUploading] = useState(false);
  const [disponibilidad, setDisponibilidad] = useState(null);
  const [checkingDisp, setCheckingDisp] = useState(false);
  const [showNotaMod, setShowNotaMod] = useState(false);

  const puedeCrear = puede(user?.rol, 'crearExpediente');
  const puedeAprobar = puede(user?.rol, 'aprobarObservar');
  const puedeFinalizar = puede(user?.rol, 'finalizar');
  const puedeDerivar = puede(user?.rol, 'derivar');
  const puedeSubirDoc = puede(user?.rol, 'subirDocumento');
  const puedeVerDerivacion = puede(user?.rol, 'verDerivacion');
  const [timelineOpen, setTimelineOpen] = useState(false);
  const modals = useModals();

  const load = useCallback(async () => {
    try { const data = await client.get('/expedientes', token); setList(data); }
    catch (err) { modals.alerta('Error', err.message); }
    finally { setLoading(false); }
  }, [token, modals]);

  useEffect(() => { load(); }, [load]);

  const loadTechos = useCallback(async () => {
    try { const data = await client.get('/techos-presupuestales', token); setTechos(data); }
    catch (err) { /* ignore */ }
  }, [token]);

  useEffect(() => { loadTechos(); }, [loadTechos]);

  const loadActividades = async (techoId) => {
    if (!techoId) { setActividades([]); return; }
    try { const data = await client.get(`/actividades-poi/techo/${techoId}`, token); setActividades(data); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const loadNecesidades = async (actividadId) => {
    if (!actividadId) { setNecesidades([]); return; }
    try { const data = await client.get(`/necesidades-pap/actividad/${actividadId}`, token); setNecesidades(data); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const handleActividadChange = (actividadId) => {
    setForm({ ...form, actividadPoiId: actividadId, necesidadPapId: '' });
    loadNecesidades(actividadId);
  };

  useEffect(() => {
    if (form.actividadPoiId && form.necesidadPapId) {
      setCheckingDisp(true);
      const qs = `?cantidadSolicitada=${form.cantidadSolicitada || 1}`;
      client.get(`/expedientes/disponibilidad/${form.actividadPoiId}/${form.necesidadPapId}${qs}`, token)
        .then(setDisponibilidad).catch(() => setDisponibilidad(null)).finally(() => setCheckingDisp(false));
    } else { setDisponibilidad(null); }
  }, [form.actividadPoiId, form.necesidadPapId, form.cantidadSolicitada, token]);

  const handleSubmit = async () => {
    if (!form.actividadPoiId || !form.necesidadPapId || !form.urgencia || !form.naturaleza) return;
    try {
      await client.post('/expedientes', {
        ...form,
        naturaleza: form.naturaleza,
        cantidadSolicitada: form.cantidadSolicitada || 1
      }, token);
      setShowForm(false);
      setForm({ actividadPoiId: '', necesidadPapId: '', urgencia: '', naturaleza: '', descripcion: '', cantidadSolicitada: 1 });
      setActividades([]); setNecesidades([]); setSelectedTecho('');
      await load();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const handleUpload = async (file, tipo) => {
    if (!detalle || !file || !tipo) return;
    setUploading(true);
    try {
      await client.upload(`/expedientes/${detalle.id}/documentos`, file, token, 'archivo', { tipo });
      client.invalidarCache('/expedientes');
      await loadDetalle(detalle.id);
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setUploading(false); }
  };

  const loadDetalle = async (id) => {
    try { const data = await client.get(`/expedientes/${id}`, token); setDetalle(data); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const cambiarEstado = async (estado) => {
    let obs = '';
    if (estado === 'Rechazado') { obs = await modals.promptText('Rechazar expediente', '¿Cuál es el motivo del rechazo?', 'Motivo:'); if (!obs) return; }
    if (estado === 'Observado') { obs = await modals.promptText('Observar expediente', 'Describa el detalle de la observación para que el laboratorio pueda corregir.', 'Detalle:'); if (!obs) return; }
    try {
      await client.put(`/expedientes/${detalle.id}/estado`, { estado, observacion: obs }, token);
      await loadDetalle(detalle.id); await load();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const showNewForm = () => {
    setShowForm(true); setDetalle(null);
    setForm({ actividadPoiId: '', necesidadPapId: '', urgencia: '', naturaleza: '', descripcion: '', cantidadSolicitada: 1 });
    setSelectedTecho(''); setActividades([]); setNecesidades([]);
  };

  //--- Form View ---
  if (showForm && !showNotaMod) {
    return (
      <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
        <button onClick={() => setShowForm(false)} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: 13, fontWeight: 600, marginBottom: 16, padding: 0 }}>
          ← Volver a expedientes
        </button>
        <div style={{ fontSize: 20, fontWeight: 800, color: '#0f172a', marginBottom: 20 }}>Nuevo Expediente</div>
        <div className="card" style={{ padding: 24, maxWidth: 600 }}>
          <div style={{ marginBottom: 12 }}>
            <label className="label">1. Seleccionar año del POI</label>
            <select className="input" value={selectedTecho} onChange={e => { setSelectedTecho(e.target.value); loadActividades(e.target.value); }}>
              <option value="">-- Año --</option>
              {techos.map(t => <option key={t.id} value={t.id}>{t.año} (disp. {formatMoney(t.montoTotal - t.montoUtilizado)})</option>)}
            </select>
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">2. Actividad POI *</label>
            <select className="input" value={form.actividadPoiId} onChange={e => handleActividadChange(e.target.value)} disabled={!selectedTecho}>
              <option value="">-- Actividad --</option>
              {actividades.map(a => <option key={a.id} value={a.id}>{a.codigo} — {a.nombre} ({formatMoney(a.presupuestoAsignado)})</option>)}
            </select>
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">3. Ítem PAP (Bien/Servicio) *</label>
            <select className="input" value={form.necesidadPapId} onChange={e => setForm({ ...form, necesidadPapId: e.target.value })} disabled={!form.actividadPoiId}>
              <option value="">-- ítem --</option>
              {necesidades.map(n => {
                const total = n.cantidad * n.precioEstimado;
                const disp = n.cantidadDisponible != null ? ` (disp: ${n.cantidadDisponible})` : '';
                return <option key={n.id} value={n.id}>[{n.tipo}] {n.nombre} x{n.cantidad}${disp} ({formatMoney(total)}) — {n.oficinaLaboratorio || ''}</option>;
              })}
            </select>
            <button type="button" onClick={() => setShowNotaMod(true)} style={{
              background: 'none', border: '1.5px dashed #cbd5e1', borderRadius: 8, padding: '6px 14px',
              cursor: 'pointer', fontSize: 12, color: '#64748b', fontWeight: 500, marginTop: 6,
              transition: 'all 0.15s', width: '100%', textAlign: 'left', fontFamily: 'Inter, sans-serif'
            }}
            onMouseEnter={e => { e.currentTarget.style.borderColor = '#2563eb'; e.currentTarget.style.color = '#2563eb'; e.currentTarget.style.background = '#f8fafc'; }}
            onMouseLeave={e => { e.currentTarget.style.borderColor = '#cbd5e1'; e.currentTarget.style.color = '#64748b'; e.currentTarget.style.background = 'transparent'; }}
            >
              🔍 ¿No encuentra su necesidad? Solicitar Inclusión / Modificación
            </button>
          </div>
          {disponibilidad && (
            <div style={{ marginBottom: 12, padding: '10px 14px', background: '#f0f9ff', borderRadius: 8, border: '1px solid #bae6fd' }}>
              <div style={{ fontSize: 12, fontWeight: 600, color: '#0369a1', marginBottom: 4 }}>Saldo disponible en el PAP</div>
              <div style={{ display: 'flex', gap: 20, fontSize: 12 }}>
                <div><span style={{ color: '#64748b' }}>Ítem: </span><strong>{disponibilidad.necesidad?.nombre}</strong></div>
                <div><span style={{ color: '#64748b' }}>Precio unit.: </span><strong>{formatMoney(disponibilidad.pap?.precioUnitario || 0)}</strong></div>
                <div><span style={{ color: '#64748b' }}>Unidad: </span><strong>{disponibilidad.pap?.unidad || '—'}</strong></div>
              </div>
              <div style={{ display: 'flex', gap: 20, fontSize: 13, marginTop: 6 }}>
                <div>
                  <span style={{ color: '#64748b' }}>Planificadas: </span><strong>{disponibilidad.pap?.cantidadPlanificada}</strong>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>Disponibles: </span>
                  <strong style={{ color: (disponibilidad.pap?.cantidadDisponible || 0) > 0 ? '#166534' : '#b91c1c' }}>
                    {disponibilidad.pap?.cantidadDisponible || 0}
                  </strong>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>Ejecutados: </span>
                  <strong style={{ color: '#d97706' }}>{disponibilidad.pap?.cantidadEjecutada || 0}</strong>
                </div>
              </div>
            </div>
          )}
          <div style={{ marginBottom: 12 }}>
            <label className="label">Cantidad a solicitar *</label>
            <input className="input" type="number" min={1} max={disponibilidad?.pap?.cantidadDisponible || 1}
              value={form.cantidadSolicitada}
              onChange={e => {
                const val = parseInt(e.target.value) || 1;
                const max = disponibilidad?.pap?.cantidadDisponible || 1;
                setForm({ ...form, cantidadSolicitada: Math.min(val, max) });
              }}
              disabled={!disponibilidad} />
            {disponibilidad && form.cantidadSolicitada > disponibilidad.pap?.cantidadDisponible && (
              <div style={{ fontSize: 11, color: '#b91c1c', marginTop: 4, fontWeight: 600 }}>
                No puede solicitar una cantidad mayor al saldo disponible en su PAP (Máx. {disponibilidad.pap?.cantidadDisponible})
              </div>
            )}
            {disponibilidad && (
              <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>
                Costo estimado: <strong>{formatMoney((form.cantidadSolicitada || 1) * (disponibilidad?.pap?.precioUnitario || 0))}</strong>
              </div>
            )}
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">4. Naturaleza del requerimiento (RF-3.4) *</label>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {['Bien', 'Servicio'].map(n => (
                <label key={n} style={{
                  display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', borderRadius: 8,
                  border: `2px solid ${form.naturaleza === n ? '#2563eb' : '#e2e8f0'}`,
                  cursor: 'pointer', fontSize: 13, fontWeight: 500,
                  background: form.naturaleza === n ? '#eff6ff' : '#fff',
                  opacity: disponibilidad?.necesidad?.tipo && disponibilidad.necesidad.tipo !== n ? 0.4 : 1
                }}>
                  <input type="radio" name="naturaleza" checked={form.naturaleza === n}
                    onChange={() => setForm({ ...form, naturaleza: n })} style={{ accentColor: '#2563eb' }} />
                  {n === 'Bien' ? '📦 Bien' : '🔧 Servicio'}
                </label>
              ))}
            </div>
            {disponibilidad?.necesidad?.tipo && (
              <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>
                El ítem PAP seleccionado es de tipo <strong>{disponibilidad.necesidad.tipo}</strong>
                {disponibilidad.necesidad.clasificador && (
                  <span> · Clasificador: <code style={{ background: '#f1f5f9', padding: '1px 4px', borderRadius: 3 }}>{disponibilidad.necesidad.clasificador}</code></span>
                )}
              </div>
            )}
          </div>
          <div style={{ marginBottom: 12 }}>
            <label className="label">5. Urgencia *</label>
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {['Urgente', 'No tan urgente', 'Puede esperar'].map(u => (
                <label key={u} style={{ display: 'flex', alignItems: 'center', gap: 6, padding: '8px 14px', borderRadius: 8, border: `2px solid ${form.urgencia === u ? '#2563eb' : '#e2e8f0'}`, cursor: 'pointer', fontSize: 13, fontWeight: 500, background: form.urgencia === u ? '#eff6ff' : '#fff' }}>
                  <input type="radio" name="urgencia" checked={form.urgencia === u} onChange={() => setForm({ ...form, urgencia: u })} style={{ accentColor: '#2563eb' }} />
                  {u}
                </label>
              ))}
            </div>
          </div>
          <div style={{ marginBottom: 16 }}>
            <label className="label">6. Descripción / Detalle</label>
            <textarea className="input" rows={4} value={form.descripcion} onChange={e => setForm({ ...form, descripcion: e.target.value })} placeholder="Describa el motivo de la solicitud..." />
          </div>
          {checkingDisp && <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 8 }}>Verificando disponibilidad…</div>}

          {disponibilidad && (
            <div style={{ marginBottom: 16 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#0f172a', marginBottom: 6 }}>Verificación de reglas</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px', borderRadius: 6, marginBottom: 4, fontSize: 12, fontWeight: 500,
                background: disponibilidad.fechaLimite.ok ? '#dcfce7' : '#fee2e2',
                color: disponibilidad.fechaLimite.ok ? '#166534' : '#b91c1c'
              }}>
                <span>{disponibilidad.fechaLimite.ok ? '✓' : '✗'}</span>
                <span>{disponibilidad.fechaLimite.ok ? 'Fecha límite vigente' : disponibilidad.fechaLimite.error}</span>
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px', borderRadius: 6, fontSize: 12, fontWeight: 500,
                background: disponibilidad.saldo.ok ? '#dcfce7' : '#fee2e2',
                color: disponibilidad.saldo.ok ? '#166534' : '#b91c1c'
              }}>
                <span>{disponibilidad.saldo.ok ? '✓' : '✗'}</span>
                <span>
                  Costo: {formatMoney(disponibilidad.costo)} |
                  Disponible: {formatMoney(disponibilidad.saldo.disponible || 0)}
                  {disponibilidad.saldo.error ? ` — ${disponibilidad.saldo.error}` : ''}
                </span>
              </div>
              <div style={{ fontSize: 11, color: '#64748b', marginTop: 4 }}>
                Asignado: {formatMoney(disponibilidad.saldo.asignado)} |
                Comprometido: {formatMoney(disponibilidad.saldo.comprometido)} |
                Ejecutado: {formatMoney(disponibilidad.saldo.ejecutado)}
              </div>
            </div>
          )}

          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => setShowForm(false)}>Cancelar</button>
            <button className="btn btn-primary" onClick={handleSubmit}
              disabled={!form.actividadPoiId || !form.necesidadPapId || !form.urgencia || !form.naturaleza || checkingDisp ||
                (disponibilidad && (!disponibilidad.fechaLimite.ok || !disponibilidad.saldo.ok ||
                  (disponibilidad.necesidad?.tipo && disponibilidad.necesidad.tipo !== form.naturaleza) ||
                  (form.cantidadSolicitada > (disponibilidad.pap?.cantidadDisponible || 0))))}>
              Crear Expediente
            </button>
          </div>
        </div>
      </div>
    );
  }

  //--- Nota Modificatoria embedded ---
  if (showNotaMod) {
    return (
      <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
        <button onClick={() => setShowNotaMod(false)} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: 13, fontWeight: 600, marginBottom: 8, padding: 0, fontFamily: 'Inter, sans-serif' }}>
          ← Volver al formulario de expediente
        </button>
        <NotaModificatoriaPage embedded={true} onSuccess={() => setShowNotaMod(false)} />
      </div>
    );
  }


  //--- Detail View ---
  if (detalle) {
    const d = detalle;
    const ec = ESTADO_COLOR[d.estado] || { bg: '#f1f5f9', fg: '#475569' };
    const totalDocs = d.documentos?.length || 0;

    return (
      <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
        <button onClick={() => setDetalle(null)} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: 13, fontWeight: 600, marginBottom: 16, padding: 0 }}>
          ← Volver a expedientes
        </button>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 20 }}>
          <div>
            <div style={{ fontSize: 20, fontWeight: 800, color: '#0f172a', display: 'flex', alignItems: 'center', gap: 8 }}>
              {d.codigo}
              <span style={{ fontSize: 11, padding: '3px 10px', borderRadius: 99, background: ec.bg, color: ec.fg, fontWeight: 600 }}>{d.estado}</span>
            </div>
            <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>{d.descripcion}</div>
          </div>
          {puedeAprobar && d.estado === 'Borrador' && (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              <button className="btn btn-warning btn-sm" onClick={() => cambiarEstado('En revision')}>🔍 Enviar a revisión</button>
              <button className="btn btn-danger btn-sm" onClick={() => cambiarEstado('Rechazado')}>✗ Rechazar</button>
            </div>
          )}
          {puedeAprobar && d.estado === 'En revision' && (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              <button className="btn btn-success btn-sm" onClick={() => cambiarEstado('Aprobado')}>✓ Aprobar</button>
              <button className="btn btn-warning btn-sm" onClick={() => cambiarEstado('Observado')}>↩ Observar</button>
              <button className="btn btn-danger btn-sm" onClick={() => cambiarEstado('Rechazado')}>✗ Rechazar</button>
            </div>
          )}
          {puedeAprobar && d.estado === 'Observado' && (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              <button className="btn btn-warning btn-sm" onClick={() => cambiarEstado('En revision')}>↻ Reenviar a revisión</button>
              <button className="btn btn-danger btn-sm" onClick={() => cambiarEstado('Rechazado')}>✗ Rechazar</button>
            </div>
          )}
          {puedeFinalizar && d.estado === 'Aprobado' && (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              <button className="btn btn-primary btn-sm" onClick={() => cambiarEstado('Finalizado')}>✓ Finalizar</button>
              {puedeDerivar && <button className="btn btn-info btn-sm" onClick={() => cambiarEstado('Derivado')}>📤 Derivar a DGA</button>}
            </div>
          )}
          {d.estado === 'Derivado' && puedeFinalizar && (
            <div style={{ display: 'flex', gap: 6, flexWrap: 'wrap' }}>
              <button className="btn btn-primary btn-sm" onClick={() => cambiarEstado('Finalizado')}>✓ Finalizar</button>
              {puedeVerDerivacion && (
                <a href={`${API_URL}/expedientes/${d.id}/derivacion`} target="_blank" rel="noreferrer">
                  <button className="btn btn-secondary btn-sm">Hoja de Derivación</button>
                </a>
              )}
            </div>
          )}
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 24 }}>
          <div className="card" style={{ padding: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748b', marginBottom: 8 }}>VINCULACIÓN POI</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>{d.actividad?.codigo} — {d.actividad?.nombre}</div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>Presupuesto: {formatMoney(d.actividad?.presupuestoAsignado)}</div>
          </div>
          <div className="card" style={{ padding: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748b', marginBottom: 8 }}>VINCULACIÓN PAP</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>{d.necesidad?.nombre}</div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>
              Solicitado: <strong>{d.cantidadSolicitada || 1} {d.necesidad?.unidad || ''}</strong> × {formatMoney(d.necesidad?.precioEstimado)} =
              <strong style={{ color: '#0f172a' }}> {formatMoney(d.costoEstimado)}</strong>
            </div>
            <div style={{ fontSize: 12, color: '#64748b', marginTop: 4 }}>Planificadas: {d.necesidad?.cantidad}</div>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 16, marginBottom: 24 }}>
          <div className="card" style={{ padding: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748b', marginBottom: 8 }}>URGENCIA</div>
            <span style={{ fontSize: 13, fontWeight: 700, padding: '4px 10px', borderRadius: 99,
              background: d.urgencia === 'Urgente' ? '#fee2e2' : d.urgencia === 'No tan urgente' ? '#fef3c7' : '#f1f5f9',
              color: d.urgencia === 'Urgente' ? '#b91c1c' : d.urgencia === 'No tan urgente' ? '#92400e' : '#475569'
            }}>{d.urgencia}</span>
          </div>
          <div className="card" style={{ padding: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748b', marginBottom: 8 }}>NATURALEZA (RF-3.4)</div>
            <span style={{ fontSize: 13, fontWeight: 700, padding: '4px 10px', borderRadius: 99,
              background: d.naturaleza === 'Bien' ? '#dbeafe' : '#f0fdf4',
              color: d.naturaleza === 'Bien' ? '#1d4ed8' : '#166534'
            }}>{d.naturaleza || '—'}</span>
            {d.necesidad?.clasificadorGasto && (
              <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 4 }}>
                Clasificador: <code style={{ background: '#f1f5f9', padding: '1px 4px', borderRadius: 3 }}>{d.necesidad.clasificadorGasto}</code>
              </div>
            )}
          </div>
          <div className="card" style={{ padding: 16 }}>
            <div style={{ fontSize: 12, fontWeight: 600, color: '#64748b', marginBottom: 8 }}>SOLICITANTE</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#0f172a' }}>{d.solicitante?.nombre}</div>
            <div style={{ fontSize: 12, color: '#64748b' }}>{d.solicitante?.rol}</div>
          </div>
        </div>

        {d.observacion && (
          <div style={{ background: '#fef2f2', color: '#b91c1c', padding: '10px 14px', borderRadius: 8, fontSize: 13, marginBottom: 24 }}>
            <strong>Observación:</strong> {d.observacion}
          </div>
        )}

        {/* Documentos */}
        <div className="card" style={{ padding: 20, marginBottom: 24 }}>
          <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>
            Documentos adjuntos ({totalDocs})
          </div>
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 16 }}>
            {puedeSubirDoc && TIPOS_DOC.map(tipo => (
              <label key={tipo} style={{ cursor: 'pointer' }}>
                <input type="file" accept=".pdf" style={{ display: 'none' }}
                  onChange={e => { if (e.target.files[0]) handleUpload(e.target.files[0], tipo); e.target.value = ''; }} />
                <span style={{ display: 'inline-block', padding: '6px 12px', fontSize: 12, fontWeight: 600, borderRadius: 8, border: '1.5px dashed #2563eb', color: '#2563eb', background: '#eff6ff' }}>
                  + {TIPOS_LABEL[tipo]}
                </span>
              </label>
            ))}
            {!puedeSubirDoc && <div style={{ fontSize: 12, color: '#94a3b8', fontStyle: 'italic' }}>Solo lectura de documentos</div>}
          </div>
          {uploading && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 14px', background: '#eff6ff', borderRadius: 8, marginTop: 8 }}>
              <div style={{ width: 18, height: 18, border: '3px solid #dbeafe', borderTopColor: '#2563eb', borderRadius: '50%', animation: 'spin 0.6s linear infinite' }} />
              <span style={{ fontSize: 12, fontWeight: 600, color: '#2563eb' }}>Subiendo documento...</span>
            </div>
          )}
          {d.documentos?.length > 0 && (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead>
                <tr style={{ borderBottom: '1px solid #e2e8f0' }}>
                  <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, color: '#475569' }}>Tipo</th>
                  <th style={{ padding: '6px 10px', textAlign: 'left', fontWeight: 600, color: '#475569' }}>Archivo</th>
                  <th style={{ padding: '6px 10px', textAlign: 'right', fontWeight: 600, color: '#475569' }}>Tamaño</th>
                  <th style={{ padding: '6px 10px' }}></th>
                </tr>
              </thead>
              <tbody>
                {d.documentos.map(doc => (
                  <tr key={doc.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '6px 10px' }}>
                      <span style={{ fontSize: 10, padding: '1px 6px', borderRadius: 99, background: '#dbeafe', color: '#1d4ed8', fontWeight: 600 }}>{TIPOS_LABEL[doc.tipo] || doc.tipo}</span>
                    </td>
                    <td style={{ padding: '6px 10px' }}>{doc.nombreOriginal}</td>
                    <td style={{ padding: '6px 10px', textAlign: 'right' }}>{doc.tamaño ? (doc.tamaño / 1024).toFixed(0) + ' KB' : '—'}</td>
                    <td style={{ padding: '6px 10px' }}>
                      <a href={`${API_URL}/expedientes/documentos/${doc.id}/download`} target="_blank" rel="noreferrer" style={{ fontSize: 12, color: '#2563eb', textDecoration: 'none' }}>Descargar</a>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Timeline / Historial */}
        <div className="card" style={{ padding: 20 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }} onClick={() => setTimelineOpen(!timelineOpen)}>
            <div style={{ fontSize: 14, fontWeight: 700, color: '#0f172a' }}>
              Historial de seguimiento ({d.logs?.length || 0})
            </div>
            <span style={{ fontSize: 14, color: '#94a3b8' }}>{timelineOpen ? '▲' : '▼'}</span>
          </div>
          {timelineOpen && d.logs?.length > 0 && (
            <div style={{ marginTop: 16 }}>
              {d.logs.map((log, i) => {
                const fecha = new Date(log.createdAt).toLocaleDateString('es-PE', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
                return (
                  <div key={log.id} style={{ display: 'flex', gap: 12, paddingBottom: 12, marginBottom: 12, borderBottom: i < d.logs.length - 1 ? '1px solid #f1f5f9' : 'none' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
                      <div style={{ width: 10, height: 10, borderRadius: '50%', background: '#2563eb', flexShrink: 0 }} />
                      {i < d.logs.length - 1 && <div style={{ width: 1, flex: 1, background: '#e2e8f0', marginTop: 4 }} />}
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontSize: 12, fontWeight: 600, color: '#0f172a' }}>
                        {log.estadoAnterior ? `${log.estadoAnterior} → ` : ''}{log.estadoNuevo}
                      </div>
                      <div style={{ fontSize: 11, color: '#64748b', marginTop: 2 }}>
                        {log.usuario?.nombre} ({log.usuario?.rol}) · {fecha}
                      </div>
                      {log.observacion && (
                        <div style={{ fontSize: 12, color: '#475569', marginTop: 4, fontStyle: 'italic' }}>"{log.observacion}"</div>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
          {timelineOpen && (!d.logs || d.logs.length === 0) && (
            <div style={{ fontSize: 12, color: '#94a3b8', marginTop: 16 }}>No hay registros de seguimiento</div>
          )}
        </div>
      </div>
    );
  }

  //--- List View ---
  return (
    <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4 }}>
            Expedientes
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            Solicitudes de compra o mantenimiento vinculadas al POI/PAP
          </div>
        </div>
        {puedeCrear && (
          <button className="btn btn-primary" onClick={showNewForm}>+ Nuevo expediente</button>
        )}
        {!puedeCrear && <div></div>}
      </div>

      {loading && <div style={{ color: '#94a3b8' }}>Cargando…</div>}

      {!loading && list.length === 0 && (
        <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>No hay expedientes registrados</div>
      )}

      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {list.map(e => {
          const ec = ESTADO_COLOR[e.estado] || { bg: '#f1f5f9', fg: '#475569' };
          return (
            <div key={e.id} className="card" style={{ padding: 16, cursor: 'pointer' }} onClick={() => { setDetalle(e); loadDetalle(e.id); }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                  <span style={{ fontSize: 13, fontWeight: 700, color: '#2563eb', fontFamily: 'monospace' }}>{e.codigo}</span>
                  <span style={{ fontSize: 14, fontWeight: 600, color: '#0f172a' }}>{e.descripcion?.substring(0, 80) || 'Sin descripción'}</span>
                  <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 99, background: ec.bg, color: ec.fg, fontWeight: 600 }}>{e.estado}</span>
                  <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 99,
                    background: e.urgencia === 'Urgente' ? '#fee2e2' : e.urgencia === 'No tan urgente' ? '#fef3c7' : '#f1f5f9',
                    color: e.urgencia === 'Urgente' ? '#b91c1c' : e.urgencia === 'No tan urgente' ? '#92400e' : '#475569',
                    fontWeight: 600
                  }}>{e.urgencia}</span>
                </div>
                <div style={{ fontSize: 12, color: '#94a3b8' }}>
                  {e.solicitante?.nombre} · {e.actividad?.codigo}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </div>
    );
  }
