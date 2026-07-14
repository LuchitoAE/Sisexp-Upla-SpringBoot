import React, { useState, useEffect, useRef, useCallback } from 'react';
import mermaid from 'mermaid';
import {
  HiOutlineChevronRight, HiOutlineEye, HiOutlineZoomIn, HiOutlineZoomOut, HiOutlineRefresh,
  HiOutlineGlobe, HiOutlineLockClosed, HiOutlineChartBar, HiOutlineDocumentText, HiOutlineBell,
  HiOutlineUserGroup, HiOutlineTemplate, HiOutlineSwitchHorizontal, HiOutlineLightningBolt, HiOutlineCube,
} from 'react-icons/hi';
import { GLOBAL_DIAGRAMS, AUTH_DIAGRAMS, PRESUPUESTO_DIAGRAMS, EXPEDIENTE_DIAGRAMS, NOTIFICACION_DIAGRAMS } from './diagrams';
import './DiagramasTab.css';

mermaid.initialize({
  startOnLoad: false,
  theme: 'dark',
  securityLevel: 'loose',
  fontFamily: 'Inter, Segoe UI, system-ui, sans-serif',
  themeVariables: {
    primaryColor: '#1e3a5f',
    primaryBorderColor: '#3360aa',
    primaryTextColor: '#e2e8f0',
    secondaryColor: '#162535',
    secondaryBorderColor: '#334b64',
    secondaryTextColor: '#cbd5e1',
    tertiaryColor: '#1a2a1a',
    tertiaryBorderColor: '#335533',
    tertiaryTextColor: '#e2e8f0',
    lineColor: '#38bdf8',
    textColor: '#e2e8f0',
    fontSize: '14px',
  },
  flowchart: { useMaxWidth: true, htmlLabels: true, curve: 'basis' },
  sequence: { useMaxWidth: true, mirrorActors: false, messageMargin: 40 },
  class: { useMaxWidth: true },
});

const MIN_ZOOM = 0.25;
const MAX_ZOOM = 3;
const ZOOM_STEP = 0.25;

const TYPE_ICON = {
  'flowchart': HiOutlineSwitchHorizontal,
  'state': HiOutlineLightningBolt,
  'sequence': HiOutlineSwitchHorizontal,
  'class': HiOutlineCube,
  'image': HiOutlineEye,
};

const TYPE_LABEL = {
  'flowchart': 'Flujo',
  'state': 'Estados',
  'sequence': 'Secuencia',
  'class': 'Clases',
  'image': 'Imagen',
};

const SERVICE_GROUPS = [
  { id: 'global', label: 'Arquitectura Global', color: '#64748b', icon: HiOutlineGlobe, diagrams: GLOBAL_DIAGRAMS },
  { id: 'auth', label: 'AUTH-SERVICE', port: ':8081', color: '#38bdf8', icon: HiOutlineLockClosed, diagrams: AUTH_DIAGRAMS },
  { id: 'presupuesto', label: 'PRESUPUESTO-SERVICE', port: ':8082', color: '#4ade80', icon: HiOutlineChartBar, diagrams: PRESUPUESTO_DIAGRAMS },
  { id: 'expediente', label: 'EXPEDIENTE-SERVICE', port: ':8083', color: '#eab308', icon: HiOutlineDocumentText, diagrams: EXPEDIENTE_DIAGRAMS },
  { id: 'notificacion', label: 'NOTIFICACION-SERVICE', port: ':8084', color: '#c084fc', icon: HiOutlineBell, diagrams: NOTIFICACION_DIAGRAMS },
];

