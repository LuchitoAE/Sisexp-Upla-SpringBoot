import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { client } from '../api/client';

function formatMoney(n) { return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 }); }

const ESTADO_COLOR = {
  'Borrador': '#94a3b8', 'En revision': '#d97706', 'Aprobado': '#16a34a',
  'Rechazado': '#dc2626', 'Finalizado': '#6b21a8', 'Observado': '#c2410c', 'Derivado': '#0369a1'
};

export default function NecesidadPAPPage() {
  const { token } = useAuth();
  const [techos, setTechos] = useState([]);
  const [actividades, setActividades] = useState([]);
  const [necesidades, setNecesidades] = useState([]);
  const [expedientes, setExp] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filtroAnio, setFiltroAnio] = useState('');
  const [expanded, setExpanded] = useState({});

  useEffect(() => {
    client.get('/techos-presupuestales', token).then(d => {
      setTechos(d);
      if (d.length > 0) setFiltroAnio(String(d[d.length - 1].año));
    }).catch(() => {});
  }, [token]);

  const load = useCallback(async (anio) => {
    if (!anio) return;
    setLoading(true);
    try {
      const [necs, exps] = await Promise.all([
        client.get('/necesidades-pap', token),
        client.get(`/reportes/expedientes?anio=${anio}`, token)
      ]);
      setNecesidades(necs);
      setExp((exps.listado || []));

      // Extract unique actividades from necesidades for the selected year
      const actMap = {};
      necs.forEach(n => {
        const act = n.actividad;
        if (act && String(act.techo?.año) === String(anio)) {
          actMap[act.id] = act;
        }
      });
      setActividades(Object.values(actMap).sort((a, b) => (a.codigo || '').localeCompare(b.codigo || '')));
    } catch (err) { /* ignore */ }
    finally { setLoading(false); }
  }, [token]);

  useEffect(() => { load(filtroAnio); }, [filtroAnio, load]);

  const toggle = (id) => setExpanded(prev => ({ ...prev, [id]: !prev[id] }));

  const techoActual = techos.find(t => String(t.año) === filtroAnio);

  const getExpedientesDeNecesidad = (necId) => {
    return expedientes.filter(e => {
      // expedientes listado doesn't have necesidadPapId directly, but we can match by looking at the necesidad's expedientes
      // Actually the reportes/expedientes listado doesn't include necesidadPapId.
      // Let me fix this - I'll fetch expedientes differently.
      return false;
    });
  };

  // For now, use the necesidad's own data for execution counts
  // The detailed execution view will be fetched on demand

  return (
    <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4 }}>
            Plan Anual de Contrataciones (PAP)
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            Vista jerárquica: POI → Ítems → Ejecución con fechas
          </div>
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <span style={{ fontSize: 12, color: '#94a3b8' }}>Año:</span>
          <select className="input" value={filtroAnio} onChange={e => setFiltroAnio(e.target.value)} style={{ width: 120 }}>
            {techos.map(t => <option key={t.id} value={t.año}>{t.año}</option>)}
          </select>
        </div>
      </div>

      {/* Techo info */}
      {techoActual && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
          {[
            { label: 'Techo total', value: formatMoney(techoActual.montoTotal), color: '#0f172a' },
            { label: 'Utilizado', value: formatMoney(techoActual.montoUtilizado), color: '#2563eb' },
            { label: 'Disponible', value: formatMoney(techoActual.montoTotal - techoActual.montoUtilizado), color: '#16a34a' },
            { label: 'Actividades', value: actividades.length, color: '#d97706' },
          ].map(k => (
            <div key={k.label} style={{ background: '#fff', borderRadius: 12, padding: 14, border: '1px solid #f1f5f9' }}>
              <div style={{ fontSize: 11, color: '#64748b', fontWeight: 500 }}>{k.label}</div>
              <div style={{ fontSize: 20, fontWeight: 800, color: k.color, marginTop: 2 }}>{k.value}</div>
            </div>
          ))}
        </div>
      )}

      {loading && <div style={{ textAlign: 'center', padding: 30, color: '#94a3b8' }}>Cargando...</div>}

      {!loading && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {actividades.map(act => {
            const items = necesidades.filter(n => n.actividadPoiId === act.id);
            const isOpen = expanded[`act-${act.id}`];
            const totalPlan = items.reduce((s, i) => s + parseInt(i.cantidad) * parseFloat(i.precioEstimado), 0);
            const totalEjec = items.reduce((s, i) => s + (parseFloat(i.montoEjecutado) || 0), 0);

            return (
              <div key={act.id} className="card" style={{ overflow: 'hidden' }}>
                {/* Actividad header */}
                <div onClick={() => toggle(`act-${act.id}`)} style={{
                  padding: '14px 18px', cursor: 'pointer',
                  display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                  transition: 'background 0.12s',
                  background: isOpen ? '#f8fafc' : '#fff'
                }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12, flex: 1 }}>
                    <span style={{ fontSize: 14, fontWeight: 700, fontFamily: 'monospace', color: '#2563eb' }}>{act.codigo}</span>
                    <span style={{ fontSize: 14, fontWeight: 600, color: '#0f172a' }}>{act.nombre}</span>
                    <span style={{ fontSize: 11, padding: '2px 8px', borderRadius: 8, background: act.planificado ? '#dcfce7' : '#fef3c7', color: act.planificado ? '#166534' : '#92400e', fontWeight: 600 }}>
                      {act.planificado ? 'PAP cerrado' : 'PAP abierto'}
                    </span>
                    {items.length === 0 && <span style={{ fontSize: 11, color: '#94a3b8' }}>Sin ítems PAP</span>}
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: 14, fontWeight: 800, color: '#0f172a' }}>{formatMoney(act.presupuestoAsignado)}</div>
                      <div style={{ fontSize: 10, color: '#94a3b8' }}>
                        {items.length} ítems · PAP: {formatMoney(totalPlan)} · Ejec: {formatMoney(totalEjec)}
                      </div>
                    </div>
                    <span style={{ fontSize: 12, color: '#94a3b8', transition: 'transform 0.2s', transform: isOpen ? 'rotate(90deg)' : 'rotate(0deg)' }}>▶</span>
                  </div>
                </div>

                {/* Items expandidos */}
                {isOpen && (
                  <div style={{ borderTop: '1px solid #f1f5f9', background: '#fafbfc' }}>
                    {items.length === 0 ? (
                      <div style={{ padding: 20, textAlign: 'center', color: '#94a3b8', fontSize: 12 }}>
                        Esta actividad no tiene ítems registrados en el PAP
                      </div>
                    ) : (
                      items.map(item => {
                        const itemOpen = expanded[`nec-${item.id}`];
                        const subtotal = parseInt(item.cantidad) * parseFloat(item.precioEstimado);
                        const execPct = item.cantidad > 0 ? Math.round(((item.cantidadEjecutada || 0) / item.cantidad) * 100) : 0;

                        return (
                          <div key={item.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                            {/* Item header */}
                            <div onClick={(e) => { e.stopPropagation(); toggle(`nec-${item.id}`); }} style={{
                              padding: '10px 18px 10px 32px', cursor: 'pointer',
                              display: 'flex', justifyContent: 'space-between', alignItems: 'center',
                              transition: 'background 0.12s'
                            }}>
                              <div style={{ flex: 1 }}>
                                <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 2 }}>
                                  <span style={{ fontSize: 13, fontWeight: 600, color: '#334155' }}>{item.nombre}</span>
                                  <span style={{ fontSize: 10, padding: '1px 6px', borderRadius: 6, background: item.tipo === 'Bien' ? '#dbeafe' : '#fef3c7', color: item.tipo === 'Bien' ? '#1d4ed8' : '#92400e', fontWeight: 600 }}>
                                    {item.tipo}
                                  </span>
                                  <code style={{ fontSize: 9, background: '#f1f5f9', padding: '1px 5px', borderRadius: 3, color: '#64748b' }}>{item.clasificadorGasto || '—'}</code>
                                </div>
                                <div style={{ display: 'flex', gap: 14, fontSize: 11, color: '#64748b' }}>
                                  <span>Plan: <strong>{item.cantidad} {item.unidad || 'und'}</strong></span>
                                  <span>Disp: <strong style={{ color: (item.cantidadDisponible || 0) > 0 ? '#166534' : '#b91c1c' }}>{item.cantidadDisponible || 0}</strong></span>
                                  <span>Ejec: <strong style={{ color: (item.cantidadEjecutada || 0) > 0 ? '#d97706' : '#94a3b8' }}>{item.cantidadEjecutada || 0}</strong> ({execPct}%)</span>
                                  <span>P.Unit: <strong>{formatMoney(item.precioEstimado)}</strong></span>
                                </div>
                                {/* Barra de progreso */}
                                <div style={{ marginTop: 6, display: 'flex', height: 5, borderRadius: 3, overflow: 'hidden', background: '#e2e8f0', maxWidth: 300 }}>
                                  <div style={{ width: `${execPct}%`, background: '#d97706', minWidth: execPct > 0 ? 4 : 0, transition: 'width 0.3s' }} />
                                </div>
                              </div>
                              <div style={{ textAlign: 'right', flexShrink: 0, marginLeft: 12 }}>
                                <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a' }}>{formatMoney(subtotal)}</div>
                                <div style={{ fontSize: 10, color: '#94a3b8' }}>
                                  Ejec: {formatMoney((item.montoEjecutado || 0))}
                                </div>
                              </div>
                            </div>

                            {/* Detalle de ejecución por ítem */}
                            {itemOpen && (
                              <ItemEjecucionDetalle necesidadId={item.id} token={token} necesidadNombre={item.nombre} />
                            )}
                          </div>
                        );
                      })
                    )}
                  </div>
                )}
              </div>
            );
          })}

          {actividades.length === 0 && (
            <div className="card" style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>No hay actividades con ítems PAP para este año</div>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Subcomponente: detalle de ejecución de un ítem ───
function ItemEjecucionDetalle({ necesidadId, token, necesidadNombre }) {
  const [expedientes, setExpedientes] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    client.get('/expedientes', token)
      .then(data => {
        // Filtrar expedientes que usan esta necesidad
        const relevantes = (Array.isArray(data) ? data : []).filter(e =>
          e.necesidadPapId === necesidadId || e.necesidad?.id === necesidadId
        );
        setExpedientes(relevantes);
      })
      .catch(() => setExpedientes([]))
      .finally(() => setLoading(false));
  }, [necesidadId, token]);

  if (loading) return <div style={{ padding: '8px 32px 12px', fontSize: 11, color: '#94a3b8' }}>Cargando ejecuciones...</div>;

  if (expedientes.length === 0) {
    return (
      <div style={{ padding: '8px 32px 16px', fontSize: 11, color: '#94a3b8' }}>
        Sin expedientes registrados para este ítem
      </div>
    );
  }

  return (
    <div style={{ padding: '4px 32px 12px' }}>
      <div style={{ fontSize: 11, fontWeight: 600, color: '#475569', marginBottom: 6 }}>
        Expedientes que consumieron este ítem ({expedientes.length})
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        {expedientes.map(e => {
          const color = ESTADO_COLOR[e.estado] || '#94a3b8';
          const costoSol = parseFloat(e.costoEstimado) || 0;
          return (
            <div key={e.id} style={{
              display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px',
              borderRadius: 8, background: '#fff', border: '1px solid #f1f5f9',
              fontSize: 12
            }}>
              <span style={{ fontFamily: 'monospace', fontSize: 11, fontWeight: 700, color: '#2563eb', minWidth: 100 }}>
                {e.codigo || `EXP-${e.id}`}
              </span>
              <span style={{
                fontSize: 10, padding: '2px 8px', borderRadius: 10, fontWeight: 600,
                background: `${color}18`, color: color
              }}>
                {e.estado}
              </span>
              <span style={{ color: '#475569', fontWeight: 600 }}>
                {e.cantidadSolicitada || '?'} und
              </span>
              <span style={{ color: '#0f172a', fontWeight: 700 }}>
                {formatMoney(costoSol)}
              </span>
              <span style={{ fontSize: 11, color: '#94a3b8', marginLeft: 'auto' }}>
                {e.solicitante?.nombre || '—'}
              </span>
              <span style={{ fontSize: 10, color: '#94a3b8' }}>
                {e.createdAt ? new Date(e.createdAt).toLocaleDateString('es-PE') : '—'}
              </span>
              {(e.estado === 'Aprobado' || e.estado === 'Finalizado' || e.estado === 'Derivado') && (
                <span style={{ fontSize: 10, color: '#16a34a', fontWeight: 600 }}>
                  ✓ {e.updatedAt ? new Date(e.updatedAt).toLocaleDateString('es-PE', { day: 'numeric', month: 'short' }) : ''}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
