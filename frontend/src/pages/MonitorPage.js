import React, { useState, useEffect, useRef, useCallback, memo } from 'react';
import { useRecorder, getRecordings } from '../contexts/RecorderContext';
import { API_URL } from '../api/client';
import './MonitorPage.css';

const POLL_MS = 5000;

const serviceDefs = [
  { id: 'nginx', title: 'NGINX Frontend', port: ':80', x: 40, y: 170, icon: '🌐', cls: 'gateway', type: 'proxy' },
  { id: 'api-gateway', title: 'API Gateway', port: ':8080', x: 280, y: 90, icon: '⚙', cls: 'gateway', type: 'gateway' },
  { id: 'eureka-server', title: 'Eureka Server', port: ':8761', x: 560, y: 30, icon: '🔍', cls: 'discovery', type: 'discovery' },
  { id: 'auth-service', title: 'AUTH-SERVICE', port: ':8081', x: 540, y: 220, icon: '🔒', type: 'service' },
  { id: 'presupuesto-service', title: 'PRESUPUESTO-SERVICE', port: ':8082', x: 240, y: 340, icon: '📊', type: 'service' },
  { id: 'expediente-service', title: 'EXPEDIENTE-SERVICE', port: ':8083', x: 500, y: 410, icon: '📄', type: 'service' },
  { id: 'notificacion-service', title: 'NOTIFICACION-SERVICE', port: ':8084', x: 800, y: 300, icon: '🔔', type: 'service' },
  { id: 'auth-db', title: 'PostgreSQL Auth', port: ':5433', x: 760, y: 130, icon: '🗄', cls: 'db', type: 'db' },
  { id: 'presupuesto-db', title: 'PostgreSQL Presup.', port: ':5434', x: 100, y: 450, icon: '🗄', cls: 'db', type: 'db' },
  { id: 'expediente-db', title: 'PostgreSQL Exped.', port: ':5435', x: 620, y: 530, icon: '🗄', cls: 'db', type: 'db' },
  { id: 'notificacion-db', title: 'PostgreSQL Notif.', port: ':5436', x: 940, y: 420, icon: '🗄', cls: 'db', type: 'db' },
  { id: 'rabbitmq', title: 'RabbitMQ', port: ':5672', x: 860, y: 510, icon: '🐰', cls: 'broker', type: 'broker' },
];

const apiKeyToId = {
  'AUTH-SERVICE': 'auth-service', 'PRESUPUESTO-SERVICE': 'presupuesto-service',
  'EXPEDIENTE-SERVICE': 'expediente-service', 'NOTIFICACION-SERVICE': 'notificacion-service',
  'API-GATEWAY': 'api-gateway', 'EUREKA-SERVER': 'eureka-server', 'NGINX': 'nginx'
};

const dbNodes = {
  'auth-db': 'AUTH-SERVICE', 'presupuesto-db': 'PRESUPUESTO-SERVICE',
  'expediente-db': 'EXPEDIENTE-SERVICE', 'notificacion-db': 'NOTIFICACION-SERVICE'
};

const edges = [
  ['nginx', 'api-gateway', '#38bdf8', 'solid'],
  ['api-gateway', 'auth-service', '#38bdf8', 'solid'],
  ['api-gateway', 'presupuesto-service', '#38bdf8', 'solid'],
  ['api-gateway', 'expediente-service', '#38bdf8', 'solid'],
  ['api-gateway', 'notificacion-service', '#38bdf8', 'solid'],
  ['auth-service', 'auth-db', '#4ade80', 'dashed'],
  ['presupuesto-service', 'presupuesto-db', '#4ade80', 'dashed'],
  ['expediente-service', 'expediente-db', '#4ade80', 'dashed'],
  ['notificacion-service', 'notificacion-db', '#4ade80', 'dashed'],
  ['auth-service', 'eureka-server', '#fb923c', 'dotted'],
  ['presupuesto-service', 'eureka-server', '#fb923c', 'dotted'],
  ['expediente-service', 'eureka-server', '#fb923c', 'dotted'],
  ['notificacion-service', 'eureka-server', '#fb923c', 'dotted'],
  ['api-gateway', 'eureka-server', '#fb923c', 'dotted'],
  ['expediente-service', 'rabbitmq', '#c084fc', 'dotted'],
  ['rabbitmq', 'notificacion-service', '#c084fc', 'dotted'],
];

