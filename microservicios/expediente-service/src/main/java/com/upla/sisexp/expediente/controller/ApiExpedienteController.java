package com.upla.sisexp.expediente.controller;

import com.upla.sisexp.common.enums.TipoDocumento;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.expediente.service.ExpedienteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/expedientes")
public class ApiExpedienteController {
    private final ExpedienteService expedienteService;
    private final RestTemplate restTemplate;

    @Value("${presupuesto.service.url}")
    private String presupuestoUrl;

    public ApiExpedienteController(ExpedienteService expedienteService, RestTemplate restTemplate) {
        this.expedienteService = expedienteService;
        this.restTemplate = restTemplate;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long solicitanteId) {
        if (solicitanteId != null) return ResponseEntity.ok(expedienteService.listarPorSolicitante(solicitanteId));
        return ResponseEntity.ok(expedienteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try { return ResponseEntity.ok(expedienteService.obtenerConLogs(id)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/rastreo/{codigo}")
    public ResponseEntity<?> rastrear(@PathVariable String codigo) {
        try { return ResponseEntity.ok(expedienteService.obtenerConLogs(expedienteService.listar().stream().filter(e -> codigo.equals(e.getCodigo())).findFirst().orElseThrow(() -> new BusinessException("No encontrado")).getId())); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/disponibilidad/{actividadId}/{necesidadId}")
    public ResponseEntity<?> disponibilidad(@PathVariable Long actividadId,
            @PathVariable Long necesidadId,
            @RequestParam(defaultValue = "1") int cantidadSolicitada) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> poi = restTemplate.getForObject(
                presupuestoUrl + "/api/actividades-poi/" + actividadId, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> pap = restTemplate.getForObject(
                presupuestoUrl + "/api/necesidades-pap/" + necesidadId, Map.class);

            Map<String, Object> result = new LinkedHashMap<>();

            Map<String, Object> necesidad = new LinkedHashMap<>();
            necesidad.put("nombre", pap.get("nombre"));
            necesidad.put("tipo", pap.get("tipo"));
            necesidad.put("clasificadorGasto", pap.get("clasificadorGasto"));

            Map<String, Object> papData = new LinkedHashMap<>();
            papData.put("precioUnitario", pap.get("precioEstimado"));
            papData.put("unidad", pap.get("unidad"));
            papData.put("cantidadPlanificada", pap.get("cantidadPlanificada"));
            papData.put("cantidadDisponible", pap.get("cantidadDisponible"));
            papData.put("cantidadEjecutada", pap.get("cantidadEjecutada"));

            Number precio = (Number) pap.get("precioEstimado");
            BigDecimal costo = precio != null
                ? BigDecimal.valueOf(precio.doubleValue()).multiply(BigDecimal.valueOf(cantidadSolicitada))
                : BigDecimal.ZERO;

            Object poiFechaLimite = poi.get("fechaLimite");
            boolean fechaOk = true;
            String fechaError = null;
            if (poiFechaLimite != null) {
                LocalDate fl = LocalDate.parse(poiFechaLimite.toString().substring(0, 10));
                if (fl.isBefore(LocalDate.now())) {
                    fechaOk = false;
                    fechaError = "La fecha limite del POI ya vencio: " + fl;
                }
            }

            Map<String, Object> fechaLimiteData = new LinkedHashMap<>();
            fechaLimiteData.put("ok", fechaOk);
            fechaLimiteData.put("error", fechaError);
            fechaLimiteData.put("fecha", poiFechaLimite != null ? poiFechaLimite.toString().substring(0, 10) : null);

            Number asignado = (Number) poi.get("presupuestoAsignado");
            Number ejecutado = (Number) poi.get("saldoEjecutado");
            Number comprometido = (Number) poi.get("saldoComprometido");
            BigDecimal asign = asignado != null ? BigDecimal.valueOf(asignado.doubleValue()) : BigDecimal.ZERO;
            BigDecimal ejec = ejecutado != null ? BigDecimal.valueOf(ejecutado.doubleValue()) : BigDecimal.ZERO;
            BigDecimal comp = comprometido != null ? BigDecimal.valueOf(comprometido.doubleValue()) : BigDecimal.ZERO;
            BigDecimal disponible = asign.subtract(ejec).subtract(comp).subtract(costo);

            Map<String, Object> saldoData = new LinkedHashMap<>();
            saldoData.put("ok", disponible.compareTo(BigDecimal.ZERO) >= 0);
            saldoData.put("disponible", disponible);
            saldoData.put("asignado", asign);
            saldoData.put("ejecutado", ejec);
            saldoData.put("comprometido", comp);
            if (disponible.compareTo(BigDecimal.ZERO) < 0) {
                saldoData.put("error", "Saldo insuficiente. Faltan " + disponible.abs());
            }

            result.put("necesidad", necesidad);
            result.put("pap", papData);
            result.put("fechaLimite", fechaLimiteData);
            result.put("saldo", saldoData);
            result.put("costo", costo);
            result.put("actividadPoiId", actividadId);
            result.put("necesidadPapId", necesidadId);
            result.put("cantidadSolicitada", cantidadSolicitada);

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("disponible", false);
            fallback.put("error", e.getMessage());
            fallback.put("necesidad", Map.of());
            fallback.put("pap", Map.of("precioUnitario", 0, "unidad", "", "cantidadPlanificada", 0, "cantidadDisponible", 999, "cantidadEjecutada", 0));
            fallback.put("fechaLimite", Map.of("ok", true, "error", null, "fecha", null));
            fallback.put("saldo", Map.of("ok", true, "disponible", BigDecimal.ZERO, "asignado", BigDecimal.ZERO, "ejecutado", BigDecimal.ZERO, "comprometido", BigDecimal.ZERO));
            fallback.put("costo", BigDecimal.ZERO);
            fallback.put("actividadPoiId", actividadId);
            fallback.put("necesidadPapId", necesidadId);
            fallback.put("cantidadSolicitada", cantidadSolicitada);
            fallback.put("fallback", true);
            return ResponseEntity.ok(fallback);
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            String actKey = body.containsKey("actividadPoiId") ? "actividadPoiId" : "actividadPOIId";
            String necKey = body.containsKey("necesidadPapId") ? "necesidadPapId" : "necesidadPAPId";
            if (body.get(actKey) == null) return ResponseEntity.badRequest().body(Map.of("error", "actividadPoiId requerido"));
            if (body.get(necKey) == null) return ResponseEntity.badRequest().body(Map.of("error", "necesidadPapId requerido"));
            if (body.get("solicitanteId") == null) return ResponseEntity.badRequest().body(Map.of("error", "solicitanteId requerido"));
            var exp = expedienteService.crear(
                Long.valueOf(body.get(actKey).toString()),
                Long.valueOf(body.get(necKey).toString()),
                Long.valueOf(body.get("solicitanteId").toString()),
                (String) body.get("urgencia"),
                (String) body.get("naturaleza"),
                (String) body.get("descripcion"),
                body.get("cantidadSolicitada") != null ? Integer.parseInt(body.get("cantidadSolicitada").toString()) : 1,
                body.get("costoEstimado") != null ? new BigDecimal(body.get("costoEstimado").toString()) : BigDecimal.ZERO
            );
            return ResponseEntity.status(201).body(exp);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> cambiarEstado(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            String estadoStr = body.get("estado");
            if (estadoStr == null || estadoStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'estado' es obligatorio"));
            }
            var exp = expedienteService.actualizarEstado(id, estadoStr, body.get("observacion"),
                body.get("usuarioId") != null ? Long.valueOf(body.get("usuarioId")) : null);
            return ResponseEntity.ok(exp);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/documentos")
    public ResponseEntity<?> subirDocumento(@PathVariable Long id,
            @RequestParam(name = "tipo") TipoDocumento tipo,
            @RequestParam(name = "archivo") MultipartFile archivo) {
        try {
            var doc = expedienteService.subirDocumento(id, tipo, archivo);
            return ResponseEntity.ok(doc);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
