package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.*;
import com.upla.sisexp.presupuesto.service.ExportService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final TechoPresupuestalRepository techoRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final RestTemplate restTemplate;
    private final ExportService exportService;

    public ReportesController(TechoPresupuestalRepository techoRepo,
                               ActividadPOIRepository actividadRepo,
                               NecesidadPAPRepository necesidadRepo,
                               RestTemplate restTemplate,
                               ExportService exportService) {
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.restTemplate = restTemplate;
        this.exportService = exportService;
    }

    @GetMapping("/anual/{anio}")
    public Map<String, Object> informeAnual(@PathVariable int anio) {
        Map<String, Object> result = new LinkedHashMap<>();

        TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
        Map<String, Object> techoMap = new LinkedHashMap<>();
        if (techo != null) {
            techoMap.put("id", techo.getId());
            techoMap.put("año", techo.getAño());
            techoMap.put("montoTotal", techo.getMontoTotal());
            techoMap.put("montoUtilizado", techo.getMontoUtilizado());
            techoMap.put("saldo", techo.getMontoTotal().subtract(techo.getMontoUtilizado()));
        }
        result.put("techo", techoMap);

        List<ActividadPOI> actividades = techo != null
            ? actividadRepo.findByTechoPresupuestalId(techo.getId())
            : List.of();
        List<Map<String, Object>> actsList = new ArrayList<>();
        for (ActividadPOI a : actividades) {
            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId());
            am.put("codigo", a.getCodigo());
            am.put("nombre", a.getNombre());
            am.put("estado", a.getEstado().name());
            am.put("presupuestoAsignado", a.getPresupuestoAsignado());
            am.put("saldoEjecutado", a.getSaldoEjecutado());
            am.put("saldoComprometido", a.getSaldoComprometido());
            am.put("disponible", a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido()));
            am.put("fechaLimite", a.getFechaLimite() != null ? a.getFechaLimite().toString() : null);
            am.put("planificado", a.getPlanificado());
            actsList.add(am);
        }
        result.put("actividades", actsList);

        int totalExp = 0;
        double totalCosto = 0;
        try {
            List<Map<String, Object>> expedientes = restTemplate.exchange(
                "http://expediente-service:8083/api/expedientes",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (expedientes != null) {
                for (Map<String, Object> e : expedientes) {
                    String codigo = (String) e.get("codigo");
                    if (codigo != null && codigo.contains("-" + anio + "-")) {
                        totalExp++;
                        Object costo = e.get("costoEstimado");
                        if (costo != null) totalCosto += ((Number) costo).doubleValue();
                    }
                }
            }
        } catch (Exception ignored) {}
        result.put("totalExpedientes", totalExp);
        result.put("costoTotalEstimado", totalCosto);

        return result;
    }

    @GetMapping("/expedientes")
    public Map<String, Object> reporteExpedientes(@RequestParam(defaultValue = "2026") int anio) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> all = restTemplate.exchange(
                "http://expediente-service:8083/api/expedientes",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();

            List<Map<String, Object>> filtered = new ArrayList<>();
            Map<String, Integer> porEstado = new LinkedHashMap<>();
            Map<String, Integer> porUrgencia = new LinkedHashMap<>();
            double totalCosto = 0;
            int conDocs = 0, sinDocs = 0;

            if (all != null) {
                for (Map<String, Object> e : all) {
                    String codigo = (String) e.get("codigo");
                    if (codigo == null || !codigo.contains("-" + anio + "-")) continue;
                    filtered.add(e);
                    String estado = (String) e.getOrDefault("estado", "—");
                    porEstado.merge(estado, 1, Integer::sum);
                    String urgencia = (String) e.getOrDefault("urgencia", "—");
                    porUrgencia.merge(urgencia, 1, Integer::sum);
                    Object costo = e.get("costoEstimado");
                    if (costo != null) totalCosto += ((Number) costo).doubleValue();
                    Object docs = e.get("documentos");
                    if (docs instanceof List && !((List<?>) docs).isEmpty()) conDocs++;
                    else sinDocs++;
                }
            }

            result.put("total", filtered.size());
            result.put("totalCosto", totalCosto);
            result.put("conDocumentos", conDocs);
            result.put("sinDocumentos", sinDocs);
            result.put("porEstado", porEstado);
            result.put("porUrgencia", porUrgencia);
            result.put("listado", filtered);
        } catch (Exception e) {
            result.put("total", 0);
            result.put("listado", List.of());
        }
        return result;
    }

    @GetMapping("/poi")
    public Map<String, Object> reportePOI(@RequestParam(defaultValue = "2026") int anio) {
        TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
        List<ActividadPOI> actividades = techo != null
            ? actividadRepo.findByTechoPresupuestalId(techo.getId())
            : List.of();

        BigDecimal totalPresupuesto = BigDecimal.ZERO;
        BigDecimal totalDisponible = BigDecimal.ZERO;

        List<Map<String, Object>> actsList = new ArrayList<>();
        for (ActividadPOI a : actividades) {
            BigDecimal asignado = a.getPresupuestoAsignado();
            BigDecimal disponible = asignado.subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
            totalPresupuesto = totalPresupuesto.add(asignado);
            totalDisponible = totalDisponible.add(disponible);

            Map<String, Object> am = new LinkedHashMap<>();
            am.put("id", a.getId());
            am.put("codigo", a.getCodigo());
            am.put("nombre", a.getNombre());
            am.put("estado", a.getEstado().name());
            am.put("presupuestoAsignado", asignado);
            am.put("saldoEjecutado", a.getSaldoEjecutado());
            am.put("saldoComprometido", a.getSaldoComprometido());
            am.put("disponible", disponible);
            am.put("planificado", a.getPlanificado());
            am.put("año", anio);
            actsList.add(am);
        }

        int pctEjecucion = totalPresupuesto.compareTo(BigDecimal.ZERO) > 0
            ? totalPresupuesto.subtract(totalDisponible)
                .divide(totalPresupuesto, 2, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)).intValue()
            : 0;

        Map<String, Object> presupuesto = new LinkedHashMap<>();
        presupuesto.put("total", totalPresupuesto);
        presupuesto.put("pctEjecucion", pctEjecucion);
        presupuesto.put("disponible", totalDisponible);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalActividades", actividades.size());
        result.put("presupuesto", presupuesto);
        result.put("actividades", actsList);
        return result;
    }

    @GetMapping("/poi/{id}")
    public Map<String, Object> detallePOI(@PathVariable Long id) {
        ActividadPOI a = actividadRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Actividad no encontrada"));

        TechoPresupuestal techo = techoRepo.findById(a.getTechoPresupuestalId()).orElse(null);

        BigDecimal asignado = a.getPresupuestoAsignado();
        BigDecimal ejecutado = a.getSaldoEjecutado();
        BigDecimal disponible = asignado.subtract(ejecutado).subtract(a.getSaldoComprometido());
        int pct = asignado.compareTo(BigDecimal.ZERO) > 0
            ? ejecutado.divide(asignado, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue()
            : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", a.getId());
        result.put("codigo", a.getCodigo());
        result.put("nombre", a.getNombre());
        result.put("estado", a.getEstado().name());
        result.put("añoTecho", techo != null ? techo.getAño() : null);
        result.put("presupuestoAsignado", asignado);
        result.put("saldoEjecutado", ejecutado);
        result.put("saldoComprometido", a.getSaldoComprometido());
        result.put("disponible", disponible);
        result.put("pctEjecucion", pct);
        result.put("fechaLimite", a.getFechaLimite() != null ? a.getFechaLimite().toString() : null);
        result.put("planificado", a.getPlanificado());

        List<NecesidadPAP> necesidades = necesidadRepo.findByActividadPOIId(id);
        List<Map<String, Object>> necList = new ArrayList<>();
        for (NecesidadPAP n : necesidades) {
            Map<String, Object> nm = new LinkedHashMap<>();
            nm.put("id", n.getId());
            nm.put("nombre", n.getNombre());
            nm.put("tipo", n.getTipo().name());
            nm.put("cantidad", n.getCantidad());
            nm.put("cantidadDisponible", n.getCantidadDisponible());
            nm.put("cantidadEjecutada", n.getCantidadEjecutada());
            nm.put("precioEstimado", n.getPrecioEstimado());
            nm.put("subtotal", n.getPrecioEstimado().multiply(BigDecimal.valueOf(n.getCantidad())));
            nm.put("montoDisponible", n.getMontoDisponible());
            nm.put("montoEjecutado", n.getMontoEjecutado());
            necList.add(nm);
        }
        result.put("necesidades", necList);

        List<Map<String, Object>> expsList = new ArrayList<>();
        try {
            List<Map<String, Object>> all = restTemplate.exchange(
                "http://expediente-service:8083/api/expedientes",
                HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            ).getBody();
            if (all != null) {
                for (Map<String, Object> e : all) {
                    Object actId = e.get("actividadPOIId");
                    if (actId != null && Long.valueOf(actId.toString()).equals(id)) {
                        Map<String, Object> em = new LinkedHashMap<>();
                        em.put("id", e.get("id"));
                        em.put("codigo", e.get("codigo"));
                        em.put("estado", e.get("estado"));
                        em.put("solicitante", "Usuario #" + e.get("solicitanteId"));
                        em.put("cantidadSolicitada", e.get("cantidadSolicitada"));
                        em.put("costoEstimado", e.get("costoEstimado"));
                        expsList.add(em);
                    }
                }
            }
        } catch (Exception ignored) {}
        result.put("expedientes", expsList);

        return result;
    }

    @GetMapping("/pap")
    public Map<String, Object> reportePAP(@RequestParam(defaultValue = "2026") int anio) {
        TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
        List<ActividadPOI> actividades = techo != null
            ? actividadRepo.findByTechoPresupuestalId(techo.getId())
            : List.of();

        List<NecesidadPAP> todas = new ArrayList<>();
        for (ActividadPOI a : actividades) {
            todas.addAll(necesidadRepo.findByActividadPOIId(a.getId()));
        }

        BigDecimal totalPlan = BigDecimal.ZERO;
        BigDecimal totalEjec = BigDecimal.ZERO;
        int cantPlan = 0, cantDisp = 0, cantEjec = 0;
        Map<String, Integer> porTipo = new LinkedHashMap<>();
        porTipo.put("Bien", 0);
        porTipo.put("Servicio", 0);

        List<Map<String, Object>> listado = new ArrayList<>();
        for (NecesidadPAP n : todas) {
            String tipoStr = n.getTipo().name();
            porTipo.merge(tipoStr, 1, Integer::sum);

            cantPlan += n.getCantidad();
            cantDisp += n.getCantidadDisponible() != null ? n.getCantidadDisponible() : 0;
            cantEjec += n.getCantidadEjecutada() != null ? n.getCantidadEjecutada() : 0;

            totalPlan = totalPlan.add(n.getPrecioEstimado().multiply(BigDecimal.valueOf(n.getCantidad())));
            totalEjec = totalEjec.add(n.getMontoEjecutado() != null ? n.getMontoEjecutado() : BigDecimal.ZERO);

            String actCodigo = "";
            for (ActividadPOI a : actividades) {
                if (a.getId().equals(n.getActividadPOIId())) {
                    actCodigo = a.getCodigo();
                    break;
                }
            }

            Map<String, Object> nm = new LinkedHashMap<>();
            nm.put("id", n.getId());
            nm.put("nombre", n.getNombre());
            nm.put("actividad", actCodigo);
            nm.put("tipo", tipoStr);
            nm.put("cantidad", n.getCantidad());
            nm.put("cantidadDisponible", n.getCantidadDisponible());
            nm.put("cantidadEjecutada", n.getCantidadEjecutada());
            nm.put("precioEstimado", n.getPrecioEstimado());
            listado.add(nm);
        }

        int pctEjecCant = cantPlan > 0 ? (cantEjec * 100 / cantPlan) : 0;
        int pctEjecMonto = totalPlan.compareTo(BigDecimal.ZERO) > 0
            ? totalEjec.divide(totalPlan, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue()
            : 0;

        Map<String, Object> cantidades = new LinkedHashMap<>();
        cantidades.put("planificado", cantPlan);
        cantidades.put("disponible", cantDisp);
        cantidades.put("ejecutado", cantEjec);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalItems", todas.size());
        result.put("pctEjecucionCantidad", pctEjecCant);
        result.put("pctEjecucionMonto", pctEjecMonto);
        result.put("porTipo", porTipo);
        result.put("cantidades", cantidades);
        result.put("listado", listado);
        return result;
    }

    @GetMapping("/pap/{id}")
    public Map<String, Object> detallePAP(@PathVariable Long id) {
        ActividadPOI a = actividadRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Actividad no encontrada"));

        TechoPresupuestal techo = techoRepo.findById(a.getTechoPresupuestalId()).orElse(null);
        BigDecimal asignado = a.getPresupuestoAsignado();
        BigDecimal ejecutado = a.getSaldoEjecutado();
        BigDecimal disponible = asignado.subtract(ejecutado).subtract(a.getSaldoComprometido());
        int pct = asignado.compareTo(BigDecimal.ZERO) > 0
            ? ejecutado.divide(asignado, 2, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).intValue()
            : 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", a.getId());
        result.put("codigo", a.getCodigo());
        result.put("nombre", a.getNombre());
        result.put("añoTecho", techo != null ? techo.getAño() : null);
        result.put("presupuestoAsignado", asignado);
        result.put("saldoEjecutado", ejecutado);
        result.put("disponible", disponible);
        result.put("pctEjecucion", pct);

        List<NecesidadPAP> necesidades = necesidadRepo.findByActividadPOIId(id);
        List<Map<String, Object>> necList = new ArrayList<>();
        for (NecesidadPAP n : necesidades) {
            Map<String, Object> nm = new LinkedHashMap<>();
            nm.put("id", n.getId());
            nm.put("nombre", n.getNombre());
            nm.put("tipo", n.getTipo().name());
            nm.put("cantidad", n.getCantidad());
            nm.put("cantidadDisponible", n.getCantidadDisponible());
            nm.put("cantidadEjecutada", n.getCantidadEjecutada());
            nm.put("precioEstimado", n.getPrecioEstimado());
            nm.put("subtotal", n.getPrecioEstimado().multiply(BigDecimal.valueOf(n.getCantidad())));
            nm.put("montoEjecutado", n.getMontoEjecutado());
            necList.add(nm);
        }
        result.put("necesidades", necList);

        return result;
    }

    // ==================== EXPORT ====================

    @GetMapping("/anual/{anio}/excel")
    public ResponseEntity<?> excelAnual(@PathVariable int anio) {
        try {
            byte[] data = exportService.exportarExcelAnual(anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe_anual_" + anio + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/anual/{anio}/pdf")
    public ResponseEntity<?> pdfAnual(@PathVariable int anio) {
        try {
            byte[] data = exportService.exportarPDF("anual", anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=informe_anual_" + anio + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/expedientes/excel")
    public ResponseEntity<?> excelExpedientes(@RequestParam(defaultValue = "2026") int anio) {
        try {
            byte[] data = exportService.exportarExcelExpedientes(anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_expedientes_" + anio + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/expedientes/pdf")
    public ResponseEntity<?> pdfExpedientes(@RequestParam(defaultValue = "2026") int anio) {
        try {
            byte[] data = exportService.exportarPDF("expedientes", anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_expedientes_" + anio + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/poi/excel")
    public ResponseEntity<?> excelPOI(@RequestParam(defaultValue = "2026") int anio) {
        try {
            byte[] data = exportService.exportarExcelPOI(anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_poi_" + anio + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/poi/pdf")
    public ResponseEntity<?> pdfPOI(@RequestParam(defaultValue = "2026") int anio) {
        try {
            byte[] data = exportService.exportarPDF("poi", anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_poi_" + anio + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando PDF: " + e.getMessage()));
        }
    }

    @GetMapping("/pap/excel")
    public ResponseEntity<?> excelPAP(@RequestParam(defaultValue = "2026") int anio) {
        try {
            byte[] data = exportService.exportarExcelPAP(anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_pap_" + anio + ".xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando Excel: " + e.getMessage()));
        }
    }

    @GetMapping("/pap/pdf")
    public ResponseEntity<?> pdfPAP(@RequestParam(defaultValue = "2026") int anio) {
        try {
            byte[] data = exportService.exportarPDF("pap", anio);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reporte_pap_" + anio + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(data);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Error generando PDF: " + e.getMessage()));
        }
    }
}
