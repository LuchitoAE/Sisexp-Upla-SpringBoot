package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.common.enums.EstadoNota;
import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.common.enums.TipoNota;
import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NotaModificatoria;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.service.NotaModificatoriaService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/notas-modificatorias")
public class NotaModificatoriaController {

    private final NotaModificatoriaService service;
    private final ActividadPOIRepository actividadRepo;

    public NotaModificatoriaController(NotaModificatoriaService service, ActividadPOIRepository actividadRepo) {
        this.service = service;
        this.actividadRepo = actividadRepo;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (NotaModificatoria n : service.listarTodas()) {
            result.add(toMap(n));
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return service.buscarPorId(id)
            .map(this::toMap)
            .orElseThrow(() -> new RuntimeException("Nota no encontrada"));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> crear(@RequestParam String tipo,
                                      @RequestParam String nuevoNombre,
                                      @RequestParam String justificacion,
                                      @RequestParam(required = false) String origen,
                                      @RequestParam(required = false) Long actividadExistenteId,
                                      @RequestParam(defaultValue = "0") BigDecimal costoEstimadoReferencial,
                                      @RequestParam(required = false) MultipartFile archivo,
                                      @RequestParam(defaultValue = "1") Long solicitanteId) {
        NotaModificatoria nota = new NotaModificatoria();
        nota.setTipo(TipoNota.valueOf(tipo));
        nota.setNuevoNombre(nuevoNombre);
        nota.setJustificacion(justificacion);
        nota.setOrigen(origen);
        nota.setActividadExistenteId(actividadExistenteId);
        nota.setCostoEstimadoReferencial(costoEstimadoReferencial);
        nota.setSolicitanteId(solicitanteId);
        if (archivo != null && !archivo.isEmpty()) {
            try {
                nota.setArchivoAdjunto(archivo.getBytes());
                nota.setNombreArchivo(archivo.getOriginalFilename());
            } catch (IOException e) {
                throw new RuntimeException("Error al leer archivo");
            }
        }
        NotaModificatoria saved = service.crear(nota);
        return toMap(saved);
    }

    @PutMapping("/{id}/configurar")
    public Map<String, Object> configurar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long actividadOrigenId = body.get("actividadOrigenId") != null
            ? Long.valueOf(body.get("actividadOrigenId").toString()) : null;
        BigDecimal monto = new BigDecimal(body.get("montoTransferir").toString());
        String clasificador = (String) body.getOrDefault("nuevoClasificadorGasto", "2.3.1.x.x.x");
        String tipoStr = (String) body.getOrDefault("nuevoTipo", "Servicio");
        Naturaleza tipo = Naturaleza.valueOf(tipoStr);
        NotaModificatoria saved = service.configurar(id, actividadOrigenId, monto, clasificador, tipo);
        return toMap(saved);
    }

    @PutMapping("/{id}/rechazar")
    public Map<String, Object> rechazar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String observacion = (String) body.getOrDefault("observacion", "");
        NotaModificatoria saved = service.rechazar(id, observacion);
        return toMap(saved);
    }

    @GetMapping("/{id}/archivo")
    public ResponseEntity<byte[]> descargarArchivo(@PathVariable Long id) {
        NotaModificatoria nota = service.buscarPorId(id)
            .orElseThrow(() -> new RuntimeException("Nota no encontrada"));
        if (nota.getArchivoAdjunto() == null)
            return ResponseEntity.notFound().build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
            .filename(nota.getNombreArchivo() != null ? nota.getNombreArchivo() : "archivo.pdf").build());
        return new ResponseEntity<>(nota.getArchivoAdjunto(), headers, HttpStatus.OK);
    }

    private Map<String, Object> toMap(NotaModificatoria n) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", n.getId());
        m.put("codigo", n.getCodigo());
        m.put("tipo", n.getTipo() != null ? n.getTipo().name() : null);
        m.put("nuevoNombre", n.getNuevoNombre());
        m.put("justificacion", n.getJustificacion());
        m.put("costoEstimadoReferencial", n.getCostoEstimadoReferencial());
        m.put("origen", n.getOrigen());
        m.put("estado", n.getEstado() != null ? n.getEstado().name() : null);
        m.put("montoTransferir", n.getMontoTransferir());
        m.put("actividadExistenteId", n.getActividadExistenteId());
        m.put("actividadOrigenId", n.getActividadOrigenId());
        m.put("nuevoClasificadorGasto", n.getNuevoClasificadorGasto());
        m.put("nuevoTipo", n.getNuevoTipo() != null ? n.getNuevoTipo().name() : null);
        m.put("observacionAdmin", n.getObservacionAdmin());
        m.put("nombreArchivo", n.getNombreArchivo());
        m.put("solicitanteId", n.getSolicitanteId());
        m.put("createdAt", n.getCreatedAt());

        if (n.getActividadExistenteId() != null) {
            actividadRepo.findById(n.getActividadExistenteId()).ifPresent(a -> {
                Map<String, Object> ae = new LinkedHashMap<>();
                ae.put("id", a.getId());
                ae.put("codigo", a.getCodigo());
                m.put("actividadExistente", ae);
            });
        }
        if (n.getActividadOrigenId() != null) {
            actividadRepo.findById(n.getActividadOrigenId()).ifPresent(a -> {
                Map<String, Object> ao = new LinkedHashMap<>();
                ao.put("id", a.getId());
                ao.put("codigo", a.getCodigo());
                m.put("actividadOrigen", ao);
            });
        }
        return m;
    }
}
