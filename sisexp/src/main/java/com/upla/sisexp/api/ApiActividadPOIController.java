package com.upla.sisexp.api;

import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.service.ActividadPOIService;
import com.upla.sisexp.service.TechoPresupuestalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/actividades-poi")
public class ApiActividadPOIController {

    private final ActividadPOIService actividadService;
    private final TechoPresupuestalService techoService;

    public ApiActividadPOIController(ActividadPOIService actividadService,
            TechoPresupuestalService techoService) {
        this.actividadService = actividadService;
        this.techoService = techoService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam(required = false) Long techoId) {
        if (techoId != null) {
            return ResponseEntity.ok(actividadService.listarPorTecho(techoId));
        }
        return ResponseEntity.ok(actividadService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(actividadService.obtener(id));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/techo/{techoId}")
    public ResponseEntity<?> listarPorTecho(@PathVariable Long techoId) {
        return ResponseEntity.ok(actividadService.listarPorTecho(techoId));
    }

    @PostMapping("/techo/{techoId}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> crearEnTecho(@PathVariable Long techoId,
            @RequestBody Map<String, Object> body) {
        try {
            var act = actividadService.crear(
                    (String) body.get("codigo"),
                    (String) body.get("nombre"),
                    new BigDecimal(body.get("presupuesto").toString()),
                    body.get("fechaLimite") != null ? LocalDate.parse((String) body.get("fechaLimite")) : null,
                    techoId);
            return ResponseEntity.ok(act);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            Long techoId = body.get("techoId") != null
                    ? Long.valueOf(body.get("techoId").toString()) : null;
            var act = actividadService.crear(
                    (String) body.get("codigo"),
                    (String) body.get("nombre"),
                    new BigDecimal(body.get("presupuesto").toString()),
                    body.get("fechaLimite") != null ? LocalDate.parse((String) body.get("fechaLimite")) : null,
                    techoId);
            return ResponseEntity.ok(act);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> editar(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            var act = actividadService.editar(id,
                    (String) body.get("codigo"),
                    (String) body.get("nombre"),
                    new BigDecimal(body.get("presupuesto").toString()),
                    body.get("fechaLimite") != null ? LocalDate.parse((String) body.get("fechaLimite")) : null);
            return ResponseEntity.ok(act);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        try {
            actividadService.eliminar(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/finalizar-pap")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> finalizarPAP(@PathVariable Long id) {
        try {
            actividadService.finalizarPAP(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/desbloquear-pap")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> desbloquearPAP(@PathVariable Long id) {
        try {
            actividadService.desbloquearPAP(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
