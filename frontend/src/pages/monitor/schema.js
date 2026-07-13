export const SERVICE_ENTITIES = {
  'auth-service': {
    name: 'AUTH-SERVICE',
    port: 8081,
    db: 'auth-db',
    dbLabel: 'PostgreSQL Auth :5433',
    tables: ['usuarios'],
    entities: [
      {
        name: 'Usuario',
        table: 'usuarios',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'nombre', type: 'String(150)', nullable: false },
          { name: 'email', type: 'String(254)', nullable: false, unique: true },
          { name: 'password', type: 'String', nullable: false, jsonIgnore: true },
          { name: 'rol', type: 'RolUsuario (enum)', nullable: false },
          { name: 'activo', type: 'Boolean', default: 'true' },
          { name: 'horarioRestringido', type: 'Boolean', default: 'true' },
          { name: 'intentosFallidos', type: 'Integer', default: '0' },
          { name: 'bloqueadoHasta', type: 'LocalDateTime' },
          { name: 'createdAt', type: 'LocalDateTime' },
          { name: 'updatedAt', type: 'LocalDateTime' },
        ]
      }
    ],
    enums: ['RolUsuario (Administrador, Coordinacion, Secretaria, Director, Laboratorio, Decanato)'],
    endpoints: [
      { method: 'POST', path: '/api/auth/login', auth: 'No' },
      { method: 'GET', path: '/api/auth/me', auth: 'JWT' },
      { method: 'GET', path: '/api/usuarios', auth: 'JWT' },
      { method: 'GET', path: '/api/usuarios/{id}', auth: 'JWT' },
      { method: 'POST', path: '/api/usuarios', auth: 'JWT' },
      { method: 'PUT', path: '/api/usuarios/{id}', auth: 'JWT' },
      { method: 'PUT', path: '/api/usuarios/{id}/toggle-activo', auth: 'JWT' },
      { method: 'POST', path: '/api/usuarios/{id}/cambiar-password', auth: 'JWT' },
      { method: 'GET', path: '/api/status', auth: 'No' },
      { method: 'GET', path: '/api/health', auth: 'No' },
    ]
  },
  'presupuesto-service': {
    name: 'PRESUPUESTO-SERVICE',
    port: 8082,
    db: 'presupuesto-db',
    dbLabel: 'PostgreSQL Presupuesto :5434',
    tables: ['techos_presupuestales', 'actividades_poi', 'necesidades_pap', 'notas_modificatorias'],
    entities: [
      {
        name: 'TechoPresupuestal',
        table: 'techos_presupuestales',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'ano', type: 'Integer', nullable: false },
          { name: 'montoTotal', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'montoUtilizado', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'creadoPorId', type: 'Long' },
          { name: 'activo', type: 'Boolean', nullable: false, default: 'true' },
          { name: 'planificado', type: 'Boolean', nullable: false, default: 'false' },
          { name: 'createdAt', type: 'LocalDateTime' },
          { name: 'updatedAt', type: 'LocalDateTime' },
        ]
      },
      {
        name: 'ActividadPOI',
        table: 'actividades_poi',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'codigo', type: 'String(20)', nullable: false },
          { name: 'nombre', type: 'String(255)', nullable: false },
          { name: 'presupuestoAsignado', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'saldoComprometido', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'saldoEjecutado', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'fechaLimite', type: 'LocalDate' },
          { name: 'estado', type: 'EstadoActividad (enum)', default: 'Pendiente' },
          { name: 'planificado', type: 'Boolean', nullable: false, default: 'false' },
          { name: 'techoPresupuestalId', type: 'Long', fk: 'techos_presupuestales' },
          { name: 'createdAt', type: 'LocalDateTime' },
          { name: 'updatedAt', type: 'LocalDateTime' },
        ]
      },
      {
        name: 'NecesidadPAP',
        table: 'necesidades_pap',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'nombre', type: 'String(255)', nullable: false },
          { name: 'cantidad', type: 'Integer', nullable: false, default: '1' },
          { name: 'precioEstimado', type: 'BigDecimal(10,2)', nullable: false },
          { name: 'unidad', type: 'String' },
          { name: 'oficinaLaboratorio', type: 'String' },
          { name: 'tipo', type: 'Naturaleza (enum)' },
          { name: 'clasificadorGasto', type: 'String' },
          { name: 'cantidadDisponible', type: 'Integer', nullable: false, default: '0' },
          { name: 'montoDisponible', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'cantidadEjecutada', type: 'Integer', nullable: false, default: '0' },
          { name: 'montoEjecutado', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'actividadPOIId', type: 'Long', fk: 'actividades_poi' },
          { name: 'createdAt', type: 'LocalDateTime' },
          { name: 'updatedAt', type: 'LocalDateTime' },
        ]
      },
      {
        name: 'NotaModificatoria',
        table: 'notas_modificatorias',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'codigo', type: 'String(30)', nullable: false },
          { name: 'tipo', type: 'TipoNota (enum)', nullable: false },
          { name: 'nuevoNombre', type: 'String(255)', nullable: false },
          { name: 'justificacion', type: 'TEXT', nullable: false },
          { name: 'costoEstimadoReferencial', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'origen', type: 'String(255)' },
          { name: 'estado', type: 'EstadoNota (enum)', nullable: false, default: 'pendiente' },
          { name: 'montoTransferir', type: 'BigDecimal(12,2)' },
          { name: 'actividadExistenteId', type: 'Long', fk: 'actividades_poi' },
          { name: 'actividadOrigenId', type: 'Long', fk: 'actividades_poi' },
          { name: 'nuevoClasificadorGasto', type: 'String(50)' },
          { name: 'nuevoTipo', type: 'Naturaleza (enum)' },
          { name: 'observacionAdmin', type: 'String(500)' },
          { name: 'nombreArchivo', type: 'String(255)' },
          { name: 'archivoAdjunto', type: 'byte[] (BYTEA)' },
          { name: 'solicitanteId', type: 'Long' },
          { name: 'createdAt', type: 'LocalDateTime' },
          { name: 'updatedAt', type: 'LocalDateTime' },
        ]
      }
    ],
    enums: [
      'EstadoActividad (Pendiente, En_progreso, Finalizada, Cerrado)',
      'EstadoNota (pendiente, configurada, rechazada)',
      'TipoNota (inclusion_item, inclusion_actividad)',
      'Naturaleza (Bien, Servicio)',
    ],
    endpoints: [
      { method: 'GET', path: '/api/techos-presupuestales', auth: 'JWT' },
      { method: 'POST', path: '/api/techos-presupuestales', auth: 'JWT' },
      { method: 'PUT', path: '/api/techos-presupuestales/{id}', auth: 'JWT' },
      { method: 'GET', path: '/api/actividades-poi', auth: 'JWT' },
      { method: 'POST', path: '/api/actividades-poi/techo/{id}', auth: 'JWT' },
      { method: 'PUT', path: '/api/actividades-poi/{id}', auth: 'JWT' },
      { method: 'GET', path: '/api/necesidades-pap', auth: 'JWT' },
      { method: 'POST', path: '/api/necesidades-pap/actividad/{id}', auth: 'JWT' },
      { method: 'PUT', path: '/api/necesidades-pap/{id}', auth: 'JWT' },
      { method: 'GET', path: '/api/notas-modificatorias', auth: 'JWT' },
      { method: 'POST', path: '/api/notas-modificatorias', auth: 'JWT' },
      { method: 'PUT', path: '/api/notas-modificatorias/{id}/configurar', auth: 'JWT' },
      { method: 'PUT', path: '/api/notas-modificatorias/{id}/rechazar', auth: 'JWT' },
      { method: 'GET', path: '/api/dashboard/alertas', auth: 'JWT' },
      { method: 'GET', path: '/api/dashboard/saldos', auth: 'JWT' },
      { method: 'GET', path: '/api/reportes/anual/{anio}', auth: 'JWT' },
      { method: 'GET', path: '/api/reportes/anual/{anio}/excel', auth: 'JWT' },
      { method: 'GET', path: '/api/reportes/anual/{anio}/pdf', auth: 'JWT' },
    ]
  },
  'expediente-service': {
    name: 'EXPEDIENTE-SERVICE',
    port: 8083,
    db: 'expediente-db',
    dbLabel: 'PostgreSQL Expediente :5435',
    tables: ['expedientes', 'documentos_adjuntos', 'seguimiento_logs'],
    entities: [
      {
        name: 'Expediente',
        table: 'expedientes',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'codigo', type: 'String(20)', nullable: false, unique: true },
          { name: 'actividadPOIId', type: 'Long', fk: 'actividades_poi' },
          { name: 'necesidadPAPId', type: 'Long', fk: 'necesidades_pap' },
          { name: 'solicitanteId', type: 'Long', fk: 'usuarios (auth)' },
          { name: 'urgencia', type: 'Urgencia (enum)', nullable: false },
          { name: 'naturaleza', type: 'Naturaleza (enum)' },
          { name: 'descripcion', type: 'TEXT' },
          { name: 'estado', type: 'EstadoExpediente (enum)', default: 'Borrador' },
          { name: 'observacion', type: 'TEXT' },
          { name: 'fechaLimite', type: 'LocalDate' },
          { name: 'cantidadSolicitada', type: 'Integer', nullable: false, default: '1' },
          { name: 'costoEstimado', type: 'BigDecimal(12,2)', nullable: false },
          { name: 'aprobadoPorId', type: 'Long' },
          { name: 'createdAt', type: 'LocalDateTime' },
          { name: 'updatedAt', type: 'LocalDateTime' },
        ]
      },
      {
        name: 'DocumentoAdjunto',
        table: 'documentos_adjuntos',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'expediente', type: '@ManyToOne -> Expediente', nullable: false },
          { name: 'tipo', type: 'TipoDocumento (enum)', nullable: false },
          { name: 'nombreOriginal', type: 'String', nullable: false },
          { name: 'nombreArchivo', type: 'String (UUID)', nullable: false },
          { name: 'mimeType', type: 'String' },
          { name: 'tamano', type: 'Long' },
          { name: 'createdAt', type: 'LocalDateTime' },
        ]
      },
      {
        name: 'SeguimientoLog',
        table: 'seguimiento_logs',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'expediente', type: '@ManyToOne -> Expediente', nullable: false },
          { name: 'estadoAnterior', type: 'String' },
          { name: 'estadoNuevo', type: 'String', nullable: false },
          { name: 'usuarioId', type: 'Long' },
          { name: 'observacion', type: 'TEXT' },
          { name: 'createdAt', type: 'LocalDateTime' },
        ]
      }
    ],
    enums: [
      'EstadoExpediente (Borrador, En_revision, Aprobado, Rechazado, Finalizado, Observado, Derivado)',
      'Urgencia (Urgente, No_tan_urgente, Puede_esperar)',
      'TipoDocumento (TDR, Especificaciones_Tecnicas, Cotizacion, Informe_Tecnico)',
    ],
    endpoints: [
      { method: 'GET', path: '/api/expedientes', auth: 'JWT' },
      { method: 'POST', path: '/api/expedientes', auth: 'JWT' },
      { method: 'GET', path: '/api/expedientes/{id}', auth: 'JWT' },
      { method: 'GET', path: '/api/expedientes/rastreo/{codigo}', auth: 'No' },
      { method: 'GET', path: '/api/expedientes/disponibilidad/{poiId}/{papId}', auth: 'JWT' },
      { method: 'PUT', path: '/api/expedientes/{id}/estado', auth: 'JWT' },
      { method: 'POST', path: '/api/expedientes/{id}/documentos', auth: 'JWT' },
    ]
  },
  'notificacion-service': {
    name: 'NOTIFICACION-SERVICE',
    port: 8084,
    db: 'notificacion-db',
    dbLabel: 'PostgreSQL Notificacion :5436',
    tables: ['notificaciones'],
    entities: [
      {
        name: 'Notificacion',
        table: 'notificaciones',
        fields: [
          { name: 'id', type: 'Long', pk: true },
          { name: 'usuarioId', type: 'Long', fk: 'usuarios (auth)' },
          { name: 'mensaje', type: 'TEXT', nullable: false },
          { name: 'tipo', type: 'TipoNotificacion (enum)' },
          { name: 'leida', type: 'Boolean', nullable: false, default: 'false' },
          { name: 'expedienteId', type: 'Long', fk: 'expedientes (expediente)' },
          { name: 'createdAt', type: 'LocalDateTime' },
        ]
      }
    ],
    enums: [
      'TipoNotificacion (observacion, rechazo, aprobacion, alerta_fecha, nota_aprobada, nota_rechazada, info)',
    ],
    endpoints: [
      { method: 'GET', path: '/api/notificaciones?usuarioId=', auth: 'JWT' },
      { method: 'GET', path: '/api/notificaciones/count?usuarioId=', auth: 'JWT' },
      { method: 'PUT', path: '/api/notificaciones/{id}/leer', auth: 'JWT' },
      { method: 'PUT', path: '/api/notificaciones/leer-todas?usuarioId=', auth: 'JWT' },
    ]
  },
};

