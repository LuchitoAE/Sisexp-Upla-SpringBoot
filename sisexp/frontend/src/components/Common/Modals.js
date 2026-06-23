import React, { useState } from 'react';

const modalStyles = {
  overlay: {
    position: 'fixed', inset: 0, background: 'rgba(15,23,42,0.5)', backdropFilter: 'blur(4px)',
    display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 9999,
    animation: 'fadeIn 0.15s ease-out'
  },
  card: {
    background: '#fff', borderRadius: 18, padding: '28px 32px 22px', width: '100%', maxWidth: 440,
    boxShadow: '0 25px 50px rgba(0,0,0,0.15)', position: 'relative'
  },
  title: { fontSize: 16, fontWeight: 700, color: '#0f172a', marginBottom: 8 },
  message: { fontSize: 13, color: '#475569', lineHeight: 1.5, marginBottom: 20 },
  input: {
    width: '100%', padding: '10px 14px', borderRadius: 10, border: '1.5px solid #e2e8f0',
    fontSize: 13, fontFamily: 'Inter, sans-serif', outline: 'none', marginBottom: 16, color: '#1e293b',
    transition: 'border 0.15s'
  },
  inputFocus: { borderColor: '#3b82f6', boxShadow: '0 0 0 3px rgba(59,130,246,0.10)' },
  actions: { display: 'flex', gap: 8, justifyContent: 'flex-end' }
};

export function Modal({ open, title, message, children, onClose, showClose }) {
  if (!open) return null;
  return (
    <div style={modalStyles.overlay} onClick={onClose}>
      <div style={modalStyles.card} onClick={e => e.stopPropagation()}>
        {showClose && (
          <button onClick={onClose} style={{
            position: 'absolute', top: 14, right: 18, background: 'none', border: 'none',
            fontSize: 18, color: '#94a3b8', cursor: 'pointer', padding: 0, lineHeight: 1
          }}>×</button>
        )}
        {title && <div style={modalStyles.title}>{title}</div>}
        {message && <div style={modalStyles.message}>{message}</div>}
        {children}
      </div>
    </div>
  );
}

// Hook: useConfirm — reemplaza window.confirm con modal estilizado
export function useConfirm() {
  const [state, setState] = useState({ open: false, title: '', message: '', resolve: null, input: false, inputLabel: '' });
  const [inputValue, setInputValue] = useState('');

  const confirm = (title, message) => new Promise(resolve => {
    setState({ open: true, title, message, resolve, input: false, inputLabel: '' });
  });

  const promptText = (title, message, label) => new Promise(resolve => {
    setState({ open: true, title, message, resolve, input: true, inputLabel: label || '' });
    setInputValue('');
  });

  const handleOk = () => {
    const val = state.input ? inputValue : true;
    state.resolve?.(val);
    setState({ ...state, open: false });
  };

  const handleCancel = () => {
    state.resolve?.(state.input ? null : false);
    setState({ ...state, open: false });
  };

  const ConfirmModal = (
    <Modal open={state.open} onClose={handleCancel}>
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, marginBottom: 12 }}>
        <span style={{ fontSize: 28, flexShrink: 0 }}>⚠</span>
        <div>
          <div style={modalStyles.title}>{state.title}</div>
          <div style={modalStyles.message}>{state.message}</div>
        </div>
      </div>
      {state.input && (
        <div>
          {state.inputLabel && <div style={{ fontSize: 12, fontWeight: 600, color: '#475569', marginBottom: 4 }}>{state.inputLabel}</div>}
          <input
            style={modalStyles.input}
            value={inputValue}
            onChange={e => setInputValue(e.target.value)}
            onFocus={e => { e.target.style.borderColor = '#3b82f6'; e.target.style.boxShadow = '0 0 0 3px rgba(59,130,246,0.10)'; }}
            onBlur={e => { e.target.style.borderColor = '#e2e8f0'; e.target.style.boxShadow = 'none'; }}
            placeholder="Escriba aquí..."
            autoFocus
          />
        </div>
      )}
      <div style={modalStyles.actions}>
        <button className="btn btn-secondary btn-sm" onClick={handleCancel}>Cancelar</button>
        <button className="btn btn-primary btn-sm" onClick={handleOk} disabled={state.input && !inputValue}>
          {state.input ? 'Confirmar' : 'Aceptar'}
        </button>
      </div>
    </Modal>
  );

  return { confirm, promptText, ConfirmModal };
}

// Hook: useAlert — notificación simple
export function useAlert() {
  const [state, setState] = useState({ open: false, title: '', message: '', color: '#16a34a' });

  const alert = (title, message, color = '#16a34a') => {
    setState({ open: true, title, message, color });
  };

  const AlertModal = (
    <Modal open={state.open} onClose={() => setState({ ...state, open: false })}>
      <div style={{ textAlign: 'center' }}>
        <div style={{ fontSize: 36, marginBottom: 8 }}>✓</div>
        <div style={{ fontSize: 16, fontWeight: 700, color: state.color, marginBottom: 4 }}>{state.title}</div>
        <div style={{ fontSize: 13, color: '#64748b', lineHeight: 1.5, marginBottom: 16 }}>{state.message}</div>
        <button className="btn btn-primary btn-sm" onClick={() => setState({ ...state, open: false })}>Entendido</button>
      </div>
    </Modal>
  );

  return { alert, AlertModal };
}
