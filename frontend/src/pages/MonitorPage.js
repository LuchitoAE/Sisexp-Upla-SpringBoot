import React, { useState } from 'react';
import { HiOutlineDesktopComputer, HiOutlineDatabase, HiOutlineDocumentReport, HiOutlineCube } from 'react-icons/hi';
import InfraTab from './monitor/InfraTab';
import DatosVivoTab from './monitor/DatosVivoTab';
import DiagramasTab from './monitor/DiagramasTab';
import EstructuraTab from './monitor/EstructuraTab';
import './MonitorPage.css';

const TABS = [
  { id: 'infra', label: 'Infraestructura', icon: HiOutlineDesktopComputer },
  { id: 'datos', label: 'Datos en Vivo', icon: HiOutlineDatabase },
  { id: 'diagramas', label: 'Diagramas', icon: HiOutlineDocumentReport },
  { id: 'estructura', label: 'Estructura', icon: HiOutlineCube },
];

export default function MonitorPage({ onBack }) {
  const [activeTab, setActiveTab] = useState('infra');
  const [navigateTarget, setNavigateTarget] = useState(null);

  const handleNavigateToTab = (targetTab, targetId) => {
    setActiveTab(targetTab);
    setNavigateTarget(targetId);
  };

  return (
    <div className="monitor-page">
      <div className="monitor-page-topbar">
        <div className="monitor-page-topbar-left">
          {onBack && (
            <button className="monitor-page-back-btn" onClick={onBack} title="Volver al sistema">
              ← Volver
            </button>
          )}
          <span className="monitor-page-brand">SISEXP-UPLA — Visor del Proyecto</span>
        </div>
        <div className="monitor-page-topbar-center">
          <div className="monitor-page-tabs">
            {TABS.map(t => {
              const Icon = t.icon;
              return (
                <button
                  key={t.id}
                  className={'monitor-page-tab' + (activeTab === t.id ? ' active' : '')}
                  onClick={() => setActiveTab(t.id)}
                >
                  <Icon style={{fontSize:14}} />
                  {t.label}
                </button>
              );
            })}
          </div>
        </div>
        <div className="monitor-page-topbar-right" />
      </div>

      <div className="monitor-page-content">
        {activeTab === 'infra' && (
          <InfraTab onNavigateToTab={handleNavigateToTab} />
        )}
        {activeTab === 'datos' && (
          <DatosVivoTab
            key={'datos-' + (navigateTarget || 'init')}
            initialDbId={navigateTarget}
          />
        )}
        {activeTab === 'diagramas' && (
          <DiagramasTab />
        )}
        {activeTab === 'estructura' && (
          <EstructuraTab
            key={'estructura-' + (navigateTarget || 'init')}
            initialServiceId={navigateTarget}
          />
        )}
      </div>
    </div>
  );
}
