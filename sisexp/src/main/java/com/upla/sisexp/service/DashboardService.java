package com.upla.sisexp.service;

import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.model.ActividadPOI;
import com.upla.sisexp.model.Expediente;
import com.upla.sisexp.model.NecesidadPAP;
import com.upla.sisexp.model.TechoPresupuestal;
import com.upla.sisexp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ExpedienteRepository expedienteRepo;
    private final ActividadPOIRepository actividadRepo;
    private final TechoPresupuestalRepository techoRepo;
    private final NecesidadPAPRepository necesidadRepo;

    private static final List<EstadoExpediente> ESTADOS_TERMINALES = List.of(
            EstadoExpediente.Finalizado, EstadoExpediente.Rechazado);

    public DashboardService(ExpedienteRepository expedienteRepo,
            ActividadPOIRepository actividadRepo,
            TechoPresupuestalRepository techoRepo,
            NecesidadPAPRepository necesidadRepo) {
        this.expedienteRepo = expedienteRepo;
        this.actividadRepo = actividadRepo;
        this.techoRepo = techoRepo;
        this.necesidadRepo = necesidadRepo;
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

    public Map<String, Object> getSaldos() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<TechoPresupuestal> techosActivos = techoRepo.findByActivoTrue();

        List<Map<String, Object>> techosList = new ArrayList<>();
        List<Map<String, Object>> actsList = new ArrayList<>();

        for (TechoPresupuestal techo : techosActivos) {
            Map<String, Object> tMap = new LinkedHashMap<>();
            tMap.put("año", techo.getAño());
            techosList.add(tMap);

            List<ActividadPOI> actividades = actividadRepo
                    .findByTechoPresupuestal_Id(techo.getId());

            for (ActividadPOI act : actividades) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", act.getId());
                item.put("codigo", act.getCodigo());
                item.put("nombre", act.getNombre());
                item.put("año", techo.getAño());
                item.put("asignado", act.getPresupuestoAsignado());
                item.put("comprometido", act.getSaldoComprometido());
                item.put("ejecutado", act.getSaldoEjecutado());

                BigDecimal disponible = act.getPresupuestoAsignado()
                        .subtract(act.getSaldoComprometido())
                        .subtract(act.getSaldoEjecutado());
                item.put("disponible", disponible);

                if (act.getPresupuestoAsignado().compareTo(BigDecimal.ZERO) > 0) {
                    item.put("pctEjecutado", act.getSaldoEjecutado()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(act.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP));
                    item.put("pctComprometido", act.getSaldoComprometido()
                            .multiply(BigDecimal.valueOf(100))
                            .divide(act.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP));
                    item.put("pctDisponible", disponible
                            .multiply(BigDecimal.valueOf(100))
                            .divide(act.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP));
                } else {
                    item.put("pctEjecutado", BigDecimal.ZERO);
                    item.put("pctComprometido", BigDecimal.ZERO);
                    item.put("pctDisponible", BigDecimal.ZERO);
                }

                actsList.add(item);
            }
        }

        result.put("techos", techosList);
        result.put("actividades", actsList);
        return result;
    }

    public Map<String, Object> getAlertas() {
        Map<String, Object> result = new LinkedHashMap<>();
        LocalDate hoy = LocalDate.now();
        int rojas = 0, amarillas = 0, verdes = 0;

        List<ActividadPOI> todas = actividadRepo.findAll();
        List<Map<String, Object>> actsAlerta = new ArrayList<>();

        for (ActividadPOI a : todas) {
            if (a.getFechaLimite() == null) continue;
            long diasRestantes = ChronoUnit.DAYS.between(hoy, a.getFechaLimite());
            String semaforo = diasRestantes < 0 ? "rojo" : (diasRestantes <= 7 ? "amarillo" : "verde");

            if ("rojo".equals(semaforo)) rojas++;
            else if ("amarillo".equals(semaforo)) amarillas++;
            else verdes++;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("codigo", a.getCodigo());
            item.put("nombre", a.getNombre());
            item.put("semaforo", semaforo);
            item.put("diasRestantes", (int) diasRestantes);
            item.put("pctEjecucion", a.getPorcentajeEjecucion() != null
                    ? a.getPorcentajeEjecucion().intValue() : 0);
            item.put("saldoDisponible", a.getPresupuestoAsignado()
                    .subtract(a.getSaldoComprometido())
                    .subtract(a.getSaldoEjecutado()));
            actsAlerta.add(item);
        }

        List<Expediente> todosExp = expedienteRepo.findByEstadoNotIn(ESTADOS_TERMINALES);
        List<Map<String, Object>> expsAlerta = new ArrayList<>();

        for (Expediente e : todosExp) {
            if (e.getCreatedAt() == null) continue;
            long diasSinMovimiento = ChronoUnit.DAYS.between(
                    e.getCreatedAt().toLocalDate(), hoy);
            if (diasSinMovimiento <= 7) continue;

            String semaforo = diasSinMovimiento > 14 ? "rojo" : "amarillo";
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", e.getId());
            item.put("codigo", e.getCodigo());
            item.put("estado", e.getEstado().name());
            item.put("urgencia", e.getUrgencia().name());
            item.put("descripcion", e.getDescripcion());
            item.put("diasSinMovimiento", (int) diasSinMovimiento);
            item.put("semaforo", semaforo);
            expsAlerta.add(item);
        }

        result.put("resumen", Map.of("rojas", rojas, "amarillas", amarillas, "verdes", verdes));
        result.put("actividades", actsAlerta);
        result.put("expedientes", expsAlerta);
        return result;
    }
}
