package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.service.ActividadPOIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/actividades-poi")
public class ApiActividadPOIController {
    private final ActividadPOIService actividadService;
    public ApiActividadPOIController(ActividadPOIService actividadService) { this.actividadService = actividadService; }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long techoId) {
        if (techoId != null) return ResponseEntity.ok(actividadService.listarPorTecho(techoId));
        return ResponseEntity.ok(actividadService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try { return ResponseEntity.ok(actividadService.obtener(id)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/techo/{techoId}")
    public ResponseEntity<?> listarPorTecho(@PathVariable Long techoId) {
        return ResponseEntity.ok(actividadService.listarPorTecho(techoId));
    }

    @PostMapping("/techo/{techoId}")
    public ResponseEntity<?> crearEnTecho(@PathVariable Long techoId, @RequestBody Map<String, Object> body) {
        try {
            var act = actividadService.crear(
                (String) body.get("codigo"), (String) body.get("nombre"),
                new BigDecimal(body.get("presupuestoAsignado").toString()),
                body.get("fechaLimite") != null ? LocalDate.parse((String) body.get("fechaLimite")) : null,
                techoId
            );
            return ResponseEntity.ok(act);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            var act = actividadService.editar(id,
                (String) body.get("codigo"), (String) body.get("nombre"),
                body.get("presupuestoAsignado") != null ? new BigDecimal(body.get("presupuestoAsignado").toString()) : null,
                body.get("fechaLimite") != null ? LocalDate.parse((String) body.get("fechaLimite")) : null
            );
            return ResponseEntity.ok(act);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try { actividadService.eliminar(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping("/{id}/finalizar-pap")
    public ResponseEntity<?> finalizarPAP(@PathVariable Long id) { return ResponseEntity.ok(Map.of("ok", true)); }

    @PostMapping("/{id}/desbloquear-pap")
    public ResponseEntity<?> desbloquearPAP(@PathVariable Long id) { return ResponseEntity.ok(Map.of("ok", true)); }
}
