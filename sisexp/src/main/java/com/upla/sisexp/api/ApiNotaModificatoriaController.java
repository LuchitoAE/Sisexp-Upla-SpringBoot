package com.upla.sisexp.api;

import com.upla.sisexp.enums.EstadoNota;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.TipoNota;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.NotaModificatoriaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/notas-modificatorias")
public class ApiNotaModificatoriaController {

    private final NotaModificatoriaService notaService;

    public ApiNotaModificatoriaController(NotaModificatoriaService notaService) {
        this.notaService = notaService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(notaService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return notaService.listar().stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion','Laboratorio','Secretaria')")
    public ResponseEntity<?> crear(
            @RequestParam TipoNota tipo,
            @RequestParam(required = false) Long actividadExistenteId,
            @RequestParam(required = false) String origen,
            @RequestParam String nuevoNombre,
            @RequestParam String justificacion,
            @RequestParam(required = false) BigDecimal costoEstimadoReferencial,
            @RequestParam(required = false) MultipartFile archivo,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            var nota = notaService.crear(tipo, actividadExistenteId, origen, nuevoNombre,
                    justificacion, costoEstimadoReferencial, archivo, user.getId());
            return ResponseEntity.ok(nota);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/configurar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> configurar(@PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            var nota = notaService.configurar(id,
                    body.get("actividadOrigenId") != null
                            ? Long.valueOf(body.get("actividadOrigenId").toString()) : null,
                    body.get("montoTransferir") != null
                            ? new BigDecimal(body.get("montoTransferir").toString()) : BigDecimal.ZERO,
                    (String) body.get("nuevoClasificadorGasto"),
                    body.get("nuevoTipo") != null
                            ? Naturaleza.valueOf((String) body.get("nuevoTipo")) : null,
                    (String) body.get("observacion"),
                    user.getId());
            return ResponseEntity.ok(nota);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public ResponseEntity<?> rechazar(@PathVariable Long id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            var nota = notaService.rechazar(id,
                    (String) body.get("observacion"), user.getId());
            return ResponseEntity.ok(nota);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
