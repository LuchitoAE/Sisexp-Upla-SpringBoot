package com.upla.sisexp.expediente.config;

import com.upla.sisexp.common.enums.EstadoExpediente;
import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.common.enums.Urgencia;
import com.upla.sisexp.expediente.model.Expediente;
import com.upla.sisexp.expediente.model.SeguimientoLog;
import com.upla.sisexp.expediente.repository.ExpedienteRepository;
import com.upla.sisexp.expediente.repository.SeguimientoLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private final ExpedienteRepository expedienteRepo;
    private final SeguimientoLogRepository logRepo;

    public DataInitializer(ExpedienteRepository expedienteRepo, SeguimientoLogRepository logRepo) {
        this.expedienteRepo = expedienteRepo;
        this.logRepo = logRepo;
    }

    @Override
    public void run(String... args) {
        if (expedienteRepo.count() > 0) {
            log.info("Expediente DB ya tiene datos, omitiendo seed");
            return;
        }
        log.info("Sembrando expedientes de muestra...");

        crear("EXP-2026-001", "Solicitud de computadoras para laboratorio de computo",
            4L, 1L, 5L, Urgencia.Urgente, Naturaleza.Bien, 10, 28000,
            EstadoExpediente.Aprobado, "Equipamiento urgente para inicio de ciclo", 1L);
        crear("EXP-2026-002", "Reparacion de techo en pabellon B",
            1L, 2L, 4L, Urgencia.No_tan_urgente, Naturaleza.Servicio, 5, 6000,
            EstadoExpediente.En_revision, null, 2L);
        crear("EXP-2026-003", "Certificacion internacional docentes ingenieria",
            9L, 2L, 5L, Urgencia.Puede_esperar, Naturaleza.Servicio, 5, 7500,
            EstadoExpediente.Borrador, null, null);
        crear("EXP-2026-004", "Adquisicion de proyectores para aulas",
            5L, 2L, 5L, Urgencia.Urgente, Naturaleza.Bien, 3, 5400,
            EstadoExpediente.Observado, "Falta cotizacion de proveedor alternativo", 1L);
        crear("EXP-2026-005", "Mantenimiento de ascensores edificio central",
            3L, 4L, 3L, Urgencia.No_tan_urgente, Naturaleza.Servicio, 1, 3500,
            EstadoExpediente.Rechazado, "Partida presupuestal insuficiente para este periodo", 2L);
        crear("EXP-2026-006", "Servicio de limpieza segundo semestre",
            13L, 2L, 3L, Urgencia.No_tan_urgente, Naturaleza.Servicio, 6, 27000,
            EstadoExpediente.Finalizado, "Servicio completado satisfactoriamente", 1L);
        crear("EXP-2026-007", "Licencias software academico 2026",
            15L, 3L, 2L, Urgencia.Urgente, Naturaleza.Bien, 15, 9000,
            EstadoExpediente.Derivado, "Derivado a DGA para tramite de pago", 2L);
        crear("EXP-2026-008", "Tablets para programa inclusion digital",
            18L, 2L, 4L, Urgencia.Puede_esperar, Naturaleza.Bien, 20, 24000,
            EstadoExpediente.Borrador, null, null);

        log.info("Seed completado: {} expedientes, {} logs",
            expedienteRepo.count(), logRepo.count());
    }

    private void crear(String codigo, String descripcion, Long necesidadId, Long actividadId,
                       Long solicitanteId, Urgencia urgencia, Naturaleza naturaleza,
                       int cantidad, double costo, EstadoExpediente estado,
                       String observacion, Long autorLogId) {
        Expediente exp = new Expediente();
        exp.setCodigo(codigo);
        exp.setDescripcion(descripcion);
        exp.setNecesidadPAPId(necesidadId);
        exp.setActividadPOIId(actividadId);
        exp.setSolicitanteId(solicitanteId);
        exp.setUrgencia(urgencia);
        exp.setNaturaleza(naturaleza);
        exp.setCantidadSolicitada(cantidad);
        exp.setCostoEstimado(BigDecimal.valueOf(costo));
        exp.setEstado(estado);
        exp.setObservacion(observacion);
        exp.setFechaLimite(LocalDate.of(2026, 12, 31));
        exp = expedienteRepo.save(exp);

        if (autorLogId != null) {
            SeguimientoLog log = new SeguimientoLog();
            log.setExpediente(exp);
            log.setEstadoNuevo(estado.name());
            log.setUsuarioId(autorLogId);
            log.setObservacion(observacion != null ? observacion : "Expediente creado");
            logRepo.save(log);
        }
    }
}