export const DB_TABLES = {
  'auth-db': {
    label: 'PostgreSQL Auth :5433',
    service: 'auth-service',
    tables: [
      { name: 'usuarios', entity: 'Usuario', count: 6, endpoint: '/usuarios', cols: ['id', 'nombre', 'email', 'rol', 'activo', 'createdAt'] },
    ]
  },
  'presupuesto-db': {
    label: 'PostgreSQL Presupuesto :5434',
    service: 'presupuesto-service',
    tables: [
      { name: 'techos_presupuestales', entity: 'TechoPresupuestal', count: 5, endpoint: '/techos-presupuestales', cols: ['id', 'ano', 'montoTotal', 'montoUtilizado', 'activo', 'planificado'] },
      { name: 'actividades_poi', entity: 'ActividadPOI', count: 20, endpoint: '/actividades-poi', cols: ['id', 'codigo', 'nombre', 'presupuestoAsignado', 'estado', 'fechaLimite'] },
      { name: 'necesidades_pap', entity: 'NecesidadPAP', count: 80, endpoint: '/necesidades-pap', cols: ['id', 'nombre', 'cantidad', 'precioEstimado', 'tipo', 'actividadPOIId'] },
      { name: 'notas_modificatorias', entity: 'NotaModificatoria', count: 4, endpoint: '/notas-modificatorias', cols: ['id', 'codigo', 'tipo', 'estado', 'justificacion'] },
    ]
  },
  'expediente-db': {
    label: 'PostgreSQL Expediente :5435',
    service: 'expediente-service',
    tables: [
      { name: 'expedientes', entity: 'Expediente', count: 8, endpoint: '/expedientes', cols: ['id', 'codigo', 'estado', 'urgencia', 'costoEstimado', 'createdAt'] },
      { name: 'documentos_adjuntos', entity: 'DocumentoAdjunto', count: '-', endpoint: null, cols: ['id', 'tipo', 'nombreOriginal', 'tamano'] },
      { name: 'seguimiento_logs', entity: 'SeguimientoLog', count: 6, endpoint: null, cols: ['id', 'estadoAnterior', 'estadoNuevo', 'createdAt'] },
    ]
  },
  'notificacion-db': {
    label: 'PostgreSQL Notificacion :5436',
    service: 'notificacion-service',
    tables: [
      { name: 'notificaciones', entity: 'Notificacion', count: '-', endpoint: '/notificaciones', cols: ['id', 'usuarioId', 'mensaje', 'tipo', 'leida', 'createdAt'] },
    ]
  },
};

