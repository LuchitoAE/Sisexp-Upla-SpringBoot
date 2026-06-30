package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.service.NecesidadPAPService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/necesidades-pap")
public class ApiNecesidadPAPController {
    private final NecesidadPAPService necesidadService;
    public ApiNecesidadPAPController(NecesidadPAPService necesidadService) { this.necesidadService = necesidadService; }

    @GetMapping
    public ResponseEntity<?> listar() { return ResponseEntity.ok(necesidadService.listar()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try { return ResponseEntity.ok(necesidadService.obtener(id)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<?> listarPorActividad(@PathVariable Long actividadId) {
        return ResponseEntity.ok(necesidadService.listarPorActividad(actividadId));
    }

    @PostMapping("/actividad/{actividadId}")
    public ResponseEntity<?> crear(@PathVariable Long actividadId, @RequestBody Map<String, Object> body) {
        try {
            var pap = necesidadService.crear(
                (String) body.get("nombre"),
                Integer.parseInt(body.get("cantidad").toString()),
                new BigDecimal(body.get("precioEstimado").toString()),
                (String) body.get("unidad"),
                (String) body.get("oficinaLaboratorio"),
                body.get("tipo") != null ? Naturaleza.valueOf((String) body.get("tipo")) : Naturaleza.Bien,
                (String) body.get("clasificadorGasto"),
                actividadId
            );
            return ResponseEntity.ok(pap);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try { necesidadService.eliminar(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