function MermaidDiagram({ chart, id, zoom, onZoomChange }) {
  const containerRef = useRef(null);
  const [svg, setSvg] = useState(null);
  const [error, setError] = useState(null);
  const [renderKey, setRenderKey] = useState(0);

  const doRender = useCallback(async () => {
    setError(null);
    setSvg(null);
    try {
      const uniqueId = 'mermaid-' + id + '-' + renderKey;
      const { svg: rendered } = await mermaid.render(uniqueId, chart);
      setSvg(rendered);
    } catch (e) {
      setError(e.message || 'Error desconocido al renderizar');
      setSvg(null);
    }
  }, [chart, id, renderKey]);

  useEffect(() => {
    doRender();
  }, [doRender]);

  const handleWheel = (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP;
    onZoomChange(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + delta)));
  };

  if (error) {
    return (
      <div className="dg-diagram-error">
        <div className="dg-error-title">Error al renderizar diagrama</div>
        <div className="dg-error-msg">{error}</div>
        <button className="dg-retry-btn" onClick={() => setRenderKey(k => k + 1)}>
          <HiOutlineRefresh style={{fontSize:12}} /> Reintentar
        </button>
        <pre className="dg-code">{chart}</pre>
      </div>
    );
  }

  if (!svg) {
    return <div className="dg-diagram-loading">Renderizando diagrama...</div>;
  }

  return (
    <div className="dg-zoom-container" onWheel={handleWheel}>
      <div
        className="dg-zoom-content"
        style={{ transform: `scale(${zoom})`, transformOrigin: 'top left' }}
        ref={containerRef}
        dangerouslySetInnerHTML={{ __html: svg }}
      />
    </div>
  );
}

function ImageDiagram({ src, alt, zoom, onZoomChange }) {
  const [error, setError] = useState(false);
  const handleWheel = (e) => {
    e.preventDefault();
    const delta = e.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP;
    onZoomChange(Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom + delta)));
  };
  if (error) {
    return (
      <div className="dg-diagram-error">
        <div className="dg-error-title">Error al cargar imagen</div>
        <div className="dg-error-msg">No se pudo cargar: {src}</div>
      </div>
    );
  }
  return (
    <div className="dg-zoom-container" onWheel={handleWheel}>
      <div className="dg-zoom-content" style={{ transform: `scale(${zoom})`, transformOrigin: 'top left' }}>
        <img
          src={src}
          alt={alt}
          style={{ maxWidth: 'none', display: 'block' }}
          onError={() => setError(true)}
        />
      </div>
    </div>
  );
}

