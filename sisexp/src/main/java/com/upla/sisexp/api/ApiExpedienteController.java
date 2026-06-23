package com.upla.sisexp.api;

import com.upla.sisexp.model.*;
import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.ExpedienteService;
import com.upla.sisexp.dto.CambiarEstadoDTO;
import com.upla.sisexp.dto.DocumentoDTO;
import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.TipoDocumento;
import com.upla.sisexp.enums.Urgencia;
import com.upla.sisexp.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/expedientes")
public class ApiExpedienteController {

    private final ExpedienteService expedienteService;

    public ApiExpedienteController(ExpedienteService expedienteService) {
        this.expedienteService = expedienteService;
    }

    @GetMapping
    public ResponseEntity<List<Expediente>> listar(
            @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(expedienteService.listar(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Expediente> detalle(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.obtenerConLogs(id));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<?> historial(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.getHistorial(id));
    }

    @GetMapping("/{id}/documentos")
    public ResponseEntity<?> documentos(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.getDocumentos(id));
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            com.upla.sisexp.dto.ExpedienteFormDTO dto = new com.upla.sisexp.dto.ExpedienteFormDTO();
            dto.setActividadPoiId(Long.valueOf(body.get("actividadPoiId").toString()));
            dto.setNecesidadPapId(Long.valueOf(body.get("necesidadPapId").toString()));
            dto.setUrgencia(Urgencia.valueOf((String) body.get("urgencia")));
            dto.setNaturaleza(body.get("naturaleza") != null
                    ? Naturaleza.valueOf((String) body.get("naturaleza")) : null);
            dto.setDescripcion((String) body.get("descripcion"));
            dto.setCantidadSolicitada(body.get("cantidadSolicitada") != null
                    ? Integer.valueOf(body.get("cantidadSolicitada").toString()) : 1);
            if (body.get("costoEstimado") != null) {
                dto.setCostoEstimado(new BigDecimal(body.get("costoEstimado").toString()));
            }
            if (body.get("fechaLimite") != null) {
                dto.setFechaLimite(LocalDate.parse((String) body.get("fechaLimite")));
            }
            Expediente exp = expedienteService.crear(dto, user);
            return ResponseEntity.ok(exp);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstadoPost(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return _actualizarEstado(id, body, user);
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstadoPut(@PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal CustomUserDetails user) {
        return _actualizarEstado(id, body, user);
    }

    private ResponseEntity<?> _actualizarEstado(Long id, Map<String, String> body,
            CustomUserDetails user) {
        try {
            CambiarEstadoDTO dto = new CambiarEstadoDTO();
            String estadoStr = body.get("estado");
            if (estadoStr == null || estadoStr.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "El campo 'estado' es obligatorio"));
            }
            dto.setNuevoEstado(EstadoExpediente.valueOf(estadoStr.replace(' ', '_')));
            dto.setObservacion(body.get("observacion"));
            Expediente exp = expedienteService.actualizarEstado(id, dto, user);
            return ResponseEntity.ok(exp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Estado invalido: " + body.get("estado")));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/documentos")
    public ResponseEntity<?> subirDocumento(@PathVariable Long id,
            @RequestParam TipoDocumento tipo,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            DocumentoDTO dto = new DocumentoDTO();
            dto.setTipo(tipo);
            dto.setArchivo(archivo);
            DocumentoAdjunto doc = expedienteService.subirDocumento(id, dto);
            return ResponseEntity.ok(doc);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/documentos/{documentoId}")
    public ResponseEntity<?> eliminarDocumento(@PathVariable Long documentoId) {
        try {
            expedienteService.eliminarDocumento(documentoId);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/actividades")
    public ResponseEntity<?> getActividades() {
        return ResponseEntity.ok(expedienteService.getActividadesActivas());
    }

    @GetMapping("/necesidades/por-actividad/{actividadId}")
    public ResponseEntity<?> necesidadesPorActividad(@PathVariable Long actividadId) {
        try {
            return ResponseEntity.ok(expedienteService.getNecesidadesPorActividad(actividadId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/disponibilidad/{actividadId}/{necesidadId}")
    public ResponseEntity<?> disponibilidad(@PathVariable Long actividadId,
            @PathVariable Long necesidadId) {
        try {
            return ResponseEntity.ok(expedienteService.getDisponibilidad(actividadId, necesidadId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
