import React, { useEffect, useState, useCallback } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { useModals } from '../App';
import { client } from '../api/client';

const ROLES = ['Administrador', 'Coordinacion', 'Secretaria', 'Director', 'Laboratorio', 'Decanato'];
const ROL_COLORS = {
  'Administrador': '#dc2626', 'Coordinacion': '#2563eb', 'Secretaria': '#7c3aed',
  'Director': '#0891b2', 'Laboratorio': '#d97706', 'Decanato': '#64748b'
};

export default function UsuariosPage() {
  const { token, user } = useAuth();
  const modals = useModals();
  const [usuarios, setUsuarios] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState(null);
  const [form, setForm] = useState({ nombre: '', email: '', password: '', rol: 'Laboratorio', activo: true, horarioRestringido: true });
  const [submitting, setSubmitting] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try { setUsuarios(await client.get('/usuarios', token)); }
    catch (err) { modals.alerta('Error', err.message); }
    finally { setLoading(false); }
  }, [token]);

  useEffect(() => { load(); }, [load]);

  const resetForm = () => setForm({ nombre: '', email: '', password: '', rol: 'Laboratorio', activo: true, horarioRestringido: true });

  const handleSubmit = async () => {
    if (!form.nombre || !form.email || (!editId && !form.password)) return;
    setSubmitting(true);
    try {
      if (editId) {
        const payload = { nombre: form.nombre, email: form.email, rol: form.rol, activo: form.activo, horarioRestringido: form.horarioRestringido };
        if (form.password) payload.password = form.password;
        await client.put(`/usuarios/${editId}`, payload, token);
      } else {
        await client.post('/usuarios', form, token);
      }
      setShowForm(false); setEditId(null); resetForm(); load();
    } catch (err) { modals.alerta('Error', err.message); }
    finally { setSubmitting(false); }
  };

  const toggleActivo = async (u) => {
    const ok = await modals.confirm(
      u.activo ? 'Desactivar usuario' : 'Activar usuario',
      `${u.activo ? '¿Desactivar' : '¿Activar'} a ${u.nombre}? ${u.activo ? 'No podrá iniciar sesión.' : 'Podrá volver a acceder al sistema.'}`
    );
    if (!ok) return;
    try { await client.patch(`/usuarios/${u.id}/toggle-activo`, {}, token); load(); }
    catch (err) { modals.alerta('Error', err.message); }
  };

  const toggleHorario = async (u) => {
    const ok = await modals.confirm(
      u.horarioRestringido ? 'Quitar restricción horaria' : 'Restringir horario laboral',
      `${u.horarioRestringido ? '¿Quitar la restricción' : '¿Restringir el acceso'} de ${u.nombre} a horario laboral (L-D 8am-8pm)?`
    );
    if (!ok) return;
    try {
      await client.put(`/usuarios/${u.id}`, { horarioRestringido: !u.horarioRestringido }, token);
      load();
    } catch (err) { modals.alerta('Error', err.message); }
  };

  const openEdit = (u) => {
    setEditId(u.id);
    setForm({ nombre: u.nombre, email: u.email, password: '', rol: u.rol, activo: u.activo, horarioRestringido: u.horarioRestringido });
    setShowForm(true);
  };

  return (
    <div style={{ padding: 28, maxWidth: 1100, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', letterSpacing: -0.4 }}>
            Gestión de Usuarios
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4 }}>
            Administración de cuentas, roles, activación y control de acceso
          </div>
        </div>
        <button className="btn btn-primary" onClick={() => { setShowForm(true); setEditId(null); resetForm(); }}>
          + Nuevo usuario
        </button>
      </div>

      {/* Form */}
      {showForm && (
        <div className="card animate-in" style={{ padding: 24, marginBottom: 24 }}>
          <div style={{ fontSize: 15, fontWeight: 700, color: '#0f172a', marginBottom: 18 }}>
            {editId ? 'Editar usuario' : 'Nuevo usuario'}
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 14 }}>
            <div>
              <label className="label">Nombre completo *</label>
              <input className="input" value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} placeholder="Juan Pérez" />
            </div>
            <div>
              <label className="label">Correo electrónico *</label>
              <input className="input" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })} placeholder="correo@upla.edu.pe" />
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: 14, marginBottom: 16 }}>
            <div>
              <label className="label">Contraseña {editId ? '(dejar vacío = no cambiar)' : '*'}</label>
              <input className="input" type="password" value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} placeholder={editId ? '••••••••' : 'Mínimo 6 caracteres'} />
            </div>
            <div>
              <label className="label">Rol *</label>
              <select className="input" value={form.rol} onChange={e => setForm({ ...form, rol: e.target.value })} disabled={editId === user?.id}>
                {ROLES.map(r => <option key={r} value={r}>{r}</option>)}
              </select>
            </div>
            <div>
              <label className="label">Acceso</label>
              <div style={{ display: 'flex', gap: 10, paddingTop: 6 }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: editId === user?.id ? 'not-allowed' : 'pointer', fontSize: 13, opacity: editId === user?.id ? 0.5 : 1 }}>
                  <input type="checkbox" checked={form.activo} onChange={e => setForm({ ...form, activo: e.target.checked })} disabled={editId === user?.id} />
                  Activo
                </label>
                <label style={{ display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer', fontSize: 13 }}>
                  <input type="checkbox" checked={form.horarioRestringido} onChange={e => setForm({ ...form, horarioRestringido: e.target.checked })} />
                  Restringir a horario laboral
                </label>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={() => { setShowForm(false); setEditId(null); }}>Cancelar</button>
            <button className="btn btn-primary" onClick={handleSubmit} disabled={submitting}>
              {submitting ? 'Guardando...' : editId ? 'Actualizar' : 'Crear usuario'}
            </button>
          </div>
        </div>
      )}

      {/* Info banner */}
      <div style={{ fontSize: 11, color: '#94a3b8', marginBottom: 14, background: '#f8fafc', padding: '8px 14px', borderRadius: 8, display: 'flex', gap: 20 }}>
        <span>🕐 <strong>Horario laboral:</strong> Lunes a Domingo, 8:00 AM – 8:00 PM</span>
        <span>🔒 <strong>Usuarios restringidos</strong> solo acceden en ese horario</span>
        <span>🚫 <strong>Inactivos</strong> no pueden iniciar sesión</span>
      </div>

      {/* Table */}
      {loading && <div style={{ color: '#94a3b8' }}>Cargando...</div>}

      {!loading && (
        <div className="card" style={{ overflow: 'hidden' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 12 }}>
            <thead>
              <tr style={{ background: '#f8fafc', borderBottom: '2px solid #e2e8f0' }}>
                <th style={{ padding: '10px 14px', textAlign: 'left', fontWeight: 600, color: '#475569', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.04 }}>Usuario</th>
                <th style={{ padding: '10px 14px', textAlign: 'left', fontWeight: 600, color: '#475569', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.04 }}>Email</th>
                <th style={{ padding: '10px 14px', textAlign: 'left', fontWeight: 600, color: '#475569', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.04 }}>Rol</th>
                <th style={{ padding: '10px 14px', textAlign: 'center', fontWeight: 600, color: '#475569', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.04 }}>Estado</th>
                <th style={{ padding: '10px 14px', textAlign: 'center', fontWeight: 600, color: '#475569', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.04 }}>Horario</th>
                <th style={{ padding: '10px 14px', textAlign: 'center', fontWeight: 600, color: '#475569', fontSize: 11, textTransform: 'uppercase', letterSpacing: 0.04 }}>Acciones</th>
              </tr>
            </thead>
            <tbody>
              {usuarios.map(u => (
                <tr key={u.id} style={{ borderBottom: '1px solid #f1f5f9' }}>
                  <td style={{ padding: '10px 14px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                      <div style={{
                        width: 28, height: 28, borderRadius: '50%',
                        background: ROL_COLORS[u.rol] || '#2563eb',
                        display: 'flex', alignItems: 'center', justifyContent: 'center',
                        fontSize: 10, fontWeight: 800, color: '#fff', flexShrink: 0
                      }}>{(u.nombre || '?').split(' ').map(s => s[0]).slice(0, 2).join('')}</div>
                      <span style={{ fontWeight: 600 }}>{u.nombre}</span>
                      {u.id === user?.id && <span style={{ fontSize: 9, color: '#3b82f6', background: '#eff6ff', padding: '1px 6px', borderRadius: 8, fontWeight: 600 }}>tú</span>}
                    </div>
                  </td>
                  <td style={{ padding: '10px 14px', color: '#64748b', fontFamily: 'monospace', fontSize: 11 }}>{u.email}</td>
                  <td style={{ padding: '10px 14px' }}>
                    <span style={{
                      fontSize: 10, padding: '2px 8px', borderRadius: 20, fontWeight: 600,
                      background: ROL_COLORS[u.rol] ? `${ROL_COLORS[u.rol]}18` : '#f1f5f9',
                      color: ROL_COLORS[u.rol] || '#475569'
                    }}>{u.rol}</span>
                  </td>
                  <td style={{ padding: '10px 14px', textAlign: 'center' }}>
                    {u.id === user?.id ? (
                      <span style={{ fontSize: 10, color: '#94a3b8' }}>—</span>
                    ) : (
                      <button onClick={() => toggleActivo(u)}
                        style={{
                          padding: '3px 12px', borderRadius: 20, border: 'none', cursor: 'pointer',
                          fontSize: 11, fontWeight: 600,
                          background: u.activo ? '#dcfce7' : '#fee2e2',
                          color: u.activo ? '#166534' : '#b91c1c'
                        }}
                        title="Clic para activar/desactivar">
                        {u.activo ? 'Activo' : 'Inactivo'}
                      </button>
                    )}
                  </td>
                  <td style={{ padding: '10px 14px', textAlign: 'center' }}>
                    <button onClick={() => toggleHorario(u)}
                      style={{
                        padding: '3px 12px', borderRadius: 20, border: 'none', cursor: 'pointer',
                        fontSize: 11, fontWeight: 600,
                        background: u.horarioRestringido ? '#fef3c7' : '#f1f5f9',
                        color: u.horarioRestringido ? '#92400e' : '#64748b'
                      }}
                      title="Clic para restringir/liberar horario">
                      {u.horarioRestringido ? 'Restringido' : 'Sin límite'}
                    </button>
                  </td>
                  <td style={{ padding: '10px 14px', textAlign: 'center' }}>
                    <div style={{ display: 'flex', gap: 4, justifyContent: 'center' }}>
                      <button className="btn btn-secondary btn-xs" onClick={() => openEdit(u)}>Editar</button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {usuarios.length === 0 && (
            <div style={{ padding: 40, textAlign: 'center', color: '#94a3b8' }}>No hay usuarios registrados</div>
          )}
        </div>
      )}
    </div>
  );
}
