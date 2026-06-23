package com.upla.sisexp.api;

import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.service.NecesidadPAPService;
import com.upla.sisexp.service.ActividadPOIService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/necesidades-pap")
public class ApiNecesidadPAPController {

    private final NecesidadPAPService necesidadService;
    private final ActividadPOIService actividadService;

    public ApiNecesidadPAPController(NecesidadPAPService necesidadService,
            ActividadPOIService actividadService) {
        this.necesidadService = necesidadService;
        this.actividadService = actividadService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long actividadId) {
        if (actividadId != null) {
            return ResponseEntity.ok(necesidadService.listarPorActividad(actividadId));
        }
        return ResponseEntity.ok(necesidadService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return necesidadService.listar().stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/actividad/{actividadId}")
    public ResponseEntity<?> listarPorActividad(@PathVariable Long actividadId) {
        return ResponseEntity.ok(necesidadService.listarPorActividad(actividadId));
    }

    @PostMapping("/actividad/{actividadId}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> crearEnActividad(@PathVariable Long actividadId,
            @RequestBody Map<String, Object> body) {
        try {
            var n = necesidadService.crear(
                    (String) body.get("nombre"),
                    Integer.valueOf(body.get("cantidad").toString()),
                    new BigDecimal(body.get("precioEstimado").toString()),
                    (String) body.get("unidad"),
                    (String) body.get("oficinaLaboratorio"),
                    Naturaleza.valueOf((String) body.get("tipo")),
                    (String) body.get("clasificadorGasto"),
                    actividadId);
            return ResponseEntity.ok(n);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            Long actividadPoiId = Long.valueOf(body.get("actividadPoiId").toString());
            var n = necesidadService.crear(
                    (String) body.get("nombre"),
                    Integer.valueOf(body.get("cantidad").toString()),
                    new BigDecimal(body.get("precioEstimado").toString()),
                    (String) body.get("unidad"),
                    (String) body.get("oficinaLaboratorio"),
                    Naturaleza.valueOf((String) body.get("tipo")),
                    (String) body.get("clasificadorGasto"),
                    actividadPoiId);
            return ResponseEntity.ok(n);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> editar(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            var n = necesidadService.editar(id,
                    (String) body.get("nombre"),
                    Integer.valueOf(body.get("cantidad").toString()),
                    new BigDecimal(body.get("precioEstimado").toString()),
                    (String) body.get("unidad"),
                    (String) body.get("oficinaLaboratorio"),
                    Naturaleza.valueOf((String) body.get("tipo")),
                    (String) body.get("clasificadorGasto"));
            return ResponseEntity.ok(n);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            necesidadService.eliminar(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
