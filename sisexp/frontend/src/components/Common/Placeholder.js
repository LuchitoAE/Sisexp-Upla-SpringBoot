import React from 'react';

export default function Placeholder({ title, description }) {
  return (
    <div style={{ padding: 32 }}>
      <div style={{ fontSize: 22, fontWeight: 800, color: '#0f172a', marginBottom: 4 }}>
        {title || 'Módulo'}
      </div>
      <div style={{ fontSize: 13, color: '#64748b' }}>
        {description || 'En construcción'}
      </div>
    </div>
  );
}
