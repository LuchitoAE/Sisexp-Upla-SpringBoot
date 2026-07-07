import { client } from './api';
import type {
  Usuario,
  TechoPresupuestal,
  ActividadPOI,
  NecesidadPAP,
  Expediente,
  Notificacion,
  NotaModificatoria,
  DashboardAlertas,
  DashboardSaldos,
  Disponibilidad,
} from '../types';

export const authApi = {
  login: (email: string, password: string) =>
    client.post<{ usuario: Usuario }>('/auth/login', { email, password }),
  me: () => client.get<Usuario>('/auth/me'),
  logout: () => client.post<void>('/auth/logout'),
};

export const usuarioApi = {
  list: () => client.get<Usuario[]>('/usuarios'),
  create: (data: Partial<Usuario>) => client.post<Usuario>('/usuarios', data),
  update: (id: number, data: Partial<Usuario>) => client.put<Usuario>(`/usuarios/${id}`, data),
  toggleActivo: (id: number) => client.patch<void>(`/usuarios/${id}/toggle-activo`),
};

export const techoApi = {
  list: () => client.get<TechoPresupuestal[]>('/techos-presupuestales'),
  create: (data: { año: number; montoTotal: string }) =>
    client.post<TechoPresupuestal>('/techos-presupuestales', data),
  update: (id: number, data: { montoTotal: string }) =>
    client.put<TechoPresupuestal>(`/techos-presupuestales/${id}`, data),
  toggleActivo: (id: number) =>
    client.patch<void>(`/techos-presupuestales/${id}/toggle-activo`),
  finalizarPOI: (id: number) =>
    client.post<void>(`/techos-presupuestales/${id}/finalizar-poi`),
  desbloquearPOI: (id: number) =>
    client.post<void>(`/techos-presupuestales/${id}/desbloquear-poi`),
};

export const actividadApi = {
  listByTecho: (techoId: number) =>
    client.get<ActividadPOI[]>(`/actividades-poi/techo/${techoId}`),
  create: (techoId: number, data: { nombre: string; presupuestoAsignado: string; fechaLimite?: string }) =>
    client.post<ActividadPOI>(`/actividades-poi/techo/${techoId}`, data),
  update: (id: number, data: Partial<ActividadPOI>) =>
    client.put<ActividadPOI>(`/actividades-poi/${id}`, data),
  delete: (id: number) => client.del<void>(`/actividades-poi/${id}`),
  finalizarPAP: (id: number) =>
    client.post<void>(`/actividades-poi/${id}/finalizar-pap`),
  desbloquearPAP: (id: number) =>
    client.post<void>(`/actividades-poi/${id}/desbloquear-pap`),
};

export const necesidadApi = {
  list: () => client.get<NecesidadPAP[]>('/necesidades-pap'),
  listByActividad: (actividadId: number) =>
    client.get<NecesidadPAP[]>(`/necesidades-pap/actividad/${actividadId}`),
  create: (actividadId: number, data: Partial<NecesidadPAP>) =>
    client.post<NecesidadPAP>(`/necesidades-pap/actividad/${actividadId}`, data),
  update: (id: number, data: Partial<NecesidadPAP>) =>
    client.put<NecesidadPAP>(`/necesidades-pap/${id}`, data),
  delete: (id: number) => client.del<void>(`/necesidades-pap/${id}`),
};

export const expedienteApi = {
  list: () => client.get<Expediente[]>('/expedientes'),
  get: (id: number) => client.get<Expediente>(`/expedientes/${id}`),
  create: (data: Record<string, unknown>) =>
    client.post<Expediente>('/expedientes', data),
  cambiarEstado: (id: number, data: { estado: string; observacion?: string }) =>
    client.put<Expediente>(`/expedientes/${id}/estado`, data),
  disponibilidad: (actividadId: number, necesidadId: number, cantidad: number) =>
    client.get<Disponibilidad>(
      `/expedientes/disponibilidad/${actividadId}/${necesidadId}?cantidadSolicitada=${cantidad}`,
    ),
  uploadDocumento: (expedienteId: number, file: File, tipo: string) =>
    client.upload(`/expedientes/${expedienteId}/documentos`, file, 'archivo', { tipo }),
};

export const notificacionApi = {
  count: () => client.get<{ count: number }>('/notificaciones/count'),
  list: () => client.get<Notificacion[]>('/notificaciones'),
  readAll: () => client.put<void>('/notificaciones/read-all'),
  readOne: (id: number) => client.put<void>(`/notificaciones/${id}/read`),
};

export const notaApi = {
  list: () => client.get<NotaModificatoria[]>('/notas-modificatorias'),
  create: (formData: FormData) => {
    return fetch('/api/notas-modificatorias', {
      method: 'POST',
      credentials: 'include',
      body: formData,
    }).then(async (res) => {
      if (!res.ok) throw new Error((await res.json()).error || 'Error al enviar');
      return res.json();
    });
  },
  configurar: (id: number, data: Record<string, unknown>) =>
    client.put<void>(`/notas-modificatorias/${id}/configurar`, data),
  rechazar: (id: number, data: { observacion: string }) =>
    client.put<void>(`/notas-modificatorias/${id}/rechazar`, data),
};

export const dashboardApi = {
  alertas: () => client.get<DashboardAlertas>('/dashboard/alertas'),
  saldos: () => client.get<DashboardSaldos>('/dashboard/saldos'),
};