function EdgeCanvas({ selected, nodesElRef }) {
  const canvasRef = useRef(null);
  const rafRef = useRef(null);

  useEffect(() => {
    const c = canvasRef.current;
    if (!c) return;
    const ctx = c.getContext('2d');
    const dpr = window.devicePixelRatio || 1;

    function resize() {
      const w = c.parentElement.clientWidth;
      const h = c.parentElement.clientHeight;
      c.width = w * dpr;
      c.height = h * dpr;
      c.style.width = w + 'px';
      c.style.height = h + 'px';
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }
    resize();
    window.addEventListener('resize', resize);

    function draw() {
      const w = c.width / dpr;
      const h = c.height / dpr;
      ctx.clearRect(0, 0, w, h);

      edges.forEach(([from, to, color, style]) => {
        const fe = document.getElementById('mn-' + from);
        const te = document.getElementById('mn-' + to);
        if (!fe || !te) return;

        const fr = fe.getBoundingClientRect();
        const tr = te.getBoundingClientRect();
        const parentR = c.parentElement.getBoundingClientRect();
        const fx = fr.left - parentR.left + fr.width / 2;
        const fy = fr.top - parentR.top + fr.height / 2;
        const tx = tr.left - parentR.left + tr.width / 2;
        const ty = tr.top - parentR.top + tr.height / 2;

        ctx.save();
        ctx.globalAlpha = selected && (selected.id === from || selected.id === to) ? 1 :
          selected ? 0.1 : 0.4;
        ctx.strokeStyle = color;
        ctx.lineWidth = selected && (selected.id === from || selected.id === to) ? 2.5 : 1.2;
        ctx.beginPath();

        if (style === 'dashed') ctx.setLineDash([8, 5]);
        else if (style === 'dotted') ctx.setLineDash([3, 5]);
        else ctx.setLineDash([]);

        ctx.moveTo(fx, fy);
        const mx = (fx + tx) / 2;
        const my = (fy + ty) / 2 + ((from + to).length % 3 - 1) * 18;
        ctx.quadraticCurveTo(mx, my, tx, ty);
        ctx.stroke();

        ctx.setLineDash([]);
        const dx = tx - mx, dy = ty - my;
        const dist = Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.001) {
          const ux = dx / dist, uy = dy / dist;
          ctx.beginPath();
          ctx.moveTo(tx, ty);
          ctx.lineTo(tx - ux * 8 + uy * 3, ty - uy * 8 - ux * 3);
          ctx.lineTo(tx - ux * 8 - uy * 3, ty - uy * 8 + ux * 3);
          ctx.fillStyle = color;
          ctx.fill();
        }
        ctx.restore();
      });
      rafRef.current = requestAnimationFrame(draw);
    }
    draw();

    return () => {
      cancelAnimationFrame(rafRef.current);
      window.removeEventListener('resize', resize);
    };
  }, [selected]);

  return <canvas ref={canvasRef} className="monitor-canvas" />;
}

function NodeCard({ def, status, selected, onClick, replayHighlight }) {
  const st = status || 'UNKNOWN';
  return (
    <div
      id={'mn-' + def.id}
      className={'monitor-node' + (def.cls ? ' ' + def.cls : '') + (selected ? ' selected' : '') + (replayHighlight ? ' replay-hit' : '')}
      style={{ left: def.x, top: def.y }}
      onClick={(e) => { e.stopPropagation(); onClick(def); }}
    >
      <span className={'node-dot status-' + st} />
      <div className="node-icon">{def.icon}</div>
      <div className="node-title">{def.title}</div>
      <div className="node-port">{def.port}</div>
    </div>
  );
}

