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
      <div className="login-hours">
        <div className="login-hours-left">
          <div className="login-hours-inner">
            <div className="login-hours-card">
              <span className="login-hours-icon">&#x1F555;</span>
              <div className="login-hours-title">Sistema fuera de horario</div>
              <p className="login-hours-text">SISEXP-UPLA opera en horario laboral:</p>
              <div className="login-hours-time">8:00 AM &mdash; 8:00 PM</div>
              <div className="login-hours-zone">Horario de Per&uacute; (UTC-5)</div>
              <button className="login-hours-btn" onClick={() => setForzarAcceso(true)}>
                Ingresar de todos modos
              </button>
            </div>
          </div>
        </div>
        <div className="login-hours-right">
          <div className="login-right-slide login-right-slide--1" />
          <div className="login-right-slide login-right-slide--2" />
          <div className="login-right-overlay" />
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

  const inputError = (field) => error && !field ? ' login-input--error' : '';

  return (
    <div className="login-container">
      <div className="login-left">
        <div className="login-left-inner">
          <img
            className="login-left-logo"
            src={require('../../assets/login/logo/upla_logo_animado.gif')}
            alt="Universidad Peruana Los Andes"
          />

          <div className="login-left-brand">SISEXP</div>
          <div className="login-left-tagline">
            Sistema de Gesti&oacute;n de Expedientes
          </div>

          <div className="login-form-section">
            <h1 className="login-title">Iniciar sesi&oacute;n</h1>
            <p className="login-subtitle">
              Ingrese sus credenciales para acceder al sistema
            </p>

            {error && (
              <div className="login-error">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
                {error}
              </div>
            )}

            <form className="login-form" onSubmit={handleSubmit}>
              <div className="login-input-group">
                <label className="login-label" htmlFor="login-email">Correo electr&oacute;nico</label>
                <input
                  id="login-email"
                  className={'login-input' + inputError(email)}
                  type="email"
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  placeholder="correo@upla.edu.pe"
                  autoFocus
                  autoComplete="email"
                />
              </div>

              <div className="login-input-group">
                <label className="login-label" htmlFor="login-password">Contrase&ntilde;a</label>
                <input
                  id="login-password"
                  className={'login-input' + inputError(password)}
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;"
                  autoComplete="current-password"
                />
              </div>

              <button type="submit" disabled={loading} className="login-btn">
                {loading ? 'Ingresando\u2026' : 'Ingresar al sistema'}
              </button>
            </form>

            <div className="login-seeds">
              <div className="login-seeds-label">Acceso r&aacute;pido demo</div>
              <div className="login-seeds-grid">
                {seeds.map(s => (
                  <button key={s.email} type="button" className="login-seed-btn"
                    onClick={() => { setEmail(s.email); setPassword(s.pass); }}
                  >
                    <span className="login-seed-btn-rol">{s.rol}</span>
                    <span className="login-seed-btn-email">{s.email}</span>
                  </button>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="login-right">
        <div className="login-right-slide login-right-slide--1" />
        <div className="login-right-slide login-right-slide--2" />
        <div className="login-right-overlay" />
      </div>
    </div>
  );
}
