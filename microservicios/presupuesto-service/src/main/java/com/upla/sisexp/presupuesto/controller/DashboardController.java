package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final TechoPresupuestalRepository techoRepo;
    private final ActividadPOIRepository actividadRepo;
    private final RestTemplate restTemplate;

    public DashboardController(TechoPresupuestalRepository techoRepo,
                                ActividadPOIRepository actividadRepo,
                                RestTemplate restTemplate) {
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.restTemplate = restTemplate;
    }

    @GetMapping("/alertas")
    public Map<String, Object> alertas() {
        List<ActividadPOI> actividades = actividadRepo.findAll();
        List<Map<String, Object>> actsAlert = new ArrayList<>();
        int rojas = 0, amarillas = 0, verdes = 0;
        LocalDate hoy = LocalDate.now();

        for (ActividadPOI a : actividades) {
            long diasRestantes = a.getFechaLimite() != null
                ? ChronoUnit.DAYS.between(hoy, a.getFechaLimite()) : 365;
            BigDecimal asignado = a.getPresupuestoAsignado();
            BigDecimal ejecutado = a.getSaldoEjecutado();
            BigDecimal disponible = asignado.subtract(ejecutado).subtract(a.getSaldoComprometido());
            BigDecimal pct = asignado.compareTo(BigDecimal.ZERO) > 0
                ? ejecutado.divide(asignado, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO;
            int pctEj = pct.intValue();
            String semaforo;
            if (diasRestantes < 0 || pctEj < 30) { semaforo = "rojo"; rojas++; }
            else if (diasRestantes < 30 || pctEj < 60) { semaforo = "amarillo"; amarillas++; }
            else { semaforo = "verde"; verdes++; }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("codigo", a.getCodigo());
            item.put("nombre", a.getNombre());
            item.put("semaforo", semaforo);
            item.put("diasRestantes", (int) diasRestantes);
            item.put("pctEjecucion", pctEj);
            item.put("saldoDisponible", disponible);
            actsAlert.add(item);
        }

        List<Map<String, Object>> expsAlert = new ArrayList<>();
        try {
            List<Map<String, Object>> expedientes = restTemplate.exchange(
                "http://expediente-service:8083/api/expedientes",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (expedientes != null) {
                for (Map<String, Object> e : expedientes) {
                    String estado = (String) e.get("estado");
                    String createdAtStr = (String) e.get("createdAt");
                    if (createdAtStr == null) continue;
                    try {
                        LocalDate created = LocalDate.parse(createdAtStr.substring(0, 10));
                        long dias = ChronoUnit.DAYS.between(created, hoy);
                        if (dias > 7 && ("Borrador".equals(estado) || "En_revision".equals(estado))) {
                            Map<String, Object> item = new LinkedHashMap<>();
                            item.put("id", e.get("id"));
                            item.put("codigo", e.get("codigo"));
                            item.put("estado", estado);
                            item.put("urgencia", e.get("urgencia"));
                            item.put("descripcion", e.get("descripcion"));
                            item.put("diasSinMovimiento", (int) dias);
                            item.put("semaforo", dias > 30 ? "rojo" : "amarillo");
                            expsAlert.add(item);
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}

        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("rojas", rojas);
        resumen.put("amarillas", amarillas);
        resumen.put("verdes", verdes);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("resumen", resumen);
        result.put("actividades", actsAlert);
        result.put("expedientes", expsAlert);
        return result;
    }

    @GetMapping("/saldos")
    public Map<String, Object> saldos() {
        List<TechoPresupuestal> techos = techoRepo.findAll();
        List<Map<String, Object>> techosList = new ArrayList<>();
        for (TechoPresupuestal t : techos) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("id", t.getId());
            tm.put("año", t.getAño());
            tm.put("montoTotal", t.getMontoTotal());
            tm.put("montoUtilizado", t.getMontoUtilizado());
            tm.put("saldo", t.getMontoTotal().subtract(t.getMontoUtilizado()));
            techosList.add(tm);
        }

        List<ActividadPOI> actividades = actividadRepo.findAll();
        List<Map<String, Object>> actsList = new ArrayList<>();
        for (ActividadPOI a : actividades) {
            BigDecimal asignado = a.getPresupuestoAsignado();
            BigDecimal ejecutado = a.getSaldoEjecutado();
            BigDecimal comprometido = a.getSaldoComprometido();
            BigDecimal disponible = asignado.subtract(ejecutado).subtract(comprometido);

            Integer anio = null;
            Long techoId = a.getTechoPresupuestalId();
            if (techoId != null) {
                anio = techoRepo.findById(techoId).map(TechoPresupuestal::getAño).orElse(null);
            }

            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId());
            am.put("codigo", a.getCodigo());
            am.put("nombre", a.getNombre());
            am.put("año", anio);
            am.put("estado", a.getEstado().name());
            am.put("asignado", asignado);
            am.put("ejecutado", ejecutado);
            am.put("comprometido", comprometido);
            am.put("disponible", disponible);
            actsList.add(am);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("techos", techosList);
        result.put("actividades", actsList);
        return result;
    }
}
