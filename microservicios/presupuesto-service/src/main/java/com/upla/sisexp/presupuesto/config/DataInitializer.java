package com.upla.sisexp.presupuesto.config;

import com.upla.sisexp.common.enums.EstadoActividad;
import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final TechoPresupuestalRepository techoRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;

    public DataInitializer(TechoPresupuestalRepository techoRepo,
                           ActividadPOIRepository actividadRepo,
                           NecesidadPAPRepository necesidadRepo) {
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
    }

    @Override
    public void run(String... args) {
        if (techoRepo.count() > 0) {
            log.info("Presupuesto DB ya tiene datos, omitiendo seed");
            return;
        }
        log.info("Sembrando datos presupuestales 2022-2026...");

        seedTecho(2022, 800000, 800000);
        seedTecho(2023, 950000, 950000);
        seedTecho(2024, 1100000, 1100000);
        seedTecho(2025, 1250000, 1250000);
        seedTecho(2026, 1500000, 750000);

        log.info("Seed completado: 5 techos, {} actividades, {} necesidades",
            actividadRepo.count(), necesidadRepo.count());
    }

    private void seedTecho(int año, int montoTotal, int montoUtilizado) {
        TechoPresupuestal techo = new TechoPresupuestal();
        techo.setAño(año);
        techo.setMontoTotal(bd(montoTotal));
        techo.setMontoUtilizado(bd(montoUtilizado));
        techo.setCreadoPorId(1L);
        techo.setActivo(true);
        techo.setPlanificado(año <= 2025);
        techo = techoRepo.save(techo);

        if (año <= 2025) {
            seedActividad(techo, "MANT-" + año, "Mantenimiento de Infraestructura", 0.35, año);
            seedActividad(techo, "EQUIP-" + año, "Equipamiento de Laboratorios", 0.30, año);
            seedActividad(techo, "CAPA-" + año, "Capacitacion Docente", 0.20, año);
            seedActividad(techo, "SERV-" + año, "Servicios Academicos", 0.15, año);
        } else {
            seedActividad(techo, "MANT-2026", "Mantenimiento de Infraestructura", 0.35, 2026);
            seedActividad(techo, "EQUIP-2026", "Equipamiento de Laboratorios", 0.30, 2026);
            seedActividad(techo, "CAPA-2026", "Capacitacion Docente", 0.20, 2026);
            seedActividad(techo, "DIGI-2026", "Transformacion Digital", 0.15, 2026);
        }
    }

    private void seedActividad(TechoPresupuestal techo, String codigo, String nombre,
                               double pctPresupuesto, int año) {
        BigDecimal asignado = techo.getMontoTotal().multiply(bd(pctPresupuesto))
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal ejecutado = techo.getMontoUtilizado().multiply(bd(pctPresupuesto))
            .setScale(2, RoundingMode.HALF_UP);

        ActividadPOI act = new ActividadPOI();
        act.setCodigo(codigo);
        act.setNombre(nombre);
        act.setPresupuestoAsignado(asignado);
        act.setSaldoEjecutado(ejecutado);
        act.setSaldoComprometido(bd(0));
        act.setFechaLimite(LocalDate.of(año, 12, 31));
        act.setEstado(año <= 2025 ? EstadoActividad.Cerrado : EstadoActividad.Pendiente);
        act.setPlanificado(año <= 2025);
        act.setTechoPresupuestalId(techo.getId());
        act = actividadRepo.save(act);

        BigDecimal dispPorItem = asignado.subtract(ejecutado)
            .divide(bd(4), 2, RoundingMode.HALF_UP);
        BigDecimal ejecPorItem = ejecutado.divide(bd(4), 2, RoundingMode.HALF_UP);

        if (codigo.startsWith("MANT")) {
            seedNecesidad(act, "Pintura de aulas", 50, 85, "galones", "Laboratorio", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Reparacion de techos", 20, 1200, "m2", "Laboratorio", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Cambio de luminarias LED", 100, 45, "unidades", "Laboratorio", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Mantenimiento de ascensores", 4, 3500, "unidades", "Laboratorio", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
        } else if (codigo.startsWith("EQUIP")) {
            seedNecesidad(act, "Computadoras de escritorio", 30, 2800, "unidades", "Laboratorio", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Proyectores multimedia", 15, 1800, "unidades", "Laboratorio", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Microscopios electronicos", 10, 4500, "unidades", "Laboratorio", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Servidores para data center", 5, 12000, "unidades", "Laboratorio", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
        } else if (codigo.startsWith("CAPA")) {
            seedNecesidad(act, "Talleres de metodologia", 8, 2500, "talleres", "Decanato", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Certificaciones internacionales", 20, 1500, "certificaciones", "Decanato", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Material didactico digital", 200, 35, "licencias", "Decanato", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Congresos academicos", 5, 8000, "eventos", "Decanato", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
        } else if (codigo.startsWith("SERV")) {
            seedNecesidad(act, "Papeleria y utiles de oficina", 500, 8, "paquetes", "Secretaria", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Servicio de limpieza outsourcing", 12, 4500, "meses", "Secretaria", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Licencias de software academico", 50, 600, "licencias", "Secretaria", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Mobiliario para aulas", 40, 350, "unidades", "Secretaria", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
        } else if (codigo.startsWith("DIGI")) {
            seedNecesidad(act, "Plataforma LMS institucional", 1, 85000, "plataforma", "Direccion", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Tablets para docentes", 60, 1200, "unidades", "Direccion", Naturaleza.Bien, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Software de videoconferencia", 1, 25000, "licencia anual", "Direccion", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
            seedNecesidad(act, "Capacitacion en TICs", 10, 3000, "talleres", "Direccion", Naturaleza.Servicio, ejecPorItem, dispPorItem, año);
        }
    }

    private void seedNecesidad(ActividadPOI act, String nombre, int cantidad, double precio,
                               String unidad, String oficina, Naturaleza tipo,
                               BigDecimal montoEjecutadoPorItem, BigDecimal montoDisponiblePorItem, int año) {
        BigDecimal precioBd = bd(precio);
        BigDecimal total = precioBd.multiply(bd(cantidad));
        boolean hayEjecucion = año <= 2025 || montoEjecutadoPorItem.compareTo(BigDecimal.ZERO) > 0;

        NecesidadPAP n = new NecesidadPAP();
        n.setNombre(nombre);
        n.setCantidad(cantidad);
        n.setPrecioEstimado(precioBd);
        n.setUnidad(unidad);
        n.setOficinaLaboratorio(oficina);
        n.setTipo(tipo);
        n.setClasificadorGasto(tipo == Naturaleza.Bien ? "2.3.1.5.1" : "2.3.2.2.1");
        n.setActividadPOIId(act.getId());

        if (hayEjecucion) {
            int cantEjec = año == 2026 ? cantidad / 2 : cantidad;
            n.setCantidadEjecutada(cantEjec);
            n.setCantidadDisponible(cantidad - cantEjec);
            n.setMontoEjecutado(precioBd.multiply(bd(cantEjec)).setScale(2, RoundingMode.HALF_UP));
            n.setMontoDisponible(precioBd.multiply(bd(cantidad - cantEjec)).setScale(2, RoundingMode.HALF_UP));
        } else {
            n.setCantidadDisponible(cantidad);
            n.setMontoDisponible(total);
        }

        necesidadRepo.save(n);
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }
}
