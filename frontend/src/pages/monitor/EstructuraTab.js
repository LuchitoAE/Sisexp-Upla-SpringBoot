import React, { useState, useEffect } from 'react';
import { HiOutlineDatabase, HiOutlineCog, HiOutlineLightningBolt } from 'react-icons/hi';
import { SERVICE_ENTITIES, DB_TABLES } from './schema';
import './EstructuraTab.css';

function EntityCard({ entity }) {
  return (
    <div className="es-entity-card">
      <div className="es-entity-header">
        <span className="es-entity-name">{entity.name}</span>
        <span className="es-entity-table">{entity.table}</span>
      </div>
      <div className="es-entity-fields">
        {entity.fields.map((f, i) => (
          <div key={i} className="es-field-row">
            <span className="es-field-name">{f.name}</span>
            <span className="es-field-type">{f.type}</span>
            <span className="es-field-flags">
              {f.pk && <span className="es-flag pk">PK</span>}
              {f.nullable === false && <span className="es-flag nn">NN</span>}
              {f.unique && <span className="es-flag uniq">UQ</span>}
              {f.fk && <span className="es-flag fk">FK→{f.fk}</span>}
              {f.jsonIgnore && <span className="es-flag json">@JsonIgnore</span>}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}

function EndpointRow({ ep }) {
  const methodColors = {
    GET: '#22c55e', POST: '#eab308', PUT: '#3b82f6', PATCH: '#a855f7', DELETE: '#ef4444'
  };
  return (
    <div className="es-endpoint-row">
      <span className="es-ep-method" style={{ color: methodColors[ep.method] || '#94a3b8', borderColor: methodColors[ep.method] || '#334b64' }}>
        {ep.method}
      </span>
      <span className="es-ep-path">{ep.path}</span>
      <span className="es-ep-auth">{ep.auth === 'No' ? <span className="es-ep-public">PUBLICO</span> : <span className="es-ep-jwt">JWT</span>}</span>
    </div>
  );
}

function ServiceStructure({ service, selected }) {
  const info = SERVICE_ENTITIES[service];
  if (!info) return null;

  return (
    <div className="es-service-section" style={{ display: selected === service ? 'block' : 'none' }}>
      <div className="es-service-header">
        <div className="es-service-title">
          <HiOutlineCog style={{fontSize:18}} />
          <span>{info.name}</span>
          <span className="es-service-port">Puerto {info.port}</span>
        </div>
        <div className="es-service-db">
          <HiOutlineDatabase style={{fontSize:14}} />
          <span>{info.dbLabel}</span>
        </div>
      </div>

      <div className="es-section">
        <div className="es-section-title">
          <HiOutlineDatabase style={{fontSize:14}} />
          Entidades ({info.entities.length})
        </div>
        <div className="es-entities-grid">
          {info.entities.map(e => (
            <EntityCard key={e.name} entity={e} />
          ))}
        </div>
      </div>

      {info.enums && info.enums.length > 0 && (
        <div className="es-section">
          <div className="es-section-title">
            <span>Enums ({info.enums.length})</span>
          </div>
          <div className="es-enums-list">
            {info.enums.map((e, i) => (
              <div key={i} className="es-enum-item">{e}</div>
            ))}
          </div>
        </div>
      )}

      <div className="es-section">
        <div className="es-section-title">
          <HiOutlineLightningBolt style={{fontSize:14}} />
          Endpoints ({info.endpoints.length})
        </div>
        <div className="es-endpoints-list">
          {info.endpoints.map((ep, i) => (
            <EndpointRow key={i} ep={ep} />
          ))}
        </div>
      </div>
    </div>
  );
}

function DbStructure({ dbId, selected }) {
  const info = DB_TABLES[dbId];
  if (!info) return null;
  const svcInfo = SERVICE_ENTITIES[info.service];

  return (
    <div className="es-service-section" style={{ display: selected === dbId ? 'block' : 'none' }}>
      <div className="es-service-header">
        <div className="es-service-title">
          <HiOutlineDatabase style={{fontSize:18}} />
          <span>{info.label}</span>
        </div>
        <div className="es-service-db">
          <span>Servicio: {info.service}</span>
        </div>
      </div>

      <div className="es-section">
        <div className="es-section-title">
          <HiOutlineDatabase style={{fontSize:14}} />
          Tablas ({info.tables.length})
        </div>
        <div className="es-tables-list">
          {info.tables.map(t => {
            const entity = svcInfo?.entities.find(e => e.table === t.name);
            return (
              <div key={t.name} className="es-table-item">
                <div className="es-table-header-row">
                  <span className="es-table-name">{t.name}</span>
                  <span className="es-table-entity">{t.entity}</span>
                  <span className="es-table-count">{t.count} reg.</span>
                </div>
                <div className="es-table-cols">
                  {t.cols.map((col, i) => (
                    <span key={i} className="es-col-pill">{col}</span>
                  ))}
                </div>
                {entity && (
                  <div className="es-table-fields">
                    {entity.fields.map((f, i) => (
                      <div key={i} className="es-field-row">
                        <span className="es-field-name">{f.name}</span>
                        <span className="es-field-type">{f.type}</span>
                        <span className="es-field-flags">
                          {f.pk && <span className="es-flag pk">PK</span>}
                          {f.nullable === false && <span className="es-flag nn">NN</span>}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export default function EstructuraTab({ initialServiceId }) {
  const [activeType, setActiveType] = useState('servicios');
  const [selected, setSelected] = useState(null);

  const services = Object.keys(SERVICE_ENTITIES);
  const dbs = Object.keys(DB_TABLES);

  useEffect(() => {
    if (initialServiceId) {
      const entityInfo = SERVICE_ENTITIES[initialServiceId];
      if (entityInfo) {
        setActiveType('servicios');
        setSelected(initialServiceId);
        return;
      }
      const dbInfo = DB_TABLES[initialServiceId];
      if (dbInfo) {
        setActiveType('bases-de-datos');
        setSelected(initialServiceId);
        return;
      }
    }
    if (!selected) {
      setSelected(services[0]);
    }
  }, [initialServiceId]);

  return (
    <div className="es-root">
      <div className="es-sidebar">
        <div className="es-sidebar-title">Estructura</div>

        <div className="es-nav-group">
          <div
            className={'es-nav-group-title' + (activeType === 'servicios' ? ' active' : '')}
            onClick={() => { setActiveType('servicios'); setSelected(services[0]); }}
          >
            <HiOutlineCog style={{fontSize:12}} />
            Servicios
          </div>
          {activeType === 'servicios' && services.map(s => {
            const info = SERVICE_ENTITIES[s];
            return (
              <div
                key={s}
                className={'es-nav-item' + (selected === s ? ' active' : '')}
                onClick={() => setSelected(s)}
              >
                <span className="es-nav-item-name">{info.name}</span>
                <span className="es-nav-item-meta">{info.entities.length} ent.</span>
              </div>
            );
          })}
        </div>

        <div className="es-nav-group">
          <div
            className={'es-nav-group-title' + (activeType === 'bases-de-datos' ? ' active' : '')}
            onClick={() => { setActiveType('bases-de-datos'); setSelected(dbs[0]); }}
          >
            <HiOutlineDatabase style={{fontSize:12}} />
            Bases de Datos
          </div>
          {activeType === 'bases-de-datos' && dbs.map(dbId => {
            const info = DB_TABLES[dbId];
            return (
              <div
                key={dbId}
                className={'es-nav-item' + (selected === dbId ? ' active' : '')}
                onClick={() => setSelected(dbId)}
              >
                <span className="es-nav-item-name">{info.label}</span>
                <span className="es-nav-item-meta">{info.tables.length} tablas</span>
              </div>
            );
          })}
        </div>
      </div>

      <div className="es-content">
        {activeType === 'servicios' && services.map(s => (
          <ServiceStructure key={s} service={s} selected={selected} />
        ))}
        {activeType === 'bases-de-datos' && dbs.map(dbId => (
          <DbStructure key={dbId} dbId={dbId} selected={selected} />
        ))}
      </div>
    </div>
  );
}
