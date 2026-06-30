import React, { useState } from 'react';

function enHorarioLaboral() {
  try {
    const d = new Date();
    const f = new Intl.DateTimeFormat('en-US', { timeZone: 'America/Lima', hour: '2-digit', minute: '2-digit', hour12: false });
    const [h, m] = f.format(d).split(':').map(Number);
    return h * 60 + m >= 480 && h * 60 + m < 1200;
  } catch { return true; }
}

export default function Login({ onLogin }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [forzarAcceso, setForzarAcceso] = useState(true);

  const horarioOk = enHorarioLaboral();

  if (!horarioOk && !forzarAcceso) {
    return (
      <div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'linear-gradient(135deg, #0f172a 0%, #1e3a5f 40%, #1e293b 100%)', padding: 20 }}>
        <div style={{ background: 'rgba(255,255,255,0.98)', borderRadius: 24, padding: '40px', width: '100%', maxWidth: 420, boxShadow: '0 25px 60px rgba(0,0,0,0.25)', textAlign: 'center' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>&#x1F555;</div>
          <div style={{ fontSize: 20, fontWeight: 800, color: '#0f172a', marginBottom: 8 }}>Sistema fuera de horario</div>
          <div style={{ fontSize: 14, color: '#64748b', marginBottom: 4 }}>SISEXP-UPLA opera en horario laboral:</div>
          <div style={{ fontSize: 28, fontWeight: 800, color: '#2563eb', margin: '12px 0' }}>8:00 AM — 8:00 PM</div>
          <div style={{ fontSize: 12, color: '#94a3b8', marginBottom: 20 }}>Horario de Perú (UTC-5)</div>
          <button onClick={() => setForzarAcceso(true)} style={{ background: 'transparent', border: '1px solid #e2e8f0', borderRadius: 10, padding: '8px 20px', color: '#64748b', cursor: 'pointer', fontSize: 12, fontFamily: 'Inter, sans-serif' }}>
            Ingresar de todos modos
          </button>
        </div>
      </div>
    );
  }

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (!email || !password) { setError('Complete todos los campos'); return; }
    setLoading(true);
    try { await onLogin(email, password); }
    catch (err) { setError(err.message); }
    finally { setLoading(false); }
  };

  const seeds = [
    { email: 'jefe@upla.edu.pe', pass: 'jefe123', rol: 'Admin' },
    { email: 'coord@upla.edu.pe', pass: 'coord123', rol: 'Coord' },
    { email: 'lab@upla.edu.pe', pass: 'lab123', rol: 'Lab' },
    { email: 'decanato@upla.edu.pe', pass: 'decanato123', rol: 'Dec' },
  ];

  return (
    <div style={{
      minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      background: 'linear-gradient(135deg, #0f172a 0%, #1e3a5f 40%, #1e293b 100%)',
      padding: 20
    }}>
      {/* Background decorative elements */}
      <div style={{ position: 'absolute', top: '10%', left: '8%', width: 300, height: 300, borderRadius: '50%', background: 'radial-gradient(circle, rgba(59,130,246,0.15) 0%, transparent 70%)', pointerEvents: 'none' }} />
      <div style={{ position: 'absolute', bottom: '5%', right: '10%', width: 400, height: 400, borderRadius: '50%', background: 'radial-gradient(circle, rgba(16,185,129,0.10) 0%, transparent 70%)', pointerEvents: 'none' }} />

      <div style={{
        background: 'rgba(255,255,255,0.98)', backdropFilter: 'blur(20px)',
        borderRadius: 24, padding: '44px 40px 36px', width: '100%', maxWidth: 420,
        boxShadow: '0 25px 60px rgba(0,0,0,0.25), 0 8px 20px rgba(0,0,0,0.15)',
        position: 'relative', zIndex: 1
      }}>
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <div style={{
            width: 56, height: 56, borderRadius: 16,
            background: 'linear-gradient(135deg, #3b82f6, #2563eb)',
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 20, fontWeight: 900, color: '#fff',
            boxShadow: '0 8px 24px rgba(37,99,235,0.30)',
            marginBottom: 16
          }}>S</div>
          <div style={{ fontSize: 24, fontWeight: 800, color: '#0f172a', letterSpacing: -0.5 }}>
            SISEXP-UPLA
          </div>
          <div style={{ fontSize: 13, color: '#64748b', marginTop: 4, fontWeight: 500 }}>
            Sistema de Gestión de Expedientes
          </div>
        </div>

        {error && (
          <div style={{
            background: '#fef2f2', color: '#991b1b', padding: '10px 14px', borderRadius: 10,
            fontSize: 13, marginBottom: 16, border: '1px solid #fecaca', fontWeight: 500,
            display: 'flex', alignItems: 'center', gap: 8
          }}>
            <span style={{ fontSize: 16 }}>⚠</span> {error}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div style={{ marginBottom: 16 }}>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 6, letterSpacing: 0.01 }}>
              Correo electrónico
            </label>
            <input
              style={{
                width: '100%', padding: '12px 16px', borderRadius: 12, border: '1.5px solid #e2e8f0',
                fontSize: 14, fontFamily: 'Inter, sans-serif', outline: 'none',
                transition: 'border 0.15s, box-shadow 0.15s', background: '#f8fafc'
              }}
              type="email" value={email}
              onChange={e => setEmail(e.target.value)}
              placeholder="correo@upla.edu.pe" autoFocus
              onFocus={e => { e.target.style.borderColor = '#3b82f6'; e.target.style.boxShadow = '0 0 0 3px rgba(59,130,246,0.10)'; e.target.style.background = '#fff'; }}
              onBlur={e => { e.target.style.borderColor = '#e2e8f0'; e.target.style.boxShadow = 'none'; e.target.style.background = '#f8fafc'; }}
            />
          </div>

          <div style={{ marginBottom: 20 }}>
            <label style={{ display: 'block', fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 6, letterSpacing: 0.01 }}>
              Contraseña
            </label>
            <input
              style={{
                width: '100%', padding: '12px 16px', borderRadius: 12, border: '1.5px solid #e2e8f0',
                fontSize: 14, fontFamily: 'Inter, sans-serif', outline: 'none',
                transition: 'border 0.15s, box-shadow 0.15s', background: '#f8fafc'
              }}
              type="password" value={password}
              onChange={e => setPassword(e.target.value)}
              placeholder="••••••••"
              onFocus={e => { e.target.style.borderColor = '#3b82f6'; e.target.style.boxShadow = '0 0 0 3px rgba(59,130,246,0.10)'; e.target.style.background = '#fff'; }}
              onBlur={e => { e.target.style.borderColor = '#e2e8f0'; e.target.style.boxShadow = 'none'; e.target.style.background = '#f8fafc'; }}
            />
          </div>

          <button
            type="submit" disabled={loading}
            style={{
              width: '100%', padding: '12px 0', borderRadius: 12, border: 'none',
              background: loading
                ? '#93c5fd'
                : 'linear-gradient(135deg, #3b82f6, #2563eb)',
              color: '#fff', fontSize: 14, fontWeight: 700, cursor: loading ? 'default' : 'pointer',
              fontFamily: 'Inter, sans-serif',
              boxShadow: loading ? 'none' : '0 4px 14px rgba(37,99,235,0.30)',
              transition: 'all 0.2s',
              letterSpacing: 0.02
            }}
          >
            {loading ? 'Ingresando…' : 'Ingresar al sistema'}
          </button>
        </form>

        {/* Quick access seeds */}
        <div style={{ marginTop: 28, paddingTop: 20, borderTop: '1px solid #f1f5f9' }}>
          <div style={{ fontSize: 10, fontWeight: 600, color: '#94a3b8', marginBottom: 10, textAlign: 'center', letterSpacing: 0.04, textTransform: 'uppercase' }}>
            Acceso rápido demo
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 6 }}>
            {seeds.map(s => (
              <button key={s.email} type="button"
                onClick={() => { setEmail(s.email); setPassword(s.pass); }}
                style={{
                  padding: '7px 10px', borderRadius: 8, border: '1px solid #e2e8f0', background: '#fff',
                  cursor: 'pointer', fontSize: 10, fontFamily: 'Inter, sans-serif', fontWeight: 500,
                  color: '#475569', transition: 'all 0.12s', textAlign: 'left'
                }}
                onMouseEnter={e => { e.currentTarget.style.background = '#f8fafc'; e.currentTarget.style.borderColor = '#3b82f6'; }}
                onMouseLeave={e => { e.currentTarget.style.background = '#fff'; e.currentTarget.style.borderColor = '#e2e8f0'; }}
              >
                <span style={{ fontWeight: 700, color: '#2563eb' }}>{s.rol}</span>{' '}
                <span style={{ color: '#94a3b8', fontSize: 9 }}>{s.email}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
