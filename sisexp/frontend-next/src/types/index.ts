// ─── Enums ───

export type RolUsuario =
  | 'Administrador'
  | 'Coordinacion'
  | 'Secretaria'
  | 'Director'
  | 'Laboratorio'
  | 'Decanato';

export type EstadoExpediente =
  | 'Borrador'
  | 'En_revision'
  | 'Aprobado'
  | 'Rechazado'
  | 'Finalizado'
  | 'Observado'
  | 'Derivado';

export type Urgencia = 'Urgente' | 'No tan urgente' | 'Puede esperar';

export type Naturaleza = 'Bien' | 'Servicio';

export type EstadoActividad =
  | 'Pendiente'
  | 'En_proceso'
  | 'Finalizada'
  | 'Extemporanea';

export type EstadoNota =
  | 'pendiente'
  | 'configurada'
  | 'rechazada';

export type TipoNota =
  | 'inclusion_item'
  | 'inclusion_actividad';

export type TipoDocumento =
  | 'TDR'
  | 'Especificaciones_Tecnicas'
  | 'Cotizacion'
  | 'Informe_Tecnico';

// ─── API Response Types ───

export interface Usuario {
  id: number;
  nombre: string;
  email: string;
  rol: RolUsuario;
  activo: boolean;
  horarioRestringido: boolean;
  createdAt?: string;
}

export interface AuthResponse {
  usuario: Usuario;
}

export interface TechoPresupuestal {
  id: number;
  año: number;
  montoTotal: number;
  montoUtilizado: number;
  activo: boolean;
  planificado: boolean;
  createdAt?: string;
}

export interface ActividadPOI {
  id: number;
  codigo: string;
  nombre: string;
  presupuestoAsignado: number;
  saldoEjecutado: number;
  saldoComprometido: number;
  disponible: number;
  fechaLimite: string | null;
  estado: string;
  planificado: boolean;
  año?: number;
  techo?: TechoPresupuestal;
}

export interface NecesidadPAP {
  id: number;
  nombre: string;
  cantidad: number;
  cantidadDisponible: number;
  cantidadEjecutada: number;
  precioEstimado: number;
  montoDisponible: number;
  montoEjecutado: number;
  unidad: string;
  oficinaLaboratorio: string;
  tipo: Naturaleza;
  clasificadorGasto: string;
  actividadPoiId: number;
  actividad?: ActividadPOI;
}

export interface DocumentoAdjunto {
  id: number;
  tipo: TipoDocumento;
  nombreOriginal: string;
  tamaño: number;
  createdAt: string;
}

export interface SeguimientoLog {
  id: number;
  estadoAnterior: string | null;
  estadoNuevo: string;
  observacion: string | null;
  usuario: { nombre: string; rol: string } | null;
  createdAt: string;
}

export interface Expediente {
  id: number;
  codigo: string;
  descripcion: string;
  urgencia: Urgencia;
  naturaleza: Naturaleza;
  estado: EstadoExpediente;
  cantidadSolicitada: number;
  costoEstimado: number;
  observacion: string | null;
  actividadPOI: ActividadPOI | null;
  necesidadPAP: NecesidadPAP | null;
  solicitante: { nombre: string; rol: string } | null;
  documentos: DocumentoAdjunto[];
  logs: SeguimientoLog[];
  createdAt: string;
  updatedAt: string;
}

export interface Notificacion {
  id: number;
  tipo: string;
  mensaje: string;
  leida: boolean;
  createdAt: string;
}

export interface NotaModificatoria {
  id: number;
  codigo: string;
  tipo: TipoNota;
  estado: EstadoNota;
  nuevoNombre: string;
  justificacion: string;
  costoEstimadoReferencial: number;
  montoTransferir: number;
  origen: string;
  solicitante: { nombre: string };
  actividadExistente: { id: number; codigo: string } | null;
  actividadOrigen: { id: number; codigo: string } | null;
  nombreArchivo: string | null;
  observacionAdmin: string | null;
  createdAt: string;
}

// ─── Extended Response Types ───

export interface DashboardAlertas {
  resumen: { rojas: number; amarillas: number; verdes: number };
  actividades: Array<{
    id: number;
    codigo: string;
    nombre: string;
    semaforo: string;
    diasRestantes: number;
    pctEjecucion: number;
    saldoDisponible: number;
  }>;
  expedientes: Array<{
    id: number;
    codigo: string;
    estado: string;
    urgencia: string;
    descripcion: string;
    semaforo: string;
    diasSinMovimiento: number;
  }>;
}

export interface DashboardSaldos {
  techos: TechoPresupuestal[];
  actividades: Array<ActividadPOI & {
    asignado: number;
    comprometido: number;
    ejecutado: number;
    disponible: number;
    año: number;
  }>;
}

export interface Disponibilidad {
  fechaLimite: { ok: boolean; error?: string };
  saldo: { ok: boolean; disponible: number; asignado: number; comprometido: number; ejecutado: number; error?: string };
  costo: number;
  necesidad: NecesidadPAP | null;
  pap: { precioUnitario: number; unidad: string; cantidadPlanificada: number; cantidadDisponible: number; cantidadEjecutada: number } | null;
}