export const ALL_ENDPOINTS = [
  { method: 'POST', path: '/api/auth/login', auth: 'No', service: 'auth-service', desc: 'Login' },
  { method: 'GET', path: '/api/auth/me', auth: 'JWT', service: 'auth-service', desc: 'Usuario actual' },
  { method: 'GET', path: '/api/usuarios', auth: 'JWT', service: 'auth-service', desc: 'Listar usuarios' },
  { method: 'POST', path: '/api/usuarios', auth: 'JWT', service: 'auth-service', desc: 'Crear usuario' },
  { method: 'GET', path: '/api/status', auth: 'No', service: 'auth-service', desc: 'Status nodos' },
  { method: 'GET', path: '/api/techos-presupuestales', auth: 'JWT', service: 'presupuesto-service', desc: 'Listar techos' },
  { method: 'GET', path: '/api/actividades-poi/techo/{id}', auth: 'JWT', service: 'presupuesto-service', desc: 'Actividades POI' },
  { method: 'GET', path: '/api/necesidades-pap/actividad/{id}', auth: 'JWT', service: 'presupuesto-service', desc: 'Necesidades PAP' },
  { method: 'GET', path: '/api/notas-modificatorias', auth: 'JWT', service: 'presupuesto-service', desc: 'Listar notas' },
  { method: 'PUT', path: '/api/notas-modificatorias/{id}/configurar', auth: 'JWT', service: 'presupuesto-service', desc: 'Configurar nota' },
  { method: 'PUT', path: '/api/notas-modificatorias/{id}/rechazar', auth: 'JWT', service: 'presupuesto-service', desc: 'Rechazar nota' },
  { method: 'GET', path: '/api/dashboard/alertas', auth: 'JWT', service: 'presupuesto-service', desc: 'Alertas dashboard' },
  { method: 'GET', path: '/api/dashboard/saldos', auth: 'JWT', service: 'presupuesto-service', desc: 'Saldos dashboard' },
  { method: 'GET', path: '/api/reportes/anual/{anio}', auth: 'JWT', service: 'presupuesto-service', desc: 'Reporte anual' },
  { method: 'GET', path: '/api/reportes/anual/{anio}/excel', auth: 'JWT', service: 'presupuesto-service', desc: 'Export Excel' },
  { method: 'GET', path: '/api/reportes/anual/{anio}/pdf', auth: 'JWT', service: 'presupuesto-service', desc: 'Export PDF' },
  { method: 'GET', path: '/api/expedientes', auth: 'JWT', service: 'expediente-service', desc: 'Listar expedientes' },
  { method: 'POST', path: '/api/expedientes', auth: 'JWT', service: 'expediente-service', desc: 'Crear expediente' },
  { method: 'GET', path: '/api/expedientes/rastreo/{codigo}', auth: 'No', service: 'expediente-service', desc: 'Rastreo publico' },
  { method: 'PUT', path: '/api/expedientes/{id}/estado', auth: 'JWT', service: 'expediente-service', desc: 'Cambiar estado' },
  { method: 'GET', path: '/api/notificaciones', auth: 'JWT', service: 'notificacion-service', desc: 'Notificaciones' },
  { method: 'GET', path: '/api/notificaciones/count', auth: 'JWT', service: 'notificacion-service', desc: 'Contar no leidas' },
  { method: 'GET', path: '/api/monitor/activity', auth: 'No', service: 'api-gateway', desc: 'Activity feed' },
];

