import React, { useEffect, useState, useMemo } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { ROL_LABEL, ROL_PROFILE, ROL_COLOR, puede } from '../utils/config';
import { client } from '../api/client';

function formatMoney(n) {
  return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
}

function SemaforoBadge({ color }) {
  const colors = { verde: '#16a34a', amarillo: '#d97706', rojo: '#dc2626' };
  const bgColors = { verde: '#dcfce7', amarillo: '#fef3c7', rojo: '#fee2e2' };
  return (
    <span style={{
      display: 'inline-block', width: 10, height: 10, borderRadius: '50%',
      background: colors[color] || '#94a3b8', flexShrink: 0,
      boxShadow: `0 0 0 3px ${bgColors[color] || '#f1f5f9'}`
    }} />
  );
}

function ProgressBar({ ejecutado, comprometido, disponible, height = 8, showLabels = false }) {
  const total = ejecutado + comprometido + disponible;
  if (total === 0) return <div style={{ height, background: '#e2e8f0', borderRadius: 4 }} />;
  const ePct = (ejecutado / total) * 100;
  const cPct = (comprometido / total) * 100;
  const dPct = (disponible / total) * 100;
  return (
    <div>
      <div style={{ display: 'flex', height, borderRadius: 4, overflow: 'hidden', background: '#e2e8f0', width: '100%' }}>
        {ejecutado > 0 && <div style={{ width: `${ePct}%`, background: '#16a34a', minWidth: 4, transition: 'width 0.4s' }} title={`Ejecutado: ${formatMoney(ejecutado)}`} />}
        {comprometido > 0 && <div style={{ width: `${cPct}%`, background: '#d97706', minWidth: 4, transition: 'width 0.4s' }} title={`Comprometido: ${formatMoney(comprometido)}`} />}
        {disponible > 0 && <div style={{ width: `${dPct}%`, background: '#3b82f6', minWidth: 4, transition: 'width 0.4s' }} title={`Disponible: ${formatMoney(disponible)}`} />}
      </div>
      {showLabels && (
        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: '#94a3b8', marginTop: 3 }}>
          <span style={{ color: '#16a34a' }}>{ePct.toFixed(0)}% ej</span>
          <span style={{ color: '#d97706' }}>{cPct.toFixed(0)}% comp</span>
          <span style={{ color: '#3b82f6' }}>{dPct.toFixed(0)}% disp</span>
        </div>
      )}
    </div>
  );
}

