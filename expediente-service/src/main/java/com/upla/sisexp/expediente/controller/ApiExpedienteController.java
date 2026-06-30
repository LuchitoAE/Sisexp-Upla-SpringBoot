package com.upla.sisexp.expediente.controller;

import com.upla.sisexp.common.enums.TipoDocumento;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.expediente.service.ExpedienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/expedientes")
public class ApiExpedienteController {
    private final ExpedienteService expedienteService;
    public ApiExpedienteController(ExpedienteService expedienteService) { this.expedienteService = expedienteService; }

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

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            var exp = expedienteService.crear(
                Long.valueOf(body.get("actividadPoiId").toString()),
                Long.valueOf(body.get("necesidadPapId").toString()),
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
            var exp = expedienteService.actualizarEstado(id, body.get("estado"), body.get("observacion"),
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