export const INFRA_NODES = [
  { id: 'nginx', title: 'NGINX Frontend', port: ':80', type: 'proxy' },
  { id: 'api-gateway', title: 'API Gateway', port: ':8080', type: 'gateway' },
  { id: 'eureka-server', title: 'Eureka Server', port: ':8761', type: 'discovery' },
  { id: 'auth-service', title: 'AUTH-SERVICE', port: ':8081', type: 'service' },
  { id: 'presupuesto-service', title: 'PRESUPUESTO-SERVICE', port: ':8082', type: 'service' },
  { id: 'expediente-service', title: 'EXPEDIENTE-SERVICE', port: ':8083', type: 'service' },
  { id: 'notificacion-service', title: 'NOTIFICACION-SERVICE', port: ':8084', type: 'service' },
  { id: 'auth-db', title: 'PostgreSQL Auth', port: ':5433', type: 'db' },
  { id: 'presupuesto-db', title: 'PostgreSQL Presup.', port: ':5434', type: 'db' },
  { id: 'expediente-db', title: 'PostgreSQL Exped.', port: ':5435', type: 'db' },
  { id: 'notificacion-db', title: 'PostgreSQL Notif.', port: ':5436', type: 'db' },
  { id: 'rabbitmq', title: 'RabbitMQ', port: ':5672', type: 'broker' },
];