export default function Dashboard() {
  const { user } = useAuth();
  const [alertas, setAlertas] = useState(null);
  const [saldos, setSaldos] = useState(null);
  const [loading, setLoading] = useState(true);
  const [saldosExpandido, setSaldosExpandido] = useState(false);
  const [filtroAnioSaldos, setFiltroAnioSaldos] = useState('');

  const profile = ROL_PROFILE[user?.rol] || {};
  const puedeVerAlertas = ['Administrador', 'Coordinacion', 'Secretaria', 'Director', 'Laboratorio'].includes(user?.rol);
  const puedeVerSaldos = ['Administrador', 'Coordinacion', 'Director'].includes(user?.rol);

  useEffect(() => {
    async function load() {
      try {
        const [alertasData, saldosData] = await Promise.all([
          puedeVerAlertas ? client.get('/dashboard/alertas') : null,
          puedeVerSaldos ? client.get('/dashboard/saldos') : null,
        ]);
        setAlertas(alertasData);
        setSaldos(saldosData);
        if (saldosData?.techos?.length) {
          const years = saldosData.techos.map(t => t.año).sort();
          setFiltroAnioSaldos(String(years[years.length - 1]));
        } else if (saldosData?.actividades?.length) {
          const years = [...new Set(saldosData.actividades.map(a => a.año).filter(Boolean))].sort();
          if (years.length > 0) setFiltroAnioSaldos(String(years[years.length - 1]));
        }
      } catch (err) { console.error(err); }
      finally { setLoading(false); }
    }
    load();
  }, []); // eslint-disable-line

  const años = useMemo(() => {
    const set = new Set();
    if (saldos?.techos) saldos.techos.forEach(t => set.add(t.año));
    if (saldos?.actividades) saldos.actividades.forEach(a => { if (a.año) set.add(a.año); });
    return [...set].sort();
  }, [saldos]);

  const totalesPorAnio = useMemo(() => {
    const map = {};
    // Inicializar techos como años base (incluso sin actividades)
    if (saldos?.techos) {
      saldos.techos.forEach(t => {
        map[String(t.año)] = { asignado: 0, comprometido: 0, ejecutado: 0, disponible: 0 };
      });
    }
    if (saldos?.actividades) {
      saldos.actividades.forEach(a => {
        const key = String(a.año || '—');
        if (!map[key]) map[key] = { asignado: 0, comprometido: 0, ejecutado: 0, disponible: 0 };
        map[key].asignado += a.asignado;
        map[key].comprometido += a.comprometido;
        map[key].ejecutado += a.ejecutado;
        map[key].disponible += a.disponible;
      });
    }
    return map;
  }, [saldos]);

  const actividadesFiltradas = useMemo(() => {
    if (!saldos?.actividades) return [];
    if (!filtroAnioSaldos && años.length > 0) return [];
    if (!filtroAnioSaldos) return saldos.actividades;
    return saldos.actividades.filter(a => String(a.año) === filtroAnioSaldos);
  }, [saldos, filtroAnioSaldos, años]);

  const totalesFiltrados = useMemo(() => {
    return actividadesFiltradas.reduce((acc, a) => ({
      asignado: acc.asignado + a.asignado,
      comprometido: acc.comprometido + a.comprometido,
      ejecutado: acc.ejecutado + a.ejecutado,
      disponible: acc.disponible + a.disponible,
    }), { asignado: 0, comprometido: 0, ejecutado: 0, disponible: 0 });
  }, [actividadesFiltradas]);

  if (loading) {
    return (
      <div style={{ padding: 40, textAlign: 'center' }}>
        <div style={{ fontSize: 15, fontWeight: 600, color: '#94a3b8' }}>Cargando panel principal…</div>
      </div>
    );
  }

  const totalRojas = alertas?.resumen?.rojas || 0;
  const totalAmarillas = alertas?.resumen?.amarillas || 0;
  const totalVerdes = alertas?.resumen?.verdes || 0;
  const totalAlertas = totalRojas + totalAmarillas + totalVerdes;

  return (
    <div style={{ padding: 28, maxWidth: 1200, margin: '0 auto' }}>
      {/* ─── Header ─── */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#0f172a', letterSpacing: -0.5 }}>
            Panel de Control
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 2 }}>
            {user?.nombre} ·{' '}
            <span style={{ color: profile.color || ROL_COLOR[user?.rol], fontWeight: 600 }}>
              {profile.label || ROL_LABEL[user?.rol]}
            </span>
          </div>
        </div>
        <div style={{
          padding: '6px 16px', borderRadius: 20, fontSize: 12, fontWeight: 600,
          background: profile.color ? `${profile.color}18` : '#f1f5f9',
          color: profile.color || '#475569', border: `1px solid ${profile.color}30`
        }}>
          {profile.label || user?.rol}
        </div>
      </div>

      {/* ─── KPI Cards por año ─── */}
      {puedeVerSaldos && saldos && (
        <div style={{ marginBottom: 24 }}>
          {años.map(anio => {
            const t = totalesPorAnio[String(anio)] || { asignado: 0, comprometido: 0, ejecutado: 0, disponible: 0 };
            return (
              <div key={anio} style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10, display: 'flex', alignItems: 'center', gap: 8 }}>
                  <span style={{ background: '#eff6ff', color: '#2563eb', padding: '3px 12px', borderRadius: 10, fontSize: 12 }}>{anio}</span>
                  <span style={{ fontSize: 11, fontWeight: 400, color: '#64748b' }}>Presupuesto total: {formatMoney(t.asignado)}</span>
                </div>
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
                  {[
                    { label: 'Presupuesto asignado', value: t.asignado, color: '#0f172a', bg: '#f8fafc' },
                    { label: 'Ejecutado', value: t.ejecutado, color: '#16a34a', bg: '#f0fdf4',
                      pct: t.asignado > 0 ? Math.round((t.ejecutado / t.asignado) * 100) : 0 },
                    { label: 'Comprometido', value: t.comprometido, color: '#d97706', bg: '#fffbeb',
                      pct: t.asignado > 0 ? Math.round((t.comprometido / t.asignado) * 100) : 0 },
                    { label: 'Disponible', value: t.disponible, color: '#3b82f6', bg: '#eff6ff',
                      pct: t.asignado > 0 ? Math.round((t.disponible / t.asignado) * 100) : 0 },
                  ].map(k => (
                    <div key={k.label} style={{
                      background: '#fff', borderRadius: 10, padding: '12px 14px',
                      border: '1px solid #f1f5f9', boxShadow: '0 1px 2px rgba(0,0,0,0.03)'
                    }}>
                      <div style={{ fontSize: 11, fontWeight: 500, color: '#64748b', marginBottom: 3 }}>{k.label}</div>
                      <div style={{ fontSize: 16, fontWeight: 800, color: k.color, letterSpacing: -0.2 }}>
                        {formatMoney(k.value)}
                      </div>
                      {k.pct !== undefined && (
                        <div style={{ fontSize: 10, color: '#94a3b8', marginTop: 1 }}>{k.pct}% del año</div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* ─── Alertas Semáforo ─── */}
      {puedeVerAlertas && alertas && (
        <div style={{ marginBottom: 24 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: '#0f172a', marginBottom: 14, display: 'flex', alignItems: 'center', gap: 8 }}>
            <span>🚦 Estado de Alertas</span>
            {totalAlertas === 0 && <span style={{ fontSize: 11, fontWeight: 400, color: '#16a34a', background: '#dcfce7', padding: '2px 10px', borderRadius: 12 }}>Todo en orden</span>}
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 16 }}>
            {[
              { label: 'Rojas', count: totalRojas, color: '#dc2626', bg: '#fef2f2', border: '#fecaca' },
              { label: 'Amarillas', count: totalAmarillas, color: '#d97706', bg: '#fffbeb', border: '#fde68a' },
              { label: 'Verdes', count: totalVerdes, color: '#16a34a', bg: '#f0fdf4', border: '#bbf7d0' },
            ].map(a => (
              <div key={a.label} style={{
                background: a.bg, borderRadius: 12, padding: '14px 18px',
                border: `1px solid ${a.border}`, textAlign: 'center'
              }}>
                <div style={{ fontSize: 32, fontWeight: 800, color: a.color, lineHeight: 1 }}>{a.count}</div>
                <div style={{ fontSize: 12, fontWeight: 600, color: a.color, marginTop: 2 }}>Alertas {a.label}</div>
              </div>
            ))}
          </div>

          {/* Actividades en alerta */}
          {alertas.actividades?.length > 0 && (
            <div style={{ background: '#fff', borderRadius: 14, padding: 18, border: '1px solid #f1f5f9', boxShadow: '0 1px 3px rgba(0,0,0,0.04)', marginBottom: 14 }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>
                📅 Actividades POI — Próximas a vencer
              </div>
              {alertas.actividades.slice(0, 8).map(a => (
                <div key={a.id} style={{
                  display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', borderRadius: 8, marginBottom: 6,
                  background: a.semaforo === 'rojo' ? '#fef2f2' : a.semaforo === 'amarillo' ? '#fffbeb' : '#f0fdf4',
                  border: `1px solid ${a.semaforo === 'rojo' ? '#fecaca' : a.semaforo === 'amarillo' ? '#fde68a' : '#bbf7d0'}`
                }}>
                  <SemaforoBadge color={a.semaforo} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12, fontWeight: 600, color: '#0f172a', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {a.codigo} — {a.nombre}
                    </div>
                    <div style={{ fontSize: 11, color: '#64748b', marginTop: 1 }}>
                      {a.diasRestantes < 0 ? `${Math.abs(a.diasRestantes)}d vencido` : `${a.diasRestantes}d restantes`} · Ejecución: {a.pctEjecucion}%
                    </div>
                  </div>
                  <div style={{ width: 80 }}>
                    <ProgressBar ejecutado={a.saldoDisponible ? 0 : a.pctEjecucion} comprometido={0} disponible={a.saldoDisponible} height={6} />
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Expedientes sin movimiento */}
          {alertas.expedientes?.length > 0 && (
            <div style={{ background: '#fff', borderRadius: 14, padding: 18, border: '1px solid #f1f5f9', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
              <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>
                ⏳ Expedientes sin movimiento (+7 días)
              </div>
              {alertas.expedientes.map(e => (
                <div key={e.id} style={{
                  display: 'flex', alignItems: 'center', gap: 10, padding: '8px 12px', borderRadius: 8, marginBottom: 6,
                  background: e.semaforo === 'rojo' ? '#fef2f2' : '#fffbeb',
                  border: `1px solid ${e.semaforo === 'rojo' ? '#fecaca' : '#fde68a'}`
                }}>
                  <SemaforoBadge color={e.semaforo} />
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 12, fontWeight: 600, color: '#0f172a' }}>
                      {e.codigo}
                      <span style={{ fontSize: 10, padding: '1px 6px', borderRadius: 8, background: '#f1f5f9', color: '#475569', fontWeight: 600, marginLeft: 6 }}>{e.estado}</span>
                      <span style={{
                        fontSize: 10, padding: '1px 6px', borderRadius: 8, marginLeft: 4, fontWeight: 600,
                        background: e.urgencia === 'Urgente' ? '#fee2e2' : e.urgencia === 'No tan urgente' ? '#fef3c7' : '#f1f5f9',
                        color: e.urgencia === 'Urgente' ? '#b91c1c' : e.urgencia === 'No tan urgente' ? '#92400e' : '#475569'
                      }}>{e.urgencia}</span>
                    </div>
                    <div style={{ fontSize: 11, color: '#64748b', marginTop: 1 }}>
                      {e.descripcion?.substring(0, 80)} · {e.diasSinMovimiento}d sin actividad
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {alertas.actividades?.length === 0 && alertas.expedientes?.length === 0 && (
            <div style={{ textAlign: 'center', padding: 20, color: '#16a34a', fontSize: 13, background: '#f0fdf4', borderRadius: 12 }}>
              ✓ Todos los indicadores están en verde. Sin alertas activas.
            </div>
          )}
        </div>
      )}

      {/* ─── Saldos en Tiempo Real ─── */}
      {puedeVerSaldos && saldos && (
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9', boxShadow: '0 1px 3px rgba(0,0,0,0.04)' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <div style={{ fontSize: 15, fontWeight: 700, color: '#0f172a' }}>
              📊 Saldos Presupuestales en Tiempo Real
            </div>
            <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
              <select value={filtroAnioSaldos} onChange={e => setFiltroAnioSaldos(e.target.value)}
                style={{ fontSize: 12, padding: '4px 10px', borderRadius: 8, border: '1px solid #e2e8f0', background: '#fff', color: '#475569' }}>
                {años.map(a => <option key={a} value={a}>{a}</option>)}
              </select>
              <button onClick={() => setSaldosExpandido(!saldosExpandido)}
                style={{ fontSize: 11, padding: '4px 12px', borderRadius: 8, border: '1px solid #e2e8f0', background: '#fff', color: '#2563eb', cursor: 'pointer', fontWeight: 600 }}>
                {saldosExpandido ? 'Colapsar' : `Ver ${actividadesFiltradas.length} actividades`}
              </button>
            </div>
          </div>

          {/* Barra de saldos general */}
          <div style={{ marginBottom: 20 }}>
            <ProgressBar
              ejecutado={totalesFiltrados.ejecutado}
              comprometido={totalesFiltrados.comprometido}
              disponible={totalesFiltrados.disponible}
              height={20}
            />
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginTop: 8 }}>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: '#16a34a' }}>{formatMoney(totalesFiltrados.ejecutado)}</div>
                <div style={{ fontSize: 11, color: '#16a34a', fontWeight: 500 }}>
                  Ejecutado ({totalesFiltrados.asignado > 0 ? Math.round((totalesFiltrados.ejecutado / totalesFiltrados.asignado) * 100) : 0}%)
                </div>
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: '#d97706' }}>{formatMoney(totalesFiltrados.comprometido)}</div>
                <div style={{ fontSize: 11, color: '#d97706', fontWeight: 500 }}>
                  Comprometido ({totalesFiltrados.asignado > 0 ? Math.round((totalesFiltrados.comprometido / totalesFiltrados.asignado) * 100) : 0}%)
                </div>
              </div>
              <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: '#3b82f6' }}>{formatMoney(totalesFiltrados.disponible)}</div>
                <div style={{ fontSize: 11, color: '#3b82f6', fontWeight: 500 }}>
                  Disponible ({totalesFiltrados.asignado > 0 ? Math.round((totalesFiltrados.disponible / totalesFiltrados.asignado) * 100) : 0}%)
                </div>
              </div>
            </div>
          </div>

          {/* Detalle por actividad */}
          {saldosExpandido && (
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8, maxHeight: 500, overflowY: 'auto' }}>
              {actividadesFiltradas.map(a => {
                const total = a.asignado;
                const pctEj = total > 0 ? Math.round((a.ejecutado / total) * 100) : 0;
                const pctDisp = total > 0 ? Math.round((a.disponible / total) * 100) : 0;
                const estadoColor = a.pctEjecutado === 100 ? '#6b21a8' : a.comprometido > 0 ? '#d97706' : '#64748b';
                return (
                  <div key={a.id} style={{
                    padding: '10px 14px', borderRadius: 10,
                    background: a.disponible === 0 && a.ejecutado > 0 ? '#f5f3ff' : '#fafbfc',
                    border: `1px solid ${a.disponible === 0 && a.ejecutado > 0 ? '#e9d5ff' : '#f1f5f9'}`,
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                      <div style={{ flex: 1, minWidth: 0 }}>
                        <div style={{ fontSize: 12, fontWeight: 600, color: '#0f172a', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                          <span style={{ fontFamily: 'monospace', color: '#2563eb', fontSize: 11 }}>{a.codigo}</span>
                          <span style={{ margin: '0 6px', color: '#cbd5e1' }}>|</span>
                          {a.nombre}
                        </div>
                      </div>
                      <div style={{ fontSize: 12, fontWeight: 700, color: '#2563eb', textAlign: 'right', flexShrink: 0, marginLeft: 12 }}>
                        {formatMoney(total)}
                      </div>
                    </div>
                    <ProgressBar ejecutado={a.ejecutado} comprometido={a.comprometido} disponible={a.disponible} height={8} />
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 10, color: '#94a3b8', marginTop: 4 }}>
                      <span>Ejec: {pctEj}%</span>
                      <span>Disp: {pctDisp}%</span>
                      {a.año && <span style={{ fontWeight: 500, color: estadoColor }}>{a.año}</span>}
                    </div>
                  </div>
                );
              })}
              {actividadesFiltradas.length === 0 && (
                <div style={{ textAlign: 'center', padding: 16, color: '#94a3b8', fontSize: 12 }}>Sin actividades para este filtro</div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Fallback for Decanato / read-only roles */}
      {!puedeVerAlertas && !puedeVerSaldos && (
        <div style={{ textAlign: 'center', padding: 48, background: '#fff', borderRadius: 14, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 32, marginBottom: 12 }}>📋</div>
          <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 6 }}>
            Sistema de Gestión de Expedientes — SISEXP-UPLA
          </div>
          <div style={{ fontSize: 13, color: '#64748b', maxWidth: 400, margin: '0 auto', lineHeight: 1.6 }}>
            Seleccione un módulo en la barra lateral para acceder a los reportes institucionales y al seguimiento presupuestal.
          </div>
        </div>
      )}
    </div>
  );
}
