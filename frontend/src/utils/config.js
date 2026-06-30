export const ROL_LABEL = {
  'Administrador': 'Administrador',
  'Coordinacion': 'Coordinación',
  'Secretaria': 'Secretaría',
  'Director': 'Director de Escuela',
  'Laboratorio': 'Resp. Laboratorio',
  'Decanato': 'Decanato'
};

export const ROL_COLOR = {
  'Administrador': '#dc2626',
  'Coordinacion': '#2563eb',
  'Secretaria': '#7c3aed',
  'Director': '#0891b2',
  'Laboratorio': '#d97706',
  'Decanato': '#64748b'
};

export const ROL_PROFILE = {
  'Administrador': { id: 'admin_planificacion', label: 'Administración / Planificación', color: '#dc2626' },
  'Coordinacion': { id: 'admin_planificacion', label: 'Administración / Planificación', color: '#2563eb' },
  'Secretaria': { id: 'secretarial', label: 'Apoyo Secretarial', color: '#7c3aed' },
  'Director': { id: 'solicitante', label: 'Usuario Solicitante', color: '#0891b2' },
  'Laboratorio': { id: 'solicitante', label: 'Usuario Solicitante', color: '#d97706' },
  'Decanato': { id: 'consulta', label: 'Consulta / Decanato', color: '#64748b' }
};

// Action-level permissions for frontend UI decisions
export const PUEDE = {
  // Expedientes
  crearExpediente: ['Administrador', 'Coordinacion', 'Laboratorio', 'Director', 'Secretaria'],
  aprobarObservar: ['Administrador', 'Coordinacion'],
  rechazar: ['Administrador', 'Coordinacion'],
  finalizar: ['Administrador', 'Coordinacion', 'Secretaria'],
  derivar: ['Administrador', 'Coordinacion', 'Secretaria'],
  cambiarEstado: ['Administrador', 'Coordinacion'],
  verDerivacion: ['Administrador', 'Coordinacion', 'Secretaria'],
  subirDocumento: ['Administrador', 'Coordinacion', 'Secretaria', 'Laboratorio', 'Director'],
  eliminarDocumento: ['Administrador'],
  verTodosExpedientes: ['Administrador', 'Coordinacion', 'Secretaria'],
  verReportes: ['Administrador', 'Coordinacion', 'Decanato', 'Director'],
};

export function puede(rol, accion) {
  const roles = PUEDE[accion];
  if (!roles) return false;
  return roles.includes(rol);
}

export const NAV_MODULES = [
  { id: 'dashboard', label: 'Panel principal' },
  { id: 'expedientes', label: 'Expedientes' },
  { id: 'techos', label: 'Techo Presupuestal' },
  { id: 'poi', label: 'Actividades POI' },
  { id: 'pap', label: 'PAP' },
  { id: 'reportes', label: 'Reportes' },
  { id: 'notas', label: 'Notas Modif.' },
  { id: 'usuarios', label: 'Usuarios' }
];

export const NAV_PERMISSIONS = {
  'Administrador': ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'reportes', 'notas', 'usuarios'],
  'Coordinacion': ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'reportes', 'notas'],
  'Secretaria': ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'notas'],
  'Director': ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'reportes', 'notas'],
  'Laboratorio': ['dashboard', 'expedientes', 'poi', 'pap', 'notas'],
  'Decanato': ['dashboard', 'pap', 'reportes', 'notas']
};