export const INFRA_EDGES = [
  ['nginx', 'api-gateway', '#38bdf8', 'solid'],
  ['api-gateway', 'auth-service', '#38bdf8', 'solid'],
  ['api-gateway', 'presupuesto-service', '#38bdf8', 'solid'],
  ['api-gateway', 'expediente-service', '#38bdf8', 'solid'],
  ['api-gateway', 'notificacion-service', '#38bdf8', 'solid'],
  ['auth-service', 'auth-db', '#4ade80', 'dashed'],
  ['presupuesto-service', 'presupuesto-db', '#4ade80', 'dashed'],
  ['expediente-service', 'expediente-db', '#4ade80', 'dashed'],
  ['notificacion-service', 'notificacion-db', '#4ade80', 'dashed'],
  ['auth-service', 'eureka-server', '#fb923c', 'dotted'],
  ['presupuesto-service', 'eureka-server', '#fb923c', 'dotted'],
  ['expediente-service', 'eureka-server', '#fb923c', 'dotted'],
  ['notificacion-service', 'eureka-server', '#fb923c', 'dotted'],
  ['api-gateway', 'eureka-server', '#fb923c', 'dotted'],
  ['expediente-service', 'rabbitmq', '#c084fc', 'dotted'],
  ['rabbitmq', 'notificacion-service', '#c084fc', 'dotted'],
];
