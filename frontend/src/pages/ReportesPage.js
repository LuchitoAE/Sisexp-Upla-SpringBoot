import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useModals } from '../App';
import { client } from '../api/client';

function formatMoney(n) {
  return 'S/ ' + Number(n).toLocaleString('es-PE', { minimumFractionDigits: 2 });
}

// ─── Export utilities ───

function exportarCSV(columnas, filas, nombreArchivo) {
  const encabezado = columnas.map(c => `"${c.label}"`).join(',');
  const lineas = filas.map(fila =>
    columnas.map(c => {
      const val = typeof c.key === 'function' ? c.key(fila) : (fila[c.key] ?? '');
      return `"${String(val).replace(/"/g, '""')}"`;
    }).join(',')
  );
  const csv = '\uFEFF' + [encabezado, ...lineas].join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url; a.download = `${nombreArchivo}.csv`; a.click();
  URL.revokeObjectURL(url);
}

function exportarPDF(titulo, secciones, nombreArchivo) {
  const w = window.open('', '_blank');
  if (!w) return;
  let html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>${titulo}</title>
<style>
  body { font-family: Arial, sans-serif; margin: 30px; color: #1e293b; font-size: 11pt; }
  h1 { font-size: 18pt; margin-bottom: 4px; color: #0f172a; }
  .sub { color: #64748b; font-size: 10pt; margin-bottom: 20px; }
  h2 { font-size: 13pt; color: #2563eb; margin: 18px 0 8px; border-bottom: 1px solid #e2e8f0; padding-bottom: 4px; }
  table { width: 100%; border-collapse: collapse; margin: 10px 0 18px; font-size: 10pt; }
  th { background: #f1f5f9; color: #475569; padding: 6px 8px; text-align: left; font-weight: 600; border-bottom: 2px solid #cbd5e1; }
  td { padding: 6px 8px; border-bottom: 1px solid #f1f5f9; }
  .kpi { display: inline-block; margin: 0 20px 12px 0; }
  .kpi-val { font-size: 16pt; font-weight: 800; }
  .kpi-label { font-size: 9pt; color: #64748b; }
  .footer { margin-top: 30px; font-size: 9pt; color: #94a3b8; text-align: center; }
</style></head><body>
<h1>${titulo}</h1>
<div class="sub">SISEXP-UPLA v2 — Generado el ${new Date().toLocaleDateString('es-PE', { day:'numeric', month:'long', year:'numeric' })}</div>`;
  for (const sec of secciones) {
    html += `<h2>${sec.titulo}</h2>`;
    if (sec.kpis) {
      html += '<div>';
      for (const k of sec.kpis) {
        html += `<div class="kpi"><div class="kpi-val">${k.value}</div><div class="kpi-label">${k.label}</div></div>`;
      }
      html += '</div>';
    }
    if (sec.tabla) {
      html += '<table><thead><tr>';
      for (const h of sec.tabla.headers) html += `<th>${h}</th>`;
      html += '</tr></thead><tbody>';
      for (const r of sec.tabla.rows) {
        html += '<tr>';
        for (const c of r) html += `<td>${c}</td>`;
        html += '</tr>';
      }
      html += '</tbody></table>';
    }
  }
  html += '<div class="footer">SISEXP-UPLA — Sistema de Gestión de Expedientes</div></body></html>';
  w.document.write(html);
  w.document.close();
  setTimeout(() => w.print(), 600);
}

const SECCIONES = [
  { id: 'anual', label: 'Informe Anual', icon: '📋' },
  { id: 'expedientes', label: 'Expedientes', icon: '📁' },
  { id: 'poi', label: 'POI General', icon: '📊' },
  { id: 'pap', label: 'PAP General', icon: '📦' },
];

function Badge({ label, color = '#64748b', bg = '#f1f5f9' }) {
  return <span style={{ fontSize: 10, padding: '2px 8px', borderRadius: 8, background: bg, color, fontWeight: 600 }}>{label}</span>;
}

function Progress({ value, max, color = '#2563eb', height = 8 }) {
  const pct = max > 0 ? Math.min(100, (value / max) * 100) : 0;
  return (
    <div style={{ background: '#f1f5f9', borderRadius: 4, height, overflow: 'hidden' }}>
      <div style={{ width: `${pct}%`, height: '100%', background: color, borderRadius: 4, transition: 'width 0.4s' }} />
    </div>
  );
}

// ─── Data normalizers ───
function normalizeAnual(raw, anio) {
  const techo = raw.techo || {};
  const acts = raw.actividades || [];
  const montoTotal = techo.montoTotal || 0;
  const ejercido = techo.montoUtilizado || 0;
  return {
    techo: {
      total: montoTotal,
      pctEjecucion: montoTotal > 0 ? Math.round((ejercido / montoTotal) * 100) : 0,
      ejercido,
      disponible: techo.saldo || 0,
    },
    actividades: {
      total: acts.length,
      pendientes: acts.filter(a => a.estado === 'Pendiente' || a.estado === 'En_proceso').length,
      cerradas: acts.filter(a => a.estado === 'Finalizada').length,
    },
    expedientes: {
      total: raw.totalExpedientes || 0,
      costoTotal: raw.costoTotalEstimado || 0,
    },
    pap: {
      totalItems: 0,
      pctEjecucionMonto: '0',
    },
    _techo: techo,
    _acts: acts,
    _anio: anio,
  };
}

function normalizeExpedientes(raw) {
  const listado = Array.isArray(raw) ? raw : (raw.listado || []);
  return { listado };
}

function normalizePOI(raw) {
  const actividades = Array.isArray(raw) ? raw : (raw.actividades || []);
  const totalPresupuesto = actividades.reduce((s, a) => s + (a.presupuestoAsignado || 0), 0);
  const totalDisponible = actividades.reduce((s, a) => s + (a.disponible || 0), 0);
  return {
    presupuesto: {
      totalPOI: actividades.length,
      presupuestoTotal: totalPresupuesto,
      ejecucionPct: totalPresupuesto > 0 ? Math.round(((totalPresupuesto - totalDisponible) / totalPresupuesto) * 100) : 0,
      disponible: totalDisponible,
    },
    actividades,
  };
}

function normalizePAP(raw) {
  const listado = Array.isArray(raw) ? raw : (raw.listado || []);
  return { listado };
}

export default function ReportesPage() {
  useAuth();
  const modals = useModals();
  const [seccion, setSeccion] = useState('anual');
  const [anio, setAnio] = useState('2026');
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState(null);
  const [techos, setTechos] = useState([]);

  useEffect(() => {
    client.get('/techos-presupuestales').then(d => {
      setTechos(d);
      if (d.length > 0) setAnio(String(d[d.length - 1].año));
    }).catch(() => {});
  }, []);

  const load = useCallback(async (secc, a) => {
    setLoading(true);
    setData(null);
    try {
      let raw;
      switch (secc) {
        case 'anual': raw = await client.get(`/reportes/anual/${a}`); setData(normalizeAnual(raw, a)); return;
        case 'expedientes': raw = await client.get(`/reportes/expedientes?anio=${a}`); setData(normalizeExpedientes(raw)); return;
        case 'poi': raw = await client.get(`/reportes/poi?anio=${a}`); setData(normalizePOI(raw)); return;
        case 'pap': raw = await client.get(`/reportes/pap?anio=${a}`); setData(normalizePAP(raw)); return;
        default: setData(null);
      }
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setLoading(false); }
  }, []);

  useEffect(() => { load(seccion, anio); }, [seccion, anio, load]);

  // ─── Export handlers ───

  const handleExportCSV = () => {
    if (!data) return;
    switch (seccion) {
      case 'anual': if (data.techo) exportarCSVAnual(data, anio); break;
      case 'expedientes': if (data.listado) exportarCSVExpedientes(data, anio); break;
      case 'poi': if (data.actividades) exportarCSVPOI(data, anio); break;
      case 'pap': if (data.listado) exportarCSVPAP(data, anio); break;
      default: break;
    }
  };

  const handleExportPDF = () => {
    if (!data) return;
    switch (seccion) {
      case 'anual': if (data.techo) exportarPDFAnual(data, anio); break;
      case 'expedientes': if (data.listado) exportarPDFExpedientes(data, anio); break;
      case 'poi': if (data.actividades) exportarPDFPOI(data, anio); break;
      case 'pap': if (data.listado) exportarPDFPAP(data, anio); break;
      default: break;
    }
  };

  return (
    <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4, marginBottom: 4 }}>
        Reportes
      </div>
      <div style={{ fontSize: 13, color: '#64748b', marginBottom: 20 }}>
        Informes detallados de expedientes, POI, PAP y consolidado anual
      </div>

      {/* Tabs */}
      <div style={{ display: 'flex', gap: 6, marginBottom: 20, flexWrap: 'wrap' }}>
        {SECCIONES.map(s => (
          <button key={s.id} onClick={() => { setSeccion(s.id); setData(null); }} style={{
            padding: '8px 18px', borderRadius: 10, border: 'none', cursor: 'pointer',
            fontSize: 13, fontWeight: seccion === s.id ? 700 : 500,
            background: seccion === s.id ? '#eff6ff' : '#fff',
            color: seccion === s.id ? '#2563eb' : '#64748b',
            border: `1px solid ${seccion === s.id ? '#bfdbfe' : '#e2e8f0'}`
          }}>
            <span style={{ marginRight: 4 }}>{s.icon}</span> {s.label}
          </button>
        ))}
        {/* Año selector */}
        <select value={anio} onChange={e => setAnio(e.target.value)} style={{
          marginLeft: 'auto', padding: '6px 12px', borderRadius: 10, border: '1px solid #e2e8f0',
          fontSize: 13, fontWeight: 600, color: '#475569', background: '#fff'
        }}>
          {techos.map(t => <option key={t.id} value={t.año}>{t.año}</option>)}
        </select>
      </div>

      {loading && <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Cargando reporte…</div>}

      {!loading && data && (
        <>
          {/* Export bar */}
          <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginBottom: 20 }}>
            <button onClick={() => handleExportCSV()} style={{
              padding: '7px 16px', borderRadius: 8, border: '1px solid #16a34a', background: '#fff',
              color: '#16a34a', cursor: 'pointer', fontSize: 12, fontWeight: 600
            }}>📥 Exportar Excel</button>
            <button onClick={() => handleExportPDF()} style={{
              padding: '7px 16px', borderRadius: 8, border: '1px solid #dc2626', background: '#fff',
              color: '#dc2626', cursor: 'pointer', fontSize: 12, fontWeight: 600
            }}>📄 Exportar PDF</button>
          </div>

          {seccion === 'anual' && <InformeAnual data={data} anio={anio} />}
          {seccion === 'expedientes' && <ReporteExpedientes data={data} />}
          {seccion === 'poi' && data.presupuesto && <ReportePOI data={data} />}
          {seccion === 'pap' && data.listado && <ReportePAP data={data} />}
        </>
      )}

      {!loading && !data && (
        <div style={{ textAlign: 'center', padding: 40, color: '#94a3b8' }}>Seleccione una sección y año para generar el reporte</div>
      )}
    </div>
  );
}

// ─── Informe Anual ───
function InformeAnual({ data, anio }) {
  return (
    <div>
      <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 16 }}>
        Informe General del Año {anio}
      </div>

      {/* Resumen ejecutivo */}
      <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9', marginBottom: 20 }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>Resumen Ejecutivo</div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12 }}>
          {[
            { label: 'Presupuesto total', value: formatMoney(data.techo.total), extra: data.techo.pctEjecucion + '% ejercido' },
            { label: 'Actividades POI', value: data.actividades.total, extra: `${data.actividades.pendientes} pendientes · ${data.actividades.cerradas} cerradas` },
            { label: 'Expedientes', value: data.expedientes.total, extra: formatMoney(data.expedientes.costoTotal) + ' costo' },
            { label: 'Ítems PAP', value: data.pap.totalItems, extra: data.pap.pctEjecucionMonto + '% ejecutado' },
          ].map(k => (
            <div key={k.label} style={{ background: '#f8fafc', borderRadius: 10, padding: 14 }}>
              <div style={{ fontSize: 11, color: '#64748b', fontWeight: 500 }}>{k.label}</div>
              <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', marginTop: 2 }}>{k.value}</div>
              <div style={{ fontSize: 11, color: '#94a3b8', marginTop: 2 }}>{k.extra}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Ejecución presupuestal */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Ejecución Presupuestal</div>
          <Progress value={data.techo.ejercido} max={data.techo.total} color="#16a34a" height={14} />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8, fontSize: 12 }}>
            <span style={{ color: '#16a34a', fontWeight: 600 }}>{formatMoney(data.techo.ejercido)} ejecutado</span>
            <span style={{ color: '#3b82f6', fontWeight: 600 }}>{formatMoney(data.techo.disponible)} disponible</span>
          </div>
        </div>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Indicadores Clave</div>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
            {[
              { label: 'Vencidas', value: data.actividades.vencidas, color: '#dc2626' },
              { label: 'Cerradas', value: data.actividades.cerradas, color: '#6b21a8' },
              { label: 'Pendientes', value: data.actividades.pendientes, color: '#d97706' },
              { label: 'Con docs', value: data.expedientes.conDocumentos, color: '#16a34a' },
            ].map(i => (
              <div key={i.label} style={{ textAlign: 'center', background: '#f8fafc', borderRadius: 8, padding: 10 }}>
                <div style={{ fontSize: 20, fontWeight: 800, color: i.color }}>{i.value}</div>
                <div style={{ fontSize: 10, color: '#64748b', marginTop: 2 }}>{i.label}</div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Expedientes por estado y mes */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20 }}>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Expedientes por Estado</div>
          {Object.entries(data.expedientes.porEstado || {}).map(([estado, count]) => (
            <div key={estado} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 12, borderBottom: '1px solid #f1f5f9' }}>
              <span style={{ fontWeight: 500 }}>{estado}</span>
              <span style={{ fontWeight: 700, color: '#2563eb' }}>{count}</span>
            </div>
          ))}
        </div>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Expedientes por Mes</div>
          {Object.entries(data.expedientes.porMes || {}).sort().map(([mes, count]) => (
            <div key={mes} style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 }}>
              <span style={{ fontSize: 11, width: 36, fontWeight: 500, color: '#64748b' }}>{mes}</span>
              <div style={{ flex: 1 }}><Progress value={count} max={Math.max(...Object.values(data.expedientes.porMes), 1)} color="#2563eb" height={6} /></div>
              <span style={{ fontSize: 12, fontWeight: 700, color: '#2563eb', width: 20, textAlign: 'right' }}>{count}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Reporte de Expedientes ───
function ReporteExpedientes({ data }) {
  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
        {[
          { label: 'Total', value: data.total, color: '#0f172a', bg: '#f8fafc' },
          { label: 'Costo total', value: formatMoney(data.totalCosto), color: '#2563eb', bg: '#eff6ff' },
          { label: 'Con documentos', value: data.conDocumentos, color: '#16a34a', bg: '#f0fdf4' },
          { label: 'Sin documentos', value: data.sinDocumentos, color: '#d97706', bg: '#fffbeb' },
        ].map(k => (
          <div key={k.label} style={{ background: k.bg, borderRadius: 12, padding: 14, border: '1px solid #f1f5f9' }}>
            <div style={{ fontSize: 11, color: '#64748b' }}>{k.label}</div>
            <div style={{ fontSize: 22, fontWeight: 800, color: k.color, marginTop: 2 }}>{k.value}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Por Estado</div>
          {Object.entries(data.porEstado || {}).map(([k, v]) => (
            <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '5px 0', fontSize: 12 }}>
              <span>{k}</span> <span style={{ fontWeight: 700 }}>{v}</span>
            </div>
          ))}
        </div>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Por Urgencia</div>
          {Object.entries(data.porUrgencia || {}).map(([k, v]) => (
            <div key={k} style={{ display: 'flex', justifyContent: 'space-between', padding: '5px 0', fontSize: 12 }}>
              <span>{k}</span> <span style={{ fontWeight: 700 }}>{v}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Listado de Expedientes</div>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead><tr style={{ borderBottom: '2px solid #e2e8f0', textAlign: 'left' }}>
            <th style={{ padding: 8, color: '#64748b' }}>Código</th><th style={{ padding: 8, color: '#64748b' }}>Estado</th><th style={{ padding: 8, color: '#64748b' }}>Urgencia</th><th style={{ padding: 8, color: '#64748b', textAlign: 'right' }}>Cant. Sol.</th><th style={{ padding: 8, color: '#64748b', textAlign: 'right' }}>Costo</th><th style={{ padding: 8, color: '#64748b' }}>Descripción</th>
          </tr></thead>
          <tbody>
            {(data.listado || []).map(e => (
              <tr key={e.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: 8, fontFamily: 'monospace', fontSize: 11, color: '#2563eb', fontWeight: 600 }}>{e.codigo}</td>
                <td style={{ padding: 8 }}><Badge label={e.estado} bg={e.estado === 'Finalizado' ? '#f3e8ff' : e.estado === 'Aprobado' ? '#dcfce7' : '#f1f5f9'} color={e.estado === 'Finalizado' ? '#6b21a8' : e.estado === 'Aprobado' ? '#166534' : '#475569'} /></td>
                <td style={{ padding: 8, fontSize: 11 }}>{e.urgencia}</td>
                <td style={{ padding: 8, textAlign: 'right', fontWeight: 600 }}>{e.cantidadSolicitada}</td>
                <td style={{ padding: 8, textAlign: 'right', fontWeight: 600 }}>{formatMoney(e.costoEstimado)}</td>
                <td style={{ padding: 8, fontSize: 11, color: '#64748b', maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{e.descripcion || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Reporte POI ───
function ReportePOI({ data }) {
  const modals = useModals();
  const [detalle, setDetalle] = useState(null);
  const [detLoading, setDetLoading] = useState(false);

  const verDetalle = async (id) => {
    setDetLoading(true);
    try {
      const d = await client.get(`/reportes/poi/${id}`);
      setDetalle(d);
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setDetLoading(false); }
  };

  if (detalle) {
    const d = detalle;
    return (
      <div>
        <button onClick={() => setDetalle(null)} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: 13, fontWeight: 600, marginBottom: 16, padding: 0 }}>← Volver al POI general</button>
        <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>{d.codigo} — {d.nombre} ({d.añoTecho})</div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
          {[
            { label: 'Presupuesto', value: formatMoney(d.presupuestoAsignado), color: '#0f172a' },
            { label: 'Ejecutado', value: formatMoney(d.saldoEjecutado), color: '#16a34a', extra: d.pctEjecucion + '%' },
            { label: 'Comprometido', value: formatMoney(d.saldoComprometido), color: '#d97706' },
            { label: 'Disponible', value: formatMoney(d.disponible), color: '#3b82f6' },
          ].map(k => (
            <div key={k.label} style={{ background: '#fff', borderRadius: 12, padding: 14, border: '1px solid #f1f5f9' }}>
              <div style={{ fontSize: 11, color: '#64748b' }}>{k.label}</div>
              <div style={{ fontSize: 18, fontWeight: 800, color: k.color, marginTop: 2 }}>{k.value}</div>
              {k.extra && <div style={{ fontSize: 10, color: '#94a3b8' }}>{k.extra}</div>}
            </div>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 8, marginBottom: 16, flexWrap: 'wrap' }}>
          <Badge label={`Estado: ${d.estado}`} color="#2563eb" bg="#eff6ff" />
          {d.fechaLimite && <Badge label={`Vence: ${d.fechaLimite}`} color="#d97706" bg="#fffbeb" />}
          {d.planificado && <Badge label="PAP cerrado" color="#16a34a" bg="#dcfce7" />}
          {!d.planificado && <Badge label="PAP abierto" color="#92400e" bg="#fef3c7" />}
        </div>

        {/* Necesidades PAP */}
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9', marginBottom: 20 }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Necesidades PAP de esta actividad</div>
          {(d.necesidades || []).length === 0 ? <div style={{ fontSize: 12, color: '#94a3b8' }}>Sin necesidades registradas</div> : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Ítem</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Plan.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Disp.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Ejec.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Subtotal</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Mto Disp.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Mto Ejec.</th>
              </tr></thead>
              <tbody>
                {d.necesidades.map(n => (
                  <tr key={n.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '6px 8px', fontWeight: 500 }}>{n.nombre}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{n.cantidad}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: (n.cantidadDisponible || 0) > 0 ? '#166534' : '#b91c1c', fontWeight: 600 }}>{n.cantidadDisponible}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: (n.cantidadEjecutada || 0) > 0 ? '#d97706' : '#94a3b8', fontWeight: 600 }}>{n.cantidadEjecutada || 0}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{formatMoney(n.subtotal)}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{formatMoney(n.montoDisponible)}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: n.montoEjecutado > 0 ? '#d97706' : '#94a3b8' }}>{formatMoney(n.montoEjecutado)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Expedientes de esta actividad */}
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Expedientes vinculados</div>
          {(d.expedientes || []).length === 0 ? <div style={{ fontSize: 12, color: '#94a3b8' }}>Sin expedientes registrados</div> : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Código</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Estado</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Solicitante</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Cant.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Costo</th>
              </tr></thead>
              <tbody>
                {d.expedientes.map(e => (
                  <tr key={e.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '6px 8px', fontFamily: 'monospace', fontSize: 11, color: '#2563eb', fontWeight: 600 }}>{e.codigo}</td>
                    <td style={{ padding: '6px 8px' }}><Badge label={e.estado} /></td>
                    <td style={{ padding: '6px 8px', fontSize: 11 }}>{e.solicitante || '—'}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 600 }}>{e.cantidadSolicitada}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 600 }}>{formatMoney(e.costoEstimado)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
        {[
          { label: 'Actividades', value: data.totalActividades, color: '#0f172a' },
          { label: 'Presupuesto total', value: formatMoney(data.presupuesto.total), color: '#2563eb' },
          { label: 'Ejecución', value: data.presupuesto.pctEjecucion + '%', color: '#16a34a' },
          { label: 'Disponible', value: formatMoney(data.presupuesto.disponible), color: '#3b82f6' },
        ].map(k => (
          <div key={k.label} style={{ background: '#fff', borderRadius: 12, padding: 14, border: '1px solid #f1f5f9' }}>
            <div style={{ fontSize: 11, color: '#64748b' }}>{k.label}</div>
            <div style={{ fontSize: 20, fontWeight: 800, color: k.color, marginTop: 2 }}>{k.value}</div>
          </div>
        ))}
      </div>

      <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>Actividades — clic para ver detalle</div>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
            <th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Código</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Nombre</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Estado</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Presupuesto</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Ejecutado</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Disponible</th><th style={{ padding: '6px 8px', textAlign: 'center', color: '#64748b' }}>PAP</th>
          </tr></thead>
          <tbody>
            {(data.actividades || []).map(a => (
              <tr key={a.id} onClick={() => verDetalle(a.id)} style={{ borderBottom: '1px solid #f1f5f9', cursor: 'pointer', transition: 'background 0.15s' }}
                onMouseEnter={e => e.currentTarget.style.background = '#f8fafc'} onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>
                <td style={{ padding: '8px', fontFamily: 'monospace', fontSize: 11, color: '#2563eb', fontWeight: 600 }}>{a.codigo}</td>
                <td style={{ padding: '8px', fontWeight: 500, fontSize: 12 }}>{a.nombre}</td>
                <td style={{ padding: '8px' }}><Badge label={a.estado} /></td>
                <td style={{ padding: '8px', textAlign: 'right', fontWeight: 600 }}>{formatMoney(a.presupuestoAsignado)}</td>
                <td style={{ padding: '8px', textAlign: 'right', color: '#16a34a', fontWeight: 600 }}>{formatMoney(a.saldoEjecutado)}</td>
                <td style={{ padding: '8px', textAlign: 'right', color: '#3b82f6', fontWeight: 600 }}>{formatMoney(a.disponible)}</td>
                <td style={{ padding: '8px', textAlign: 'center' }}>{a.planificado ? <Badge label="Cerrado" color="#16a34a" bg="#dcfce7" /> : <Badge label="Abierto" color="#92400e" bg="#fef3c7" />}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Reporte PAP ───
function ReportePAP({ data }) {
  const modals = useModals();
  const [detalle, setDetalle] = useState(null);
  const [detLoading, setDetLoading] = useState(false);

  const verDetalle = async (id) => {
    setDetLoading(true);
    try {
      const d = await client.get(`/reportes/pap/${id}`);
      setDetalle(d);
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setDetLoading(false); }
  };

  if (detalle) {
    const d = detalle;
    return (
      <div>
        <button onClick={() => setDetalle(null)} style={{ background: 'none', border: 'none', color: '#2563eb', cursor: 'pointer', fontSize: 13, fontWeight: 600, marginBottom: 16, padding: 0 }}>← Volver al PAP general</button>
        <div style={{ fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 12 }}>{d.codigo} — {d.nombre} ({d.añoTecho})</div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
          {[
            { label: 'Presupuesto actividad', value: formatMoney(d.presupuestoAsignado), color: '#0f172a' },
            { label: 'Ítems PAP', value: (d.necesidades || []).length, color: '#2563eb' },
            { label: 'Ejecutado POI', value: formatMoney(d.saldoEjecutado), color: '#16a34a', extra: d.pctEjecucion + '%' },
            { label: 'Disponible POI', value: formatMoney(d.disponible), color: '#3b82f6' },
          ].map(k => (
            <div key={k.label} style={{ background: '#fff', borderRadius: 12, padding: 14, border: '1px solid #f1f5f9' }}>
              <div style={{ fontSize: 11, color: '#64748b' }}>{k.label}</div>
              <div style={{ fontSize: 18, fontWeight: 800, color: k.color, marginTop: 2 }}>{k.value}</div>
              {k.extra && <div style={{ fontSize: 10, color: '#94a3b8' }}>{k.extra}</div>}
            </div>
          ))}
        </div>

        {/* Necesidades detalle */}
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Desglose del PAP</div>
          {(d.necesidades || []).length === 0 ? <div style={{ fontSize: 12, color: '#94a3b8' }}>Sin necesidades registradas</div> : (
            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
              <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Ítem</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Tipo</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Plan.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Disp.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Ejec.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>P. Unit.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Subtotal</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Mto Ejec.</th>
              </tr></thead>
              <tbody>
                {d.necesidades.map(n => (
                  <tr key={n.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                    <td style={{ padding: '6px 8px', fontWeight: 500, maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{n.nombre}</td>
                    <td style={{ padding: '6px 8px' }}><Badge label={n.tipo} color={n.tipo === 'Bien' ? '#1d4ed8' : '#92400e'} bg={n.tipo === 'Bien' ? '#dbeafe' : '#fef3c7'} /></td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{n.cantidad}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: (n.cantidadDisponible || 0) > 0 ? '#166534' : '#b91c1c', fontWeight: 600 }}>{n.cantidadDisponible}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: (n.cantidadEjecutada || 0) > 0 ? '#d97706' : '#94a3b8', fontWeight: 600 }}>{n.cantidadEjecutada || 0}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right' }}>{formatMoney(n.precioEstimado)}</td>
                 <td style={{ padding: '6px 8px', textAlign: 'right', fontWeight: 600 }}>{formatMoney(n.subtotal)}</td>
                    <td style={{ padding: '6px 8px', textAlign: 'right', color: n.montoEjecutado > 0 ? '#d97706' : '#94a3b8', fontWeight: 600 }}>{formatMoney(n.montoEjecutado)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    );
  }

  return (
    <div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 12, marginBottom: 20 }}>
        {[
          { label: 'Total Ítems', value: data.totalItems, color: '#0f172a' },
          { label: 'Ejec. Cantidad', value: data.pctEjecucionCantidad + '%', color: '#d97706' },
          { label: 'Ejec. Monto', value: data.pctEjecucionMonto + '%', color: '#16a34a' },
          { label: 'Bienes / Servicios', value: `${data.porTipo?.Bien || 0} / ${data.porTipo?.Servicio || 0}`, color: '#2563eb' },
        ].map(k => (
          <div key={k.label} style={{ background: '#fff', borderRadius: 12, padding: 14, border: '1px solid #f1f5f9' }}>
            <div style={{ fontSize: 11, color: '#64748b' }}>{k.label}</div>
            <div style={{ fontSize: 20, fontWeight: 800, color: k.color, marginTop: 2 }}>{k.value}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 20, marginBottom: 20 }}>
        <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Cantidades (Total del año)</div>
          {[
            { label: 'Planificadas', value: data.cantidades?.planificado, color: '#2563eb' },
            { label: 'Disponibles', value: data.cantidades?.disponible, color: '#16a34a' },
            { label: 'Ejecutadas', value: data.cantidades?.ejecutado, color: '#d97706' },
          ].map(r => (
            <div key={r.label} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 13 }}>
              <span>{r.label}</span>
              <span style={{ fontWeight: 700, color: r.color }}>{r.value}</span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ background: '#fff', borderRadius: 14, padding: 20, border: '1px solid #f1f5f9' }}>
        <div style={{ fontSize: 13, fontWeight: 700, color: '#0f172a', marginBottom: 10 }}>Ítems PAP</div>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
          <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
            <th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Ítem</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Actividad</th><th style={{ padding: '6px 8px', textAlign: 'left', color: '#64748b' }}>Tipo</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Plan.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Disp.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>Ejec.</th><th style={{ padding: '6px 8px', textAlign: 'right', color: '#64748b' }}>P. Unit.</th>
          </tr></thead>
          <tbody>
            {(data.listado || []).map(n => (
              <tr key={n.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                <td style={{ padding: '6px 8px', fontWeight: 500 }}>{n.nombre}</td>
                <td style={{ padding: '6px 8px', fontFamily: 'monospace', fontSize: 10, color: '#2563eb' }}>{n.actividad}</td>
                <td style={{ padding: '6px 8px' }}><Badge label={n.tipo} color={n.tipo === 'Bien' ? '#1d4ed8' : '#92400e'} bg={n.tipo === 'Bien' ? '#dbeafe' : '#fef3c7'} /></td>
                <td style={{ padding: '6px 8px', textAlign: 'right' }}>{n.cantidad}</td>
                <td style={{ padding: '6px 8px', textAlign: 'right', color: (n.cantidadDisponible || 0) > 0 ? '#166534' : '#b91c1c', fontWeight: 600 }}>{n.cantidadDisponible}</td>
                <td style={{ padding: '6px 8px', textAlign: 'right', color: (n.cantidadEjecutada || 0) > 0 ? '#d97706' : '#94a3b8', fontWeight: 600 }}>{n.cantidadEjecutada || 0}</td>
                <td style={{ padding: '6px 8px', textAlign: 'right' }}>{formatMoney(n.precioEstimado)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Export CSV ───

function exportarCSVAnual(data, anio) {
  exportarCSV(
    [{ label: 'Indicador', key: 'label' }, { label: 'Valor', key: 'value' }],
    [
      { label: 'Presupuesto total', value: formatMoney(data.techo.total) },
      { label: '% Ejecución', value: data.techo.pctEjecucion + '%' },
      { label: 'Ejecutado', value: formatMoney(data.techo.ejercido) },
      { label: 'Disponible', value: formatMoney(data.techo.disponible) },
      { label: 'Actividades total', value: data.actividades.total },
      { label: 'Pendientes', value: data.actividades.pendientes },
      { label: 'Cerradas', value: data.actividades.cerradas },
      { label: 'Vencidas', value: data.actividades.vencidas },
      { label: 'Expedientes total', value: data.expedientes.total },
      { label: 'Costo total', value: formatMoney(data.expedientes.costoTotal) },
      { label: 'Con documentos', value: data.expedientes.conDocumentos },
      { label: 'Ítems PAP', value: data.pap.totalItems },
      { label: '% Ejecución PAP', value: data.pap.pctEjecucionMonto + '%' },
    ],
    `informe-anual-${anio}`
  );
}

function exportarCSVExpedientes(data, anio) {
  exportarCSV(
    [{ label: 'Código', key: 'codigo' }, { label: 'Estado', key: 'estado' }, { label: 'Urgencia', key: 'urgencia' }, { label: 'Cantidad Solicitada', key: 'cantidadSolicitada' }, { label: 'Costo', key: function(e) { return formatMoney(e.costoEstimado); } }, { label: 'Descripción', key: 'descripcion' }],
    (data.listado || []),
    `reporte-expedientes-${anio}`
  );
}

function exportarCSVPOI(data, anio) {
  exportarCSV(
    [{ label: 'Código', key: 'codigo' }, { label: 'Nombre', key: 'nombre' }, { label: 'Estado', key: 'estado' }, { label: 'Presupuesto', key: function(a) { return formatMoney(a.presupuestoAsignado); } }, { label: 'Ejecutado', key: function(a) { return formatMoney(a.saldoEjecutado); } }, { label: 'Comprometido', key: function(a) { return formatMoney(a.saldoComprometido); } }, { label: 'Disponible', key: function(a) { return formatMoney(a.disponible); } }, { label: 'PAP', key: function(a) { return a.planificado ? 'Cerrado' : 'Abierto'; } }],
    (data.actividades || []),
    `reporte-poi-${anio}`
  );
}

function exportarCSVPAP(data, anio) {
  exportarCSV(
    [{ label: 'Ítem', key: 'nombre' }, { label: 'Actividad', key: 'actividad' }, { label: 'Tipo', key: 'tipo' }, { label: 'Planificadas', key: 'cantidad' }, { label: 'Disponibles', key: 'cantidadDisponible' }, { label: 'Ejecutadas', key: 'cantidadEjecutada' }, { label: 'Precio Unitario', key: function(n) { return formatMoney(n.precioEstimado); } }],
    (data.listado || []),
    `reporte-pap-${anio}`
  );
}

// ─── Export PDF ───

function exportarPDFAnual(data, anio) {
  exportarPDF(`Informe Anual ${anio}`, [
    { titulo: 'Resumen Ejecutivo', kpis: [
      { label: 'Presupuesto total', value: formatMoney(data.techo.total) },
      { label: '% Ejecución', value: data.techo.pctEjecucion + '%' },
      { label: 'Actividades', value: data.actividades.total },
      { label: 'Expedientes', value: data.expedientes.total },
      { label: 'Ítems PAP', value: data.pap.totalItems },
    ]},
    { titulo: 'Expedientes por Estado', tabla: { headers: ['Estado', 'Cantidad'], rows: Object.entries(data.expedientes.porEstado || {}).map(function(e) { return [e[0], String(e[1])]; }) }},
    { titulo: 'Expedientes por Urgencia', tabla: { headers: ['Urgencia', 'Cantidad'], rows: Object.entries(data.expedientes.porUrgencia || {}).map(function(e) { return [e[0], String(e[1])]; }) }},
  ], `informe-anual-${anio}`);
}

function exportarPDFExpedientes(data, anio) {
  exportarPDF(`Expedientes ${anio}`, [
    { titulo: 'Resumen', kpis: [
      { label: 'Total', value: data.total },
      { label: 'Costo total', value: formatMoney(data.totalCosto) },
      { label: 'Con documentos', value: data.conDocumentos },
      { label: 'Sin documentos', value: data.sinDocumentos },
    ]},
    { titulo: 'Listado', tabla: { headers: ['Código', 'Estado', 'Urgencia', 'Cant. Sol.', 'Costo', 'Descripción'], rows: (data.listado || []).map(function(e) { return [e.codigo, e.estado, e.urgencia, String(e.cantidadSolicitada), formatMoney(e.costoEstimado), e.descripcion || '']; }) }},
  ], `reporte-expedientes-${anio}`);
}

function exportarPDFPOI(data, anio) {
  exportarPDF(`POI General ${anio}`, [
    { titulo: 'Resumen', kpis: [
      { label: 'Actividades', value: data.totalActividades },
      { label: 'Presupuesto', value: formatMoney(data.presupuesto.total) },
      { label: 'Ejecución', value: data.presupuesto.pctEjecucion + '%' },
      { label: 'Disponible', value: formatMoney(data.presupuesto.disponible) },
    ]},
    { titulo: 'Actividades', tabla: { headers: ['Código', 'Nombre', 'Estado', 'Presupuesto', 'Ejecutado', 'Comprometido', 'Disponible', 'PAP'], rows: (data.actividades || []).map(function(a) { return [a.codigo, a.nombre, a.estado, formatMoney(a.presupuestoAsignado), formatMoney(a.saldoEjecutado), formatMoney(a.saldoComprometido), formatMoney(a.disponible), a.planificado ? 'Cerrado' : 'Abierto']; }) }},
  ], `reporte-poi-${anio}`);
}

function exportarPDFPAP(data, anio) {
  exportarPDF(`PAP General ${anio}`, [
    { titulo: 'Resumen', kpis: [
      { label: 'Total Ítems', value: data.totalItems },
      { label: 'Ejec. Cantidad', value: data.pctEjecucionCantidad + '%' },
      { label: 'Ejec. Monto', value: data.pctEjecucionMonto + '%' },
      { label: 'Planificados', value: data.cantidades?.planificado },
      { label: 'Disponibles', value: data.cantidades?.disponible },
      { label: 'Ejecutados', value: data.cantidades?.ejecutado },
    ]},
    { titulo: 'Ítems PAP', tabla: { headers: ['Ítem', 'Actividad', 'Tipo', 'Plan.', 'Disp.', 'Ejec.', 'P. Unit.'], rows: (data.listado || []).map(function(n) { return [n.nombre, n.actividad, n.tipo, String(n.cantidad), String(n.cantidadDisponible), String(n.cantidadEjecutada || 0), formatMoney(n.precioEstimado)]; }) }},
  ], `reporte-pap-${anio}`);
}
