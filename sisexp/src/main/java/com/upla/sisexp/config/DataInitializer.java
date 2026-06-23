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
import java.util.ArrayList;
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
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public DataInitializer(RolRepository rolRepo, UsuarioRepository usuarioRepo,
            TechoPresupuestalRepository techoRepo, ActividadPOIRepository actividadRepo,
            NecesidadPAPRepository necesidadRepo,
            PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.rolRepo = rolRepo;
        this.usuarioRepo = usuarioRepo;
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        boolean primerDespliegue = usuarioRepo.count() == 0;

        if (primerDespliegue) {
            log.info("=== PRIMER DESPLIEGUE: sembrando datos iniciales ===");
            List<Rol> roles = seedRoles();
            List<Usuario> usuarios = seedUsuarios();
            List<TechoPresupuestal> techos = seedTechos(usuarios);
            List<ActividadPOI> actividades = seedActividades(techos);
            List<NecesidadPAP> necesidades = seedNecesidades(actividades);
            log.info("=== Seed inicial: {} roles, {} usuarios, {} techos, {} POI, {} PAP, 0 expedientes ===",
                    roles.size(), usuarios.size(), techos.size(), actividades.size(), necesidades.size());
            return;
        }

        log.info("Limpiando expedientes, PAP, logs y notificaciones existentes...");
        jdbc.execute("TRUNCATE TABLE notificaciones RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE seguimiento_logs RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE documentos_adjuntos RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE expedientes RESTART IDENTITY CASCADE");
        jdbc.execute("TRUNCATE TABLE necesidades_pap RESTART IDENTITY CASCADE");

        log.info("=== SISEXP-UPLA: limpieza de datos transaccionales y resiembra de PAP ===");

        List<ActividadPOI> actividades = actividadRepo.findAll();
        log.info("{} actividades POI existentes conservadas", actividades.size());

        if (actividades.size() <= 9) {
            List<TechoPresupuestal> techos = techoRepo.findAll();
            TechoPresupuestal t2026 = techos.stream()
                    .filter(t -> t.getAño() == 2026).findFirst().orElse(null);
            if (t2026 != null) {
                List<ActividadPOI> nuevas = seedActividadesExtra(t2026);
                actividades = new ArrayList<>(actividades);
                actividades.addAll(nuevas);
                log.info("+{} nuevas actividades POI agregadas", nuevas.size());
            }
        }

        List<NecesidadPAP> necesidades = seedNecesidades(actividades);

        log.info("=== Seed datos: {} POI, {} PAP, 0 expedientes (sistema listo para usar) ===",
                actividades.size(), necesidades.size());
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
                crearPOI("POI-2.02", "Equipamiento de laboratorios de computo", bd(15000), bd(0), bd(0),
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

    private List<ActividadPOI> seedActividadesExtra(TechoPresupuestal t2026) {
        log.info("Agregando mas actividades POI para 2026...");
        return actividadRepo.saveAll(List.of(
                crearPOI("POI-2.06", "Adquisicion de equipos de laboratorio de fisica", bd(12000), bd(0), bd(0),
                        LocalDate.of(2026, 7, 15), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.07", "Implementacion de biblioteca digital", bd(18000), bd(0), bd(0),
                        LocalDate.of(2026, 12, 20), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.08", "Programa de salud ocupacional", bd(6500), bd(0), bd(0),
                        LocalDate.of(2026, 6, 30), EstadoActividad.En_Ejecucion, false, t2026),
                crearPOI("POI-2.09", "Sistema de videovigilancia campus", bd(11000), bd(0), bd(0),
                        LocalDate.of(2026, 10, 31), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.10", "Feria de innovacion tecnologica", bd(5000), bd(0), bd(0),
                        LocalDate.of(2026, 8, 15), EstadoActividad.Pendiente, false, t2026),
                crearPOI("POI-2.11", "Mantenimiento de areas verdes", bd(4000), bd(0), bd(0),
                        LocalDate.of(2026, 4, 30), EstadoActividad.En_Ejecucion, false, t2026),
                crearPOI("POI-2.12", "Programa de becas de investigacion", bd(20000), bd(0), bd(0),
                        LocalDate.of(2026, 11, 15), EstadoActividad.Pendiente, false, t2026)
        ));
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
        log.info("Insertando necesidades PAP para {} actividades...", actividades.size());
        List<NecesidadPAP> todas = new ArrayList<>();

        for (ActividadPOI a : actividades) {
            String codigo = a.getCodigo();
            switch (codigo) {
                case "POI-2.01" -> todas.addAll(List.of(
                    crearPAP("Cableado electrico trifasico", 200, bd(15), "METRO", "Mantenimiento",
                            Naturaleza.Bien, "2.3.1.5.1.1", 200, bd(3000), 0, bd(0), a),
                    crearPAP("Interruptores termomagneticos", 20, bd(85), "UNIDAD", "Mantenimiento",
                            Naturaleza.Bien, "2.3.1.5.1.1", 20, bd(1700), 0, bd(0), a),
                    crearPAP("Servicio tecnico electricista", 1, bd(2800), "SERVICIO", "Mantenimiento",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(2800), 0, bd(0), a)
                ));
                case "POI-2.02" -> todas.addAll(List.of(
                    crearPAP("Computadoras Core i7", 10, bd(3500), "UNIDAD", "Lab. Computo 02",
                            Naturaleza.Bien, "2.3.1.2.1.1", 10, bd(35000), 0, bd(0), a),
                    crearPAP("Proyectores HD 4K", 5, bd(2000), "UNIDAD", "Lab. Computo 02",
                            Naturaleza.Bien, "2.3.1.2.1.1", 5, bd(10000), 0, bd(0), a),
                    crearPAP("Servicio de instalacion de red", 1, bd(1500), "SERVICIO", "Lab. Computo 02",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(1500), 0, bd(0), a)
                ));
                case "POI-2.03" -> todas.addAll(List.of(
                    crearPAP("Licencias software Office 365", 50, bd(120), "UNIDAD", "TI",
                            Naturaleza.Bien, "2.3.1.3.1.1", 50, bd(6000), 0, bd(0), a),
                    crearPAP("Servicio de migracion de datos", 1, bd(4000), "SERVICIO", "TI",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(4000), 0, bd(0), a)
                ));
                case "POI-2.04" -> todas.addAll(List.of(
                    crearPAP("Servicio de catering para 200 personas", 1, bd(3750), "SERVICIO", "Oficina de Eventos",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(3750), 0, bd(0), a),
                    crearPAP("Material de oficina para congreso", 100, bd(25), "UNIDAD", "Almacen Central",
                            Naturaleza.Bien, "2.3.1.2.1.1", 100, bd(2500), 0, bd(0), a),
                    crearPAP("Alquiler de equipo de sonido", 1, bd(1750), "SERVICIO", "Oficina de Eventos",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(1750), 0, bd(0), a)
                ));
                case "POI-2.05" -> todas.addAll(List.of(
                    crearPAP("Pintura latex para interiores", 50, bd(48), "GALON", "Infraestructura",
                            Naturaleza.Bien, "2.3.1.5.1.1", 50, bd(2400), 0, bd(0), a),
                    crearPAP("Carpinteria metalica para ventanas", 30, bd(120), "UNIDAD", "Infraestructura",
                            Naturaleza.Bien, "2.3.1.5.1.1", 30, bd(3600), 0, bd(0), a),
                    crearPAP("Servicio de albanileria", 1, bd(3000), "SERVICIO", "Infraestructura",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(3000), 0, bd(0), a)
                ));
                case "POI-2.06" -> todas.addAll(List.of(
                    crearPAP("Microscopios binoculares", 5, bd(1200), "UNIDAD", "Lab. Fisica",
                            Naturaleza.Bien, "2.3.1.2.1.1", 5, bd(6000), 0, bd(0), a),
                    crearPAP("Osciloscopios digitales", 3, bd(2000), "UNIDAD", "Lab. Fisica",
                            Naturaleza.Bien, "2.3.1.2.1.1", 3, bd(6000), 0, bd(0), a)
                ));
                case "POI-2.07" -> todas.addAll(List.of(
                    crearPAP("Tablets para consulta digital", 20, bd(450), "UNIDAD", "Biblioteca Central",
                            Naturaleza.Bien, "2.3.1.2.1.1", 20, bd(9000), 0, bd(0), a),
                    crearPAP("Plataforma de gestion bibliotecaria", 1, bd(7000), "LICENCIA", "Biblioteca Central",
                            Naturaleza.Bien, "2.3.1.3.1.1", 1, bd(7000), 0, bd(0), a),
                    crearPAP("Servicio de digitalizacion de tesis", 1, bd(2000), "SERVICIO", "Biblioteca Central",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(2000), 0, bd(0), a)
                ));
                case "POI-2.08" -> todas.addAll(List.of(
                    crearPAP("Botiquines de primeros auxilios", 15, bd(120), "UNIDAD", "Bienestar",
                            Naturaleza.Bien, "2.3.1.4.1.1", 15, bd(1800), 0, bd(0), a),
                    crearPAP("Servicio de examen medico ocupacional", 1, bd(4700), "SERVICIO", "Bienestar",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(4700), 0, bd(0), a)
                ));
                case "POI-2.09" -> todas.addAll(List.of(
                    crearPAP("Camaras IP 4K exterior", 20, bd(350), "UNIDAD", "Seguridad",
                            Naturaleza.Bien, "2.3.1.2.1.1", 20, bd(7000), 0, bd(0), a),
                    crearPAP("Servicio de instalacion de CCTV", 1, bd(4000), "SERVICIO", "Seguridad",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(4000), 0, bd(0), a)
                ));
                case "POI-2.10" -> todas.addAll(List.of(
                    crearPAP("Stands modulares para feria", 10, bd(250), "UNIDAD", "Oficina de Eventos",
                            Naturaleza.Bien, "2.3.1.5.1.1", 10, bd(2500), 0, bd(0), a),
                    crearPAP("Servicio de coffee break", 1, bd(1500), "SERVICIO", "Oficina de Eventos",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(1500), 0, bd(0), a),
                    crearPAP("Material promocional impreso", 500, bd(2), "UNIDAD", "Oficina de Eventos",
                            Naturaleza.Bien, "2.3.1.2.1.1", 500, bd(1000), 0, bd(0), a)
                ));
                case "POI-2.11" -> todas.addAll(List.of(
                    crearPAP("Plantas ornamentales", 100, bd(18), "UNIDAD", "Mantenimiento",
                            Naturaleza.Bien, "2.3.1.5.1.1", 100, bd(1800), 0, bd(0), a),
                    crearPAP("Servicio de jardineria mensual", 1, bd(2200), "SERVICIO", "Mantenimiento",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(2200), 0, bd(0), a)
                ));
                case "POI-2.12" -> todas.addAll(List.of(
                    crearPAP("Becas de movilidad academica", 5, bd(2500), "BECA", "Investigacion",
                            Naturaleza.Bien, "2.3.1.6.1.1", 5, bd(12500), 0, bd(0), a),
                    crearPAP("Servicio de publicacion en revistas indexadas", 1, bd(5000), "SERVICIO", "Investigacion",
                            Naturaleza.Servicio, "2.3.2.5.1.1", 1, bd(5000), 0, bd(0), a),
                    crearPAP("Equipos de proteccion para laboratorio", 10, bd(250), "UNIDAD", "Investigacion",
                            Naturaleza.Bien, "2.3.1.4.1.1", 10, bd(2500), 0, bd(0), a)
                ));
            }
        }

        return necesidadRepo.saveAll(todas);
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

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }
}
