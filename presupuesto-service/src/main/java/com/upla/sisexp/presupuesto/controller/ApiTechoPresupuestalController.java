package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.service.TechoPresupuestalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/techos-presupuestales")
public class ApiTechoPresupuestalController {
    private final TechoPresupuestalService techoService;
    public ApiTechoPresupuestalController(TechoPresupuestalService techoService) { this.techoService = techoService; }

    @GetMapping
    public ResponseEntity<?> listar() { return ResponseEntity.ok(techoService.listar()); }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try { return ResponseEntity.ok(techoService.obtener(id)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            var t = techoService.crear(
                Integer.valueOf(body.get("año").toString()),
                new BigDecimal(body.get("montoTotal").toString()),
                body.get("creadoPorId") != null ? Long.valueOf(body.get("creadoPorId").toString()) : null
            );
            return ResponseEntity.ok(t);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            var t = techoService.editar(id,
                body.get("año") != null ? Integer.valueOf(body.get("año").toString()) : null,
                body.get("montoTotal") != null ? new BigDecimal(body.get("montoTotal").toString()) : null
            );
            return ResponseEntity.ok(t);
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try { techoService.toggleActivo(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }
}