function DetailPanel({ selected, statuses, activity, onClose, onNavigate }) {
  if (!selected) return null;
  const s = statuses[selected.id];
  const st = s?.status || 'UNKNOWN';
  const svcName = Object.entries(apiKeyToId).find(([, v]) => v === selected.id)?.[0] || selected.id;

  return (
    <div className="monitor-detail show">
      <span className="detail-close" onClick={onClose}>&times;</span>
      <h3>{selected.title} {selected.port}</h3>
      <div className="detail-row">
        <span className="l">Status</span>
        <span className={st === 'UP' ? 'text-up' : 'text-down'}>{st}</span>
      </div>
      {s?.host && (
        <div className="detail-row">
          <span className="l">Host</span><span>{s.host}</span>
        </div>
      )}
      {s?.port && (
        <div className="detail-row">
          <span className="l">Puerto</span><span>{s.port}</span>
        </div>
      )}
      {s?.instances !== undefined && (
        <div className="detail-row">
          <span className="l">Instancias</span><span>{s.instances}</span>
        </div>
      )}
      {selected.type !== 'db' && selected.type !== 'broker' && selected.type !== 'proxy' && (
        <button className="detail-navigate" onClick={() => onNavigate(selected)}>
          Ir al modulo →
        </button>
      )}

      {activity.length > 0 && (
        <div className="detail-actions">
          <div className="detail-actions-title">Acciones recientes:</div>
          {activity.slice(0, 8).map((a, i) => (
            <div key={i} className="detail-action-row">
              <span className="action-time">{new Date(a.ts || a.timestamp).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
              <span className="action-desc">{a.description || (a.method + ' ' + a.path)}</span>
            </div>
          ))}
        </div>
      )}

      {s?.components && (
        <div className="detail-components">
          <div className="detail-actions-title">Componentes:</div>
          {Object.entries(s.components).map(([k, v]) => (
            <div key={k} className={'comp-row ' + (v === 'UP' ? 'comp-up' : 'comp-down')}>
              <span>{k}</span><span>{v}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ActivityFeed({ activities, replayActions, replayIndex, isReplaying }) {
  const feedRef = useRef(null);
  const items = isReplaying ? replayActions : activities;

  useEffect(() => {
    if (feedRef.current) {
      feedRef.current.scrollTop = feedRef.current.scrollHeight;
    }
  }, [items]);

  if (isReplaying && items) {
    return (
      <div className="activity-feed" ref={feedRef}>
        <div className="activity-header-replay">▶ REPRODUCIENDO — Accion {replayIndex + 1}/{items.length}</div>
        {items.map((a, i) => (
          <div key={i} className={'activity-row' + (i <= replayIndex ? ' played' : ' pending')}>
            <span className="act-time">{new Date(a.ts).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
            <span className="act-method">{a.method}</span>
            <span className="act-path">{a.path}</span>
            <span className="act-status">{a.status}</span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="activity-feed" ref={feedRef}>
      {items.length === 0 && (
        <div className="activity-empty">Esperando actividad del sistema...</div>
      )}
      {items.map((a, i) => {
        const isError = a.status >= 400;
        return (
          <div key={i} className={'activity-row' + (isError ? ' error' : '')}>
            <span className="act-time">{new Date(a.timestamp).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
            <span className="act-dot" style={{ background: isError ? '#ef4444' : '#22c55e' }} />
            <span className="act-user">{a.userEmail || 'anonimo'}</span>
            <span className="act-desc">{a.description}</span>
          </div>
        );
      })}
    </div>
  );
}

function RecordingsPanel({ recordings, onPlay, onDelete }) {
  if (recordings.length === 0) {
    return (
      <div className="recordings-panel">
        <div className="activity-empty">No hay grabaciones. Usa el boton Grabar en el header para capturar acciones.</div>
      </div>
    );
  }

  return (
    <div className="recordings-panel">
      {recordings.map(r => (
        <div key={r.id} className="recording-card">
          <div className="rec-info">
            <div className="rec-name">{r.name}</div>
            <div className="rec-meta">{r.totalActions} acciones · {r.duration}s</div>
            <div className="rec-date">{new Date(r.date).toLocaleString('es-PE')}</div>
          </div>
          <div className="rec-actions">
            <button className="rec-btn play" onClick={() => onPlay(r)} title="Reproducir">▶</button>
            <button className="rec-btn delete" onClick={() => onDelete(r.id)} title="Eliminar">✕</button>
          </div>
        </div>
      ))}
    </div>
  );
}

const SERVICE_TO_NAV = {
  'auth-service': 'usuarios',
  'presupuesto-service': 'reportes',
  'expediente-service': 'expedientes',
  'notificacion-service': null,
  'nginx': null, 'api-gateway': null, 'eureka-server': null
};

export default function MonitorPage() {
  const [statuses, setStatuses] = useState({});
  const [selected, setSelected] = useState(null);
  const [paused, setPaused] = useState(false);
  const [latency, setLatency] = useState(null);
  const [activities, setActivities] = useState([]);
  const [tab, setTab] = useState('activity');
  const [recordings, setRecordings] = useState([]);
  const { deleteRecording } = useRecorder();
  const [replaying, setReplaying] = useState(null);
  const [replayIndex, setReplayIndex] = useState(-1);
  const [replaySpeed, setReplaySpeed] = useState(1);
  const replayTimerRef = useRef(null);
  const activityTimerRef = useRef(null);

  const pollStatus = useCallback(async () => {
    const start = performance.now();
    try {
      const res = await fetch(API_URL + '/status');
      const data = await res.json();
      setLatency(Math.round(performance.now() - start));

      const newStatuses = {};
      for (const [apiKey, nodeId] of Object.entries(apiKeyToId)) {
        const svc = data[apiKey];
        if (svc) {
          newStatuses[nodeId] = {
            status: svc.status, host: svc.host, port: svc.port,
            instances: svc.instances, components: svc.components, detail: svc.detail
          };
        } else {
          newStatuses[nodeId] = { status: 'UNKNOWN' };
        }
      }
      for (const [dbId, svcKey] of Object.entries(dbNodes)) {
        const d = data[svcKey]?.components?.db;
        newStatuses[dbId] = { status: d === 'UP' ? 'UP' : d ? 'DOWN' : 'UNKNOWN' };
      }
      const rHealth = data['EXPEDIENTE-SERVICE']?.components?.rabbit ||
        data['NOTIFICACION-SERVICE']?.components?.rabbit;
      newStatuses['rabbitmq'] = { status: rHealth === 'UP' ? 'UP' : rHealth ? 'DOWN' : 'UNKNOWN' };

      setStatuses(newStatuses);
    } catch {
      setLatency(null);
    }
  }, []);

  const fetchActivity = useCallback(async () => {
    try {
      const res = await fetch(API_URL + '/monitor/activity?since=5');
      if (res.ok) {
        const data = await res.json();
        setActivities(data);
      }
    } catch { /* ignore */ }
  }, []);

  useEffect(() => {
    pollStatus();
    fetchActivity();
    if (!paused) {
      const s = setInterval(pollStatus, POLL_MS);
      activityTimerRef.current = setInterval(fetchActivity, POLL_MS);
      return () => { clearInterval(s); clearInterval(activityTimerRef.current); };
    }
  }, [paused, pollStatus, fetchActivity]);

  useEffect(() => {
    setRecordings(getRecordings());
  }, []);

  const togglePause = () => {
    setPaused(p => {
      if (p) {
        pollStatus();
        fetchActivity();
      }
      return !p;
    });
  };

  const upCount = Object.values(statuses).filter(s => s.status === 'UP').length;
  const downCount = Object.values(statuses).filter(s => s.status === 'DOWN' || s.status === 'UNKNOWN').length;

  const filteredActivity = selected
    ? activities.filter(a => {
      const svcName = Object.entries(apiKeyToId).find(([, v]) => v === selected.id)?.[0];
      return svcName && a.service === svcName;
    })
    : activities;

  const startReplay = (rec) => {
    setReplaying(rec);
    setReplayIndex(-1);
    setTab('activity');
    let i = 0;
    replayTimerRef.current = setInterval(() => {
      if (i >= rec.actions.length) {
        clearInterval(replayTimerRef.current);
        setReplaying(null);
        setReplayIndex(-1);
        return;
      }
      setReplayIndex(i);
      i++;
    }, 500 / replaySpeed);
  };

  const stopReplay = () => {
    if (replayTimerRef.current) clearInterval(replayTimerRef.current);
    setReplaying(null);
    setReplayIndex(-1);
  };

  const handleNavigate = (node) => {
    const route = SERVICE_TO_NAV[node.id];
    if (route && window.__SISEXP_NAVIGATE__) {
      window.__SISEXP_NAVIGATE__(route);
    }
  };

  const getReplayHighlight = () => {
    if (!replaying || replayIndex < 0) return null;
    const action = replaying.actions[replayIndex];
    if (!action) return null;
    for (const [apiKey, nodeId] of Object.entries(apiKeyToId)) {
      if (action.path.includes('/' + apiKey.toLowerCase().replace('-service', '') + '/') ||
        action.path.includes('/expedientes') || action.path.includes('/notificaciones')) {
        return nodeId;
      }
    }
    return 'api-gateway';
  };

  const replayHighlight = getReplayHighlight();

  return (
    <div className="monitor-page">
      <div className="monitor-topbar">
        <div className="topbar-left">
          <span className={'topbar-dot' + (downCount > 0 ? ' has-down' : '')} />
          <span className="topbar-count">{upCount}/{serviceDefs.length} UP</span>
        </div>
        <div className="topbar-center">
          {latency !== null && <span className="topbar-lat">⏱ {latency}ms</span>}
          {replaying && (
            <span className="topbar-replaying">
              ▶ {replaying.name} — {replayIndex + 1}/{replaying.actions.length}
            </span>
          )}
        </div>
        <div className="topbar-right">
          {replaying ? (
            <button className="topbar-btn" onClick={stopReplay}>⏹ Detener</button>
          ) : (
            <>
              <button className="topbar-btn" onClick={togglePause}>
                {paused ? '▶ Reanudar' : '⏸ Pausar'}
              </button>
              <button className="topbar-btn" onClick={() => { pollStatus(); fetchActivity(); }}>
                🔄 Sondear
              </button>
            </>
          )}
        </div>
      </div>

      <div className="monitor-graph">
        <EdgeCanvas selected={selected} />
        <div className="monitor-nodes" onClick={() => setSelected(null)}>
          {serviceDefs.map(def => (
            <NodeCard
              key={def.id}
              def={def}
              status={statuses[def.id]?.status}
              selected={selected?.id === def.id}
              replayHighlight={replayHighlight === def.id}
              onClick={setSelected}
            />
          ))}
        </div>
        <DetailPanel
          selected={selected}
          statuses={statuses}
          activity={filteredActivity}
          onClose={() => setSelected(null)}
          onNavigate={handleNavigate}
        />
      </div>

      <div className="monitor-bottom">
        <div className="bottom-tabs">
          <button className={'bottom-tab' + (tab === 'activity' ? ' active' : '')} onClick={() => { setTab('activity'); stopReplay(); }}>
            📋 Actividad
          </button>
          <button className={'bottom-tab' + (tab === 'recordings' ? ' active' : '')} onClick={() => { setTab('recordings'); stopReplay(); }}>
            📼 Grabaciones ({recordings.length})
          </button>
          {replaying && (
            <div className="replay-controls">
              <button className="replay-ctrl-btn" onClick={stopReplay}>⏹</button>
              <span className="replay-speed-label">Velocidad:</span>
              {[1, 2, 4].map(s => (
                <button key={s} className={'replay-speed-btn' + (replaySpeed === s ? ' active' : '')}
                  onClick={() => setReplaySpeed(s)}>{s}x</button>
              ))}
            </div>
          )}
        </div>
        {tab === 'activity' ? (
          <ActivityFeed
            activities={replaying ? [] : activities}
            replayActions={replaying?.actions}
            replayIndex={replayIndex}
            isReplaying={!!replaying}
          />
        ) : (
          <RecordingsPanel
            recordings={recordings}
            onPlay={startReplay}
            onDelete={(id) => {
              deleteRecording(id);
              setRecordings(getRecordings());
            }}
          />
        )}
      </div>
    </div>
  );
}
