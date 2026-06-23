package com.upla.sisexp.service;

import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.model.ActividadPOI;
import com.upla.sisexp.model.Expediente;
import com.upla.sisexp.model.TechoPresupuestal;
import com.upla.sisexp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class DashboardService {

    private final ExpedienteRepository expedienteRepo;
    private final ActividadPOIRepository actividadRepo;
    private final TechoPresupuestalRepository techoRepo;

    private static final List<EstadoExpediente> ESTADOS_TERMINALES = List.of(
            EstadoExpediente.Finalizado, EstadoExpediente.Rechazado);

    public DashboardService(ExpedienteRepository expedienteRepo,
            ActividadPOIRepository actividadRepo,
            TechoPresupuestalRepository techoRepo) {
        this.expedienteRepo = expedienteRepo;
        this.actividadRepo = actividadRepo;
        this.techoRepo = techoRepo;
    }

    public Map<String, Object> getKPIs() {
        Map<String, Object> kpis = new LinkedHashMap<>();
        long total = expedienteRepo.count();
        kpis.put("total", total);

        for (EstadoExpediente estado : EstadoExpediente.values()) {
            kpis.put(estado.name(), expedienteRepo.countByEstado(estado));
        }

        long vencidos = expedienteRepo.countByFechaLimiteBeforeAndEstadoNotIn(
                LocalDate.now(), ESTADOS_TERMINALES);
        kpis.put("vencidos", vencidos);

        return kpis;
    }

    public List<Map<String, Object>> getSaldos() {
        List<TechoPresupuestal> techosActivos = techoRepo.findByActivoTrue();
        List<Map<String, Object>> resultado = new ArrayList<>();

        for (TechoPresupuestal techo : techosActivos) {
            List<ActividadPOI> actividades = actividadRepo
                    .findByTechoPresupuestal_Id(techo.getId());

            for (ActividadPOI act : actividades) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", act.getId());
                item.put("codigo", act.getCodigo());
                item.put("nombre", act.getNombre());
                item.put("techoAño", techo.getAño());
                item.put("presupuesto", act.getPresupuestoAsignado());
                item.put("comprometido", act.getSaldoComprometido());
                item.put("ejecutado", act.getSaldoEjecutado());

                BigDecimal disponible = act.getPresupuestoAsignado()
                        .subtract(act.getSaldoComprometido())
                        .subtract(act.getSaldoEjecutado());
                item.put("disponible", disponible);

                if (act.getPresupuestoAsignado().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal pctEjecutado = act.getSaldoEjecutado()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(act.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP);
                    BigDecimal pctComprometido = act.getSaldoComprometido()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(act.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP);
                    BigDecimal pctDisponible = disponible
                            .multiply(BigDecimal.valueOf(100))
                            .divide(act.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP);
                    item.put("pctEjecutado", pctEjecutado);
                    item.put("pctComprometido", pctComprometido);
                    item.put("pctDisponible", pctDisponible);
                } else {
                    item.put("pctEjecutado", BigDecimal.ZERO);
                    item.put("pctComprometido", BigDecimal.ZERO);
                    item.put("pctDisponible", BigDecimal.ZERO);
                }

                resultado.add(item);
            }
        }

        return resultado;
    }

    public List<Expediente> getAlertas() {
        LocalDate hoy = LocalDate.now();
        LocalDate en7Dias = hoy.plusDays(7);

        List<Expediente> alertas = new ArrayList<>();
        alertas.addAll(expedienteRepo.findByFechaLimiteBetweenAndEstadoNotIn(
                hoy, en7Dias, ESTADOS_TERMINALES));
        alertas.addAll(expedienteRepo.findByFechaLimiteBeforeAndEstadoNotIn(
                hoy, ESTADOS_TERMINALES));

        return alertas;
    }
}
