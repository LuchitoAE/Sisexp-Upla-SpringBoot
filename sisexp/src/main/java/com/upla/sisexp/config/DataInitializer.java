package com.upla.sisexp.config;

import com.upla.sisexp.enums.*;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(2)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RolRepository rolRepo;
    private final UsuarioRepository usuarioRepo;
    private final TechoPresupuestalRepository techoRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final ExpedienteRepository expedienteRepo;
    private final SeguimientoLogRepository seguimientoRepo;
    private final NotificacionRepository notificacionRepo;
    private final DocumentoAdjuntoRepository documentoRepo;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DataInitializer(RolRepository rolRepo, UsuarioRepository usuarioRepo,
            TechoPresupuestalRepository techoRepo, ActividadPOIRepository actividadRepo,
            NecesidadPAPRepository necesidadRepo, ExpedienteRepository expedienteRepo,
            SeguimientoLogRepository seguimientoRepo, NotificacionRepository notificacionRepo,
            DocumentoAdjuntoRepository documentoRepo,
            PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.expedienteRepo = expedienteRepo;
        this.seguimientoRepo = seguimientoRepo;
        this.notificacionRepo = notificacionRepo;
        this.documentoRepo = documentoRepo;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        if (usuarioRepo.count() > 0) {
            log.info("Limpiando datos existentes para nuevo seed...");
            jdbc.execute("TRUNCATE TABLE notificaciones RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE seguimiento_logs RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE documentos_adjuntos RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE expedientes RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE necesidades_pap RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE actividades_poi RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE techos_presupuestales RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE usuarios RESTART IDENTITY CASCADE");
            jdbc.execute("TRUNCATE TABLE roles RESTART IDENTITY CASCADE");
        }

        log.info("=== Iniciando seed data SISEXP-UPLA ===");

        List<Rol> roles = seedRoles();
        List<Usuario> usuarios = seedUsuarios();
        List<TechoPresupuestal> techos = seedTechos(usuarios);
        List<ActividadPOI> actividades = seedActividades(techos);
        List<NecesidadPAP> necesidades = seedNecesidades(actividades);
        List<Expediente> expedientes = seedExpedientes(actividades, necesidades, usuarios);
        seedSeguimientoLogs(expedientes, usuarios);
        seedNotificaciones(usuarios, expedientes);

        log.info("=== Seed completado: {} roles, {} usuarios, {} techos, {} POI, {} PAP, {} expedientes ===",
                roles.size(), usuarios.size(), techos.size(), actividades.size(),
                necesidades.size(), expedientes.size());
    }

    private List<Rol> seedRoles() {
        log.info("Insertando roles...");
        List<Rol> roles = rolRepo.saveAll(List.of(
                crearRol("ADMIN", "Administrador", "Acceso total al sistema"),
                crearRol("COORD", "Coordinacion", "Gestion administrativa y aprobaciones"),
                crearRol("SEC", "Secretaria", "Gestion de expedientes y documentos"),
                crearRol("DIR", "Director", "Validacion y revision"),
                crearRol("LAB", "Laboratorio", "Solicitudes de recursos"),
                crearRol("DEC", "Decanato", "Solo lectura y reportes")
        ));
        return roles;
    }

    private Rol crearRol(String codigo, String nombre, String descripcion) {
        Rol r = new Rol();
        r.setCodigo(codigo);
        r.setNombre(nombre);
        r.setDescripcion(descripcion);
        return r;
    }

    private List<Usuario> seedUsuarios() {
        log.info("Insertando usuarios...");
        List<Usuario> usuarios = usuarioRepo.saveAll(List.of(
                crearUsuario("Jefe Administrativo", "jefe@upla.edu.pe", "jefe123",
                        RolUsuario.Administrador, false),
                crearUsuario("Coordinador Admin", "coord@upla.edu.pe", "coord123",
                        RolUsuario.Coordinacion, true),
                crearUsuario("Secretaria General", "secretaria@upla.edu.pe", "secretaria123",
                        RolUsuario.Secretaria, true),
                crearUsuario("Director de Escuela", "director@upla.edu.pe", "director123",
                        RolUsuario.Director, true),
                crearUsuario("Resp. Laboratorio", "lab@upla.edu.pe", "lab123",
                        RolUsuario.Laboratorio, true),
                crearUsuario("Decano", "decanato@upla.edu.pe", "decanato123",
                        RolUsuario.Decanato, true)
        ));
        return usuarios;
    }

    private Usuario crearUsuario(String nombre, String email, String password,
            RolUsuario rol, boolean horarioRestringido) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setRol(rol);
        u.setActivo(true);
        u.setHorarioRestringido(horarioRestringido);
        u.setIntentosFallidos(0);
        return u;
    }

    private List<TechoPresupuestal> seedTechos(List<Usuario> usuarios) {
        log.info("Insertando techos presupuestales...");

        TechoPresupuestal t2025 = new TechoPresupuestal();
        t2025.setAño(2025);
        t2025.setMontoTotal(new BigDecimal("45000"));
        t2025.setMontoUtilizado(new BigDecimal("45000"));
        t2025.setCreadoPor(usuarios.get(0));
        t2025.setActivo(true);
        t2025.setPlanificado(true);

        TechoPresupuestal t2026 = new TechoPresupuestal();
        t2026.setAño(2026);
        t2026.setMontoTotal(new BigDecimal("115000"));
        t2026.setMontoUtilizado(BigDecimal.ZERO);
        t2026.setCreadoPor(usuarios.get(0));
        t2026.setActivo(true);
        t2026.setPlanificado(false);

        return techoRepo.saveAll(List.of(t2025, t2026));
    }

    private List<ActividadPOI> seedActividades(List<TechoPresupuestal> techos) {
        log.info("Insertando actividades POI...");
        TechoPresupuestal t2025 = techos.get(0);
        TechoPresupuestal t2026 = techos.get(1);

        List<ActividadPOI> historicas2025 = actividadRepo.saveAll(List.of(
                crearPOI("POI-1.01", "Mantenimiento de equipos", bd(12000), bd(0), bd(12000),
                        LocalDate.of(2025, 6, 30), EstadoActividad.Cerrado, true, t2025),
                crearPOI("POI-1.02", "Capacitacion docente", bd(8000), bd(0), bd(8000),
                        LocalDate.of(2025, 8, 15), EstadoActividad.Cerrado, true, t2025),
                crearPOI("POI-1.03", "Aula virtual", bd(15000), bd(0), bd(15000),
                        LocalDate.of(2025, 12, 31), EstadoActividad.Cerrado, true, t2025),
                crearPOI("POI-1.04", "Material didactico", bd(10000), bd(0), bd(10000),
                        LocalDate.of(2025, 10, 15), EstadoActividad.Cerrado, true, t2025)
        ));

        List<ActividadPOI> vigentes2026 = actividadRepo.saveAll(List.of(
                crearPOI("POI-2.01", "Mantenimiento de infraestructura electrica", bd(7500), bd(0), bd(0),
                        LocalDate.of(2026, 5, 31), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.02", "Equipamiento de laboratorios de computo", bd(15000), bd(2000), bd(7500),
                        LocalDate.of(2026, 8, 31), EstadoActividad.En_Ejecucion, false, t2026),
                crearPOI("POI-2.03", "Renovacion de software academico", bd(10000), bd(0), bd(0),
                        LocalDate.of(2026, 7, 31), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.04", "Organizacion de congreso institucional", bd(8000), bd(0), bd(0),
                        LocalDate.of(2026, 9, 30), EstadoActividad.En_Ejecucion, false, t2026),
                crearPOI("POI-2.05", "Mejoramiento de aulas y pabellones", bd(9000), bd(0), bd(0),
                        LocalDate.of(2026, 11, 30), EstadoActividad.Pendiente, false, t2026)
        ));

        return List.of(historicas2025.get(0), historicas2025.get(1), historicas2025.get(2), historicas2025.get(3),
                vigentes2026.get(0), vigentes2026.get(1), vigentes2026.get(2), vigentes2026.get(3),
                vigentes2026.get(4));
    }

    private ActividadPOI crearPOI(String codigo, String nombre, BigDecimal presupuesto,
            BigDecimal saldoComprometido, BigDecimal saldoEjecutado,
            LocalDate fechaLimite, EstadoActividad estado, boolean planificado,
            TechoPresupuestal techo) {
        ActividadPOI a = new ActividadPOI();
        a.setCodigo(codigo);
        a.setNombre(nombre);
        a.setPresupuestoAsignado(presupuesto);
        a.setSaldoComprometido(saldoComprometido);
        a.setSaldoEjecutado(saldoEjecutado);
        a.setFechaLimite(fechaLimite);
        a.setEstado(estado);
        a.setPlanificado(planificado);
        a.setTechoPresupuestal(techo);
        return a;
    }

    private List<NecesidadPAP> seedNecesidades(List<ActividadPOI> actividades) {
        log.info("Insertando necesidades PAP...");
        ActividadPOI poiComp = actividadPorCodigo(actividades, "POI-2.02");
        ActividadPOI poiCong = actividadPorCodigo(actividades, "POI-2.04");

        return necesidadRepo.saveAll(List.of(
                crearPAP("Computadoras Core i7", 10, bd(3500), "UNIDAD", "Lab. Computo 02",
                        Naturaleza.Bien, "2.3.1.2.1.1", 7, bd(24500), 3, bd(10500), poiComp),
                crearPAP("Proyectores HD 4K", 5, bd(2000), "UNIDAD", "Lab. Computo 02",
                        Naturaleza.Bien, "2.3.1.2.1.1", 3, bd(6000), 2, bd(4000), poiComp),
                crearPAP("Servicio de catering", 1, bd(3750), "SERVICIO", "Oficina de Eventos",
                        Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(3750), 0, bd(0), poiCong),
                crearPAP("Material de oficina", 100, bd(25), "UNIDAD", "Almacen Central",
                        Naturaleza.Bien, "2.3.1.2.1.1", 100, bd(2500), 0, bd(0), poiCong)
        ));
    }

    private NecesidadPAP crearPAP(String nombre, int cantidad, BigDecimal precioEstimado,
            String unidad, String oficina, Naturaleza tipo, String clasificador,
            int cantDisp, BigDecimal montoDisp, int cantEje, BigDecimal montoEje,
            ActividadPOI actividad) {
        NecesidadPAP n = new NecesidadPAP();
        n.setNombre(nombre);
        n.setCantidad(cantidad);
        n.setPrecioEstimado(precioEstimado);
        n.setUnidad(unidad);
        n.setOficinaLaboratorio(oficina);
        n.setTipo(tipo);
        n.setClasificadorGasto(clasificador);
        n.setCantidadDisponible(cantDisp);
        n.setMontoDisponible(montoDisp);
        n.setCantidadEjecutada(cantEje);
        n.setMontoEjecutado(montoEje);
        n.setActividadPOI(actividad);
        return n;
    }

    private List<Expediente> seedExpedientes(List<ActividadPOI> actividades,
            List<NecesidadPAP> necesidades, List<Usuario> usuarios) {
        log.info("Insertando expedientes de prueba...");
        ActividadPOI poiComp = actividadPorCodigo(actividades, "POI-2.02");
        NecesidadPAP papComp = necesidadPorNombre(necesidades, "Computadoras Core i7");
        NecesidadPAP papProy = necesidadPorNombre(necesidades, "Proyectores HD 4K");

        Usuario jefe = usuarioPorEmail(usuarios, "jefe@upla.edu.pe");
        Usuario coord = usuarioPorEmail(usuarios, "coord@upla.edu.pe");
        Usuario secretaria = usuarioPorEmail(usuarios, "secretaria@upla.edu.pe");
        Usuario director = usuarioPorEmail(usuarios, "director@upla.edu.pe");
        Usuario lab = usuarioPorEmail(usuarios, "lab@upla.edu.pe");

        return expedienteRepo.saveAll(List.of(
                crearExpediente("EXP-2026-0001", poiComp, papComp, lab,
                        Urgencia.Urgente, Naturaleza.Bien,
                        "Renovacion de 3 computadoras del Lab. Computo 02 por fallas de hardware",
                        EstadoExpediente.Finalizado, null, 3, bd(10500)),
                crearExpediente("EXP-2026-0002", poiComp, papProy, director,
                        Urgencia.No_tan_urgente, Naturaleza.Bien,
                        "Adquisicion de 1 proyector 4K para sala de conferencias",
                        EstadoExpediente.En_revision, null, 1, bd(2000)),
                crearExpediente("EXP-2026-0003", poiComp, papComp, lab,
                        Urgencia.Urgente, Naturaleza.Bien,
                        "Compra de 1 computadora adicional para nuevo docente investigador",
                        EstadoExpediente.Aprobado, null, 1, bd(3500)),
                crearExpediente("EXP-2026-0004", poiComp, papProy, secretaria,
                        Urgencia.Puede_esperar, Naturaleza.Bien,
                        "Solicitud de 2 proyectores para pabellon B — requiere cotizacion actualizada",
                        EstadoExpediente.Observado, "Falta adjuntar cotizacion actualizada del proveedor", 2, bd(4000)),
                crearExpediente("EXP-2026-0005", poiComp, papComp, director,
                        Urgencia.Urgente, Naturaleza.Bien,
                        "Adquisicion de microscopio binocular para practicas de biologia celular",
                        EstadoExpediente.Borrador, null, 1, bd(2500)),
                crearExpediente("EXP-2026-0006", poiComp, papProy, lab,
                        Urgencia.Urgente, Naturaleza.Bien,
                        "Proyector para Laboratorio de Fisica — reemplazo por equipo danado",
                        EstadoExpediente.Rechazado, "Presupuesto insuficiente para este periodo fiscal", 1, bd(2000))
        ));
    }

    private Expediente crearExpediente(String codigo, ActividadPOI poi, NecesidadPAP pap,
            Usuario solicitante, Urgencia urgencia, Naturaleza naturaleza,
            String descripcion, EstadoExpediente estado, String observacion,
            int cantidad, BigDecimal costo) {
        Expediente e = new Expediente();
        e.setCodigo(codigo);
        e.setActividadPOI(poi);
        e.setNecesidadPAP(pap);
        e.setSolicitante(solicitante);
        e.setUrgencia(urgencia);
        e.setNaturaleza(naturaleza);
        e.setDescripcion(descripcion);
        e.setEstado(estado);
        e.setObservacion(observacion);
        e.setCantidadSolicitada(cantidad);
        e.setCostoEstimado(costo);
        return e;
    }

    private void seedSeguimientoLogs(List<Expediente> expedientes, List<Usuario> usuarios) {
        log.info("Insertando historial de seguimiento...");
        Usuario lab = usuarioPorEmail(usuarios, "lab@upla.edu.pe");
        Usuario coord = usuarioPorEmail(usuarios, "coord@upla.edu.pe");
        Usuario secretaria = usuarioPorEmail(usuarios, "secretaria@upla.edu.pe");

        Expediente e1 = expedientePorCodigo(expedientes, "EXP-2026-0001");
        Expediente e2 = expedientePorCodigo(expedientes, "EXP-2026-0002");
        Expediente e3 = expedientePorCodigo(expedientes, "EXP-2026-0003");
        Expediente e4 = expedientePorCodigo(expedientes, "EXP-2026-0004");

        seguimientoRepo.saveAll(List.of(
                crearLog(e1, null, "Borrador", lab, "Expediente creado", dt(2026, 3, 10)),
                crearLog(e1, "Borrador", "En_revision", coord, "Enviado a revision", dt(2026, 3, 11)),
                crearLog(e1, "En_revision", "Aprobado", coord, "Cumple con los requisitos", dt(2026, 3, 12)),
                crearLog(e1, "Aprobado", "Finalizado", secretaria, "Ejecutado satisfactoriamente", dt(2026, 3, 15)),

                crearLog(e2, null, "Borrador", usuarios.get(3), "Expediente creado", dt(2026, 4, 1)),
                crearLog(e2, "Borrador", "En_revision", coord, "Enviado a revision por Direccion", dt(2026, 4, 2)),

                crearLog(e3, null, "Borrador", lab, "Expediente creado", dt(2026, 4, 20)),
                crearLog(e3, "Borrador", "En_revision", coord, "Enviado a revision", dt(2026, 4, 21)),
                crearLog(e3, "En_revision", "Aprobado", coord, "Presupuesto verificado y aprobado", dt(2026, 4, 22)),

                crearLog(e4, null, "Borrador", secretaria, "Expediente creado", dt(2026, 5, 5)),
                crearLog(e4, "Borrador", "En_revision", coord, "Enviado a revision", dt(2026, 5, 6)),
                crearLog(e4, "En_revision", "Observado", coord,
                        "Falta adjuntar cotizacion actualizada del proveedor", dt(2026, 5, 7))
        ));
    }

    private SeguimientoLog crearLog(Expediente e, String estadoAnterior, String estadoNuevo,
            Usuario usuario, String observacion, LocalDateTime fecha) {
        SeguimientoLog log = new SeguimientoLog();
        log.setExpediente(e);
        log.setEstadoAnterior(estadoAnterior);
        log.setEstadoNuevo(estadoNuevo);
        log.setUsuario(usuario);
        log.setObservacion(observacion);
        log.setCreatedAt(fecha);
        return log;
    }

    private void seedNotificaciones(List<Usuario> usuarios, List<Expediente> expedientes) {
        log.info("Insertando notificaciones...");
        Usuario lab = usuarioPorEmail(usuarios, "lab@upla.edu.pe");
        Usuario coord = usuarioPorEmail(usuarios, "coord@upla.edu.pe");
        Usuario director = usuarioPorEmail(usuarios, "director@upla.edu.pe");
        Usuario secretaria = usuarioPorEmail(usuarios, "secretaria@upla.edu.pe");

        Expediente e1 = expedientePorCodigo(expedientes, "EXP-2026-0001");
        Expediente e2 = expedientePorCodigo(expedientes, "EXP-2026-0002");
        Expediente e3 = expedientePorCodigo(expedientes, "EXP-2026-0003");
        Expediente e4 = expedientePorCodigo(expedientes, "EXP-2026-0004");

        notificacionRepo.saveAll(List.of(
                crearNotif(lab, "Su expediente EXP-2026-0001 ha sido aprobado",
                        TipoNotificacion.aprobacion, e1),
                crearNotif(lab, "Su expediente EXP-2026-0003 ha sido aprobado",
                        TipoNotificacion.aprobacion, e3),
                crearNotif(coord, "EXP-2026-0002 requiere revision",
                        TipoNotificacion.info, e2),
                crearNotif(director, "EXP-2026-0002 ha sido enviado a revision",
                        TipoNotificacion.info, e2),
                crearNotif(secretaria, "EXP-2026-0004 tiene observaciones",
                        TipoNotificacion.observacion, e4),
                crearNotif(lab, "Alerta: actividad POI-2.02 proxima a vencer (31/08/2026)",
                        TipoNotificacion.alerta_fecha, null)
        ));
    }

    private Notificacion crearNotif(Usuario usuario, String mensaje,
            TipoNotificacion tipo, Expediente expediente) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setExpediente(expediente);
        n.setLeida(false);
        return n;
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }

    private LocalDateTime dt(int year, int month, int day) {
        return LocalDateTime.of(year, month, day, 10, 0);
    }

    private ActividadPOI actividadPorCodigo(List<ActividadPOI> list, String codigo) {
        return list.stream().filter(a -> codigo.equals(a.getCodigo())).findFirst().orElseThrow();
    }

    private NecesidadPAP necesidadPorNombre(List<NecesidadPAP> list, String nombre) {
        return list.stream().filter(n -> nombre.equals(n.getNombre())).findFirst().orElseThrow();
    }

    private Expediente expedientePorCodigo(List<Expediente> list, String codigo) {
        return list.stream().filter(e -> codigo.equals(e.getCodigo())).findFirst().orElseThrow();
    }

    private Usuario usuarioPorEmail(List<Usuario> list, String email) {
        return list.stream().filter(u -> email.equals(u.getEmail())).findFirst().orElseThrow();
    }
}
