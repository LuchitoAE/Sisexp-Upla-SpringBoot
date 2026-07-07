import type { RolUsuario } from '../types';

export const ROL_LABEL: Record<string, string> = {
  Administrador: 'Administrador',
  Coordinacion: 'Coordinación',
  Secretaria: 'Secretaría',
  Director: 'Director de Escuela',
  Laboratorio: 'Resp. Laboratorio',
  Decanato: 'Decanato',
};

export const ROL_COLOR: Record<string, string> = {
  Administrador: '#dc2626',
  Coordinacion: '#0ea5e9',
  Secretaria: '#7c3aed',
  Director: '#0891b2',
  Laboratorio: '#d97706',
  Decanato: '#64748b',
};

export const ROL_PROFILE: Record<string, { id: string; label: string; color: string }> = {
  Administrador: { id: 'admin_planificacion', label: 'Administración / Planificación', color: '#dc2626' },
  Coordinacion: { id: 'admin_planificacion', label: 'Administración / Planificación', color: '#0ea5e9' },
  Secretaria: { id: 'secretarial', label: 'Apoyo Secretarial', color: '#7c3aed' },
  Director: { id: 'solicitante', label: 'Usuario Solicitante', color: '#0891b2' },
  Laboratorio: { id: 'solicitante', label: 'Usuario Solicitante', color: '#d97706' },
  Decanato: { id: 'consulta', label: 'Consulta / Decanato', color: '#64748b' },
};

export const PUEDE: Record<string, RolUsuario[]> = {
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

export function puede(rol: RolUsuario | undefined, accion: string): boolean {
  if (!rol) return false;
  const roles = PUEDE[accion];
  if (!roles) return false;
  return roles.includes(rol);
}

export function esGestionPresupuestal(rol: RolUsuario | undefined): boolean {
  return rol === 'Administrador' || rol === 'Coordinacion';
}

export const NAV_PERMISSIONS: Record<string, string[]> = {
  Administrador: ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'reportes', 'notas', 'usuarios'],
  Coordinacion: ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'reportes', 'notas'],
  Secretaria: ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'notas'],
  Director: ['dashboard', 'expedientes', 'techos', 'poi', 'pap', 'reportes', 'notas'],
  Laboratorio: ['dashboard', 'expedientes', 'poi', 'pap', 'notas'],
  Decanato: ['dashboard', 'pap', 'reportes', 'notas'],
};

export const NAV_MODULES = [
  { id: 'dashboard', label: 'Panel principal', icon: 'LayoutDashboard' },
  { id: 'expedientes', label: 'Expedientes', icon: 'FileText' },
  { id: 'techos', label: 'Techo Presupuestal', icon: 'Wallet' },
  { id: 'poi', label: 'Actividades POI', icon: 'ClipboardList' },
  { id: 'pap', label: 'PAP', icon: 'Package' },
  { id: 'reportes', label: 'Reportes', icon: 'BarChart3' },
  { id: 'notas', label: 'Notas Modif.', icon: 'FileEdit' },
  { id: 'usuarios', label: 'Usuarios', icon: 'Users' },
] as const;
