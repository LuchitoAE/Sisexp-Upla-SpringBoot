package com.upla.sisexp.api;

import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.TechoPresupuestalService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/techos-presupuestales")
public class ApiTechoPresupuestalController {

    private final TechoPresupuestalService techoService;

    public ApiTechoPresupuestalController(TechoPresupuestalService techoService) {
        this.techoService = techoService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(techoService.listar());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            Integer año = Integer.valueOf(body.get("año").toString());
            java.math.BigDecimal montoTotal = new java.math.BigDecimal(body.get("montoTotal").toString());
            var t = techoService.crear(año, montoTotal, user.getId());
            return ResponseEntity.ok(t);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return techoService.listar().stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> editar(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            var techoExistente = techoService.listar().stream()
                    .filter(t -> t.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new BusinessException("Techo no encontrado"));
            Integer año = body.containsKey("año") && body.get("año") != null
                    ? Integer.valueOf(body.get("año").toString()) : techoExistente.getAño();
            java.math.BigDecimal montoTotal = body.containsKey("montoTotal") && body.get("montoTotal") != null
                    ? new java.math.BigDecimal(body.get("montoTotal").toString()) : techoExistente.getMontoTotal();
            var t = techoService.editar(id, año, montoTotal);
            return ResponseEntity.ok(t);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/toggle-activo")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            techoService.toggleActivo(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/finalizar-poi")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> finalizarPoi(@PathVariable Long id) {
        try {
            techoService.togglePlanificado(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/desbloquear-poi")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> desbloquearPoi(@PathVariable Long id) {
        try {
            techoService.togglePlanificado(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