export default function DiagramasTab() {
  const [activeServiceId, setActiveServiceId] = useState('global');
  const [activeDiagram, setActiveDiagram] = useState(null);
  const [showCode, setShowCode] = useState({});
  const [zoom, setZoom] = useState(1);

  const serviceGroups = SERVICE_GROUPS.map(g => ({
    ...g,
    isOpen: activeServiceId === g.id,
  }));

  useEffect(() => {
    const group = SERVICE_GROUPS.find(g => g.id === activeServiceId);
    if (group && group.diagrams.length > 0 && !activeDiagram) {
      setActiveDiagram(group.diagrams[0].id);
    }
  }, [activeServiceId]);

  const toggleCode = (id) => {
    setShowCode(prev => ({ ...prev, [id]: !prev[id] }));
  };

  const changeDiagram = (serviceId, diagramId) => {
    setActiveServiceId(serviceId);
    setActiveDiagram(diagramId);
    setZoom(1);
  };

  const zoomIn = () => setZoom(z => Math.min(MAX_ZOOM, z + ZOOM_STEP));
  const zoomOut = () => setZoom(z => Math.max(MIN_ZOOM, z - ZOOM_STEP));
  const zoomReset = () => setZoom(1);

  let currentDiagram = null;
  let currentGroup = null;
  for (const group of SERVICE_GROUPS) {
    const found = group.diagrams.find(d => d.id === activeDiagram);
    if (found) {
      currentDiagram = found;
      currentGroup = group;
      break;
    }
  }

  const totalDiagrams = SERVICE_GROUPS.reduce((sum, g) => sum + g.diagrams.length, 0);

  return (
    <div className="dg-root">
      <div className="dg-sidebar">
        <div className="dg-sidebar-header">
          <HiOutlineTemplate style={{fontSize:14}} />
          <span>Diagramas ICONIX</span>
          <span className="dg-total-badge">{totalDiagrams}</span>
        </div>

        {SERVICE_GROUPS.map(group => {
          const GIcon = group.icon;
          const isActive = activeServiceId === group.id;
          return (
            <div key={group.id} className="dg-service-group">
              <div
                className={'dg-service-header' + (isActive ? ' active' : '')}
                onClick={() => setActiveServiceId(group.id)}
                style={{ '--svc-color': group.color }}
              >
                <HiOutlineChevronRight
                  style={{
                    fontSize: 10,
                    transform: isActive ? 'rotate(90deg)' : 'none',
                    transition: 'transform 0.15s',
                    color: group.color,
                  }}
                />
                <GIcon style={{fontSize:13, color: group.color}} />
                <span className="dg-service-label">{group.label}</span>
                {group.port && <span className="dg-service-port">{group.port}</span>}
                <span className="dg-service-count">{group.diagrams.length}</span>
              </div>
              {isActive && group.diagrams.map(d => {
                const TIcon = TYPE_ICON[d.type] || HiOutlineCube;
                return (
                  <div
                    key={d.id}
                    className={'dg-diagram-item' + (activeDiagram === d.id ? ' active' : '')}
                    onClick={() => changeDiagram(group.id, d.id)}
                  >
                    <TIcon style={{fontSize:11, flexShrink:0}} />
                    <span className="dg-diagram-name">{d.title}</span>
                    <span className="dg-diagram-type">{TYPE_LABEL[d.type] || d.type}</span>
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>

      <div className="dg-content">
        {currentDiagram ? (
          <div className="dg-viewer">
            <div className="dg-viewer-header">
              <div>
                <div className="dg-viewer-service" style={{ color: currentGroup?.color }}>
                  {currentGroup?.label}
                </div>
                <div className="dg-viewer-title">{currentDiagram.title}</div>
                <div className="dg-viewer-desc">{currentDiagram.desc}</div>
              </div>
              <div className="dg-viewer-actions">
                <button className="dg-code-toggle" onClick={() => toggleCode(currentDiagram.id)}>
                  <HiOutlineEye style={{fontSize:12}} />
                  {showCode[currentDiagram.id] ? 'Ocultar' : 'Codigo'}
                </button>
                <div className="dg-zoom-controls">
                  <button className="dg-zoom-btn" onClick={zoomOut} disabled={zoom <= MIN_ZOOM} title="Alejar">
                    <HiOutlineZoomOut style={{fontSize:13}} />
                  </button>
                  <span className="dg-zoom-label" onClick={zoomReset} title="Reset zoom">
                    {Math.round(zoom * 100)}%
                  </span>
                  <button className="dg-zoom-btn" onClick={zoomIn} disabled={zoom >= MAX_ZOOM} title="Acercar">
                    <HiOutlineZoomIn style={{fontSize:13}} />
                  </button>
                </div>
              </div>
            </div>

            {showCode[currentDiagram.id] && (
              <pre className="dg-code">{currentDiagram.mermaid}</pre>
            )}

            <div className="dg-diagram-wrap">
              {currentDiagram.type === 'image' ? (
                <ImageDiagram
                  key={currentDiagram.id}
                  src={currentDiagram.imageSrc}
                  alt={currentDiagram.title}
                  zoom={zoom}
                  onZoomChange={setZoom}
                />
              ) : (
                <MermaidDiagram
                  key={currentDiagram.id + '-' + (showCode[currentDiagram.id] ? '1' : '0')}
                  chart={currentDiagram.mermaid}
                  id={currentDiagram.id}
                  zoom={zoom}
                  onZoomChange={setZoom}
                />
              )}
            </div>
          </div>
        ) : (
          <div className="dg-empty">
            <div className="dg-empty-title">{totalDiagrams} Diagramas ICONIX</div>
            <div className="dg-empty-desc">Selecciona un servicio y luego un diagrama para visualizarlo.</div>
          </div>
        )}
      </div>
    </div>
  );
}
