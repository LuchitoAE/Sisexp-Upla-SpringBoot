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
import org.springframework.transaction.annotation.Transactional;

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
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DataInitializer(RolRepository rolRepo, UsuarioRepository usuarioRepo,
            TechoPresupuestalRepository techoRepo, ActividadPOIRepository actividadRepo,
            NecesidadPAPRepository necesidadRepo, ExpedienteRepository expedienteRepo,
            SeguimientoLogRepository seguimientoRepo, NotificacionRepository notificacionRepo,
            PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.expedienteRepo = expedienteRepo;
        this.seguimientoRepo = seguimientoRepo;
        this.notificacionRepo = notificacionRepo;
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

        log.info("=== Seed completado: {} roles, {} usuarios, {} techos, {} POI ===",
                roles.size(), usuarios.size(), techos.size(), actividades.size());
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
                crearPOI("POI-2.02", "Equipamiento de laboratorios de computo", bd(15000), bd(3600), bd(0),
                        LocalDate.of(2026, 8, 31), EstadoActividad.En_Ejecucion, false, t2026),
                crearPOI("POI-2.03", "Renovacion de software academico", bd(10000), bd(0), bd(0),
                        LocalDate.of(2026, 7, 31), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.04", "Organizacion de congreso institucional", bd(8000), bd(0), bd(3750),
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
        return List.of();
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
        return List.of();
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
