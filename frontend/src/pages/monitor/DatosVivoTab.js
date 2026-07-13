import React, { useState, useEffect, useRef, useCallback } from 'react';
import { HiOutlineRefresh, HiOutlineChevronDown, HiOutlineChevronRight, HiOutlineClock, HiOutlineExclamation } from 'react-icons/hi';
import { API_URL } from '../../api/client';
import { DB_TABLES } from './schema';
import './DatosVivoTab.css';

function TableCard({ table, expanded, onToggle, data, loading, error, lastUpdate, onFetch }) {
  const isOpen = expanded[table.name];
  const hasData = data !== undefined;
  const needsFetch = isOpen && !hasData && !loading;

  useEffect(() => {
    if (needsFetch && onFetch) {
      onFetch(table);
    }
  }, [needsFetch, onFetch, table]);

  return (
    <div className="dv-card">
      <div className="dv-card-header" onClick={() => onToggle(table.name)}>
        <div className="dv-card-title">
          {isOpen ? <HiOutlineChevronDown style={{fontSize:14}} /> : <HiOutlineChevronRight style={{fontSize:14}} />}
          <span className="dv-table-name">{table.name}</span>
          <span className="dv-entity-label">{table.entity}</span>
        </div>
        <div className="dv-card-meta">
          {lastUpdate && (
            <span className="dv-last-update" title="Ultima actualizacion">
              <HiOutlineClock style={{fontSize:11}} />
              {lastUpdate.toLocaleTimeString('es-PE', {hour:'2-digit',minute:'2-digit',second:'2-digit'})}
            </span>
          )}
          {error ? (
            <span className="dv-row-count error" title={error}>
              <HiOutlineExclamation style={{fontSize:10}} /> Error
            </span>
          ) : loading ? (
            <span className="dv-row-count loading">cargando...</span>
          ) : data ? (
            <span className="dv-row-count ok">{data.length} filas</span>
          ) : (
            <span className="dv-row-count">{table.count} reg.</span>
          )}
        </div>
      </div>

      {isOpen && (
        <div className="dv-card-body">
          {error && (
            <div className="dv-error">
              <HiOutlineExclamation style={{fontSize:14}} />
              <div className="dv-error-msg">{error}</div>
              <button className="dv-retry-btn" onClick={() => onFetch && onFetch(table)}>Reintentar</button>
            </div>
          )}
          {loading && <div className="dv-loading">Consultando API...</div>}
          {!loading && !error && !data && (
            <div className="dv-loading">Abriendo...</div>
          )}
          {!loading && !error && data && data.length === 0 && (
            <div className="dv-empty">Sin registros</div>
          )}
          {!loading && !error && data && data.length > 0 && (
            <div className="dv-table-wrap">
              <table className="dv-table">
                <thead>
                  <tr>
                    {table.cols.map(col => (
                      <th key={col}>{col}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {data.slice(0, 50).map((row, i) => (
                    <tr key={row.id || i}>
                      {table.cols.map(col => (
                        <td key={col} title={formatCell(row[col])}>
                          {formatCell(row[col])}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
              {data.length > 50 && (
                <div className="dv-truncated">Mostrando 50 de {data.length} registros</div>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function formatCell(val) {
  if (val === null || val === undefined) return '-';
  if (typeof val === 'boolean') return val ? 'Si' : 'No';
  if (typeof val === 'object') return JSON.stringify(val).substring(0, 60);
  return String(val);
}

export default function DatosVivoTab({ initialDbId }) {
  const [expanded, setExpanded] = useState({});
  const [tableData, setTableData] = useState({});
  const [loading, setLoading] = useState({});
  const [errors, setErrors] = useState({});
  const [lastUpdate, setLastUpdate] = useState({});
  const [paused, setPaused] = useState(true);
  const [refreshingAll, setRefreshingAll] = useState(false);
  const timerRef = useRef(null);

  const tableList = Object.values(DB_TABLES).flatMap(db =>
    db.tables.filter(t => t.endpoint).map(t => ({ ...t, db: db.label }))
  );

  const fetchTable = useCallback(async (table) => {
    const token = localStorage.getItem('jwt_token');
    setLoading(prev => ({ ...prev, [table.name]: true }));
    setErrors(prev => {
      const next = { ...prev };
      delete next[table.name];
      return next;
    });
    try {
      let url = API_URL + table.endpoint;
      if (table.name === 'notificaciones') {
        url += '?usuarioId=1';
      }
      const res = await fetch(url, {
        headers: token ? { Authorization: `Bearer ${token}` } : {}
      });
      if (!res.ok) {
        const text = await res.text().catch(() => '');
        throw new Error(`HTTP ${res.status}${text ? ': ' + text.substring(0, 80) : ''}`);
      }
      const data = await res.json();
      const arr = Array.isArray(data) ? data : (data.content || data.data || []);
      setTableData(prev => ({ ...prev, [table.name]: arr }));
      setLastUpdate(prev => ({ ...prev, [table.name]: new Date() }));
    } catch (e) {
      setErrors(prev => ({ ...prev, [table.name]: e.message }));
    } finally {
      setLoading(prev => ({ ...prev, [table.name]: false }));
    }
  }, []);

  const refreshExpanded = useCallback(() => {
    const expandedTables = tableList.filter(t => expanded[t.name] && t.endpoint);
    expandedTables.forEach(t => fetchTable(t));
  }, [tableList, expanded, fetchTable]);

  const refreshAll = useCallback(async () => {
    setRefreshingAll(true);
    const expandedTables = tableList.filter(t => expanded[t.name] && t.endpoint);
    await Promise.all(expandedTables.map(t => fetchTable(t)));
    setRefreshingAll(false);
  }, [tableList, expanded, fetchTable]);

  useEffect(() => {
    if (timerRef.current) clearInterval(timerRef.current);
    if (!paused) {
      timerRef.current = setInterval(refreshExpanded, 30000);
    }
    return () => { if (timerRef.current) clearInterval(timerRef.current); };
  }, [paused, refreshExpanded]);

  useEffect(() => {
    if (initialDbId) {
      const db = DB_TABLES[initialDbId];
      if (db) {
        setExpanded(prev => {
          const next = { ...prev };
          db.tables.forEach(t => { if (t.endpoint) next[t.name] = true; });
          return next;
        });
      }
    }
  }, [initialDbId]);

  const toggleExpand = (name) => {
    setExpanded(prev => {
      const next = { ...prev, [name]: !prev[name] };
      return next;
    });
  };

  const expandedCount = tableList.filter(t => expanded[t.name]).length;
  const errorCount = tableList.filter(t => errors[t.name]).length;

  return (
    <div className="dv-root">
      <div className="dv-controls">
        <div className="dv-controls-left">
          <span className="dv-title">Datos en Vivo — {tableList.length} tablas</span>
          <span className="dv-subtitle">
            {expandedCount > 0
              ? `${expandedCount} expandidas · ${paused ? 'Auto-refresh pausado' : 'Auto-refresh 30s'}`
              : 'Click en una tabla para cargar sus datos'}
            {errorCount > 0 && <span style={{color:'#ef4444',marginLeft:8}}>{errorCount} errores</span>}
          </span>
        </div>
        <div className="dv-controls-right">
          <div className="dv-poll-selector">
            <button
              className={'dv-poll-btn' + (paused ? ' active paused' : '')}
              onClick={() => setPaused(true)}
            >
              Pausado
            </button>
            <button
              className={'dv-poll-btn' + (!paused ? ' active' : '')}
              onClick={() => setPaused(false)}
            >
              30s
            </button>
          </div>
          <button className="dv-refresh-btn" onClick={refreshAll} disabled={refreshingAll || expandedCount === 0}>
            <HiOutlineRefresh style={{fontSize:14}} />
            {refreshingAll ? 'Cargando...' : 'Actualizar'}
          </button>
        </div>
      </div>

      <div className="dv-cards">
        {tableList.map(table => (
          <TableCard
            key={table.name}
            table={table}
            expanded={expanded}
            onToggle={toggleExpand}
            data={tableData[table.name]}
            loading={loading[table.name]}
            error={errors[table.name]}
            lastUpdate={lastUpdate[table.name]}
            onFetch={fetchTable}
          />
        ))}
      </div>
    </div>
  );
}
