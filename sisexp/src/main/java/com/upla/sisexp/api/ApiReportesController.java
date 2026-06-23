package com.upla.sisexp.api;

import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes")
public class ApiReportesController {

    private final ExpedienteRepository expedienteRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final TechoPresupuestalRepository techoRepo;

    private static final List<EstadoExpediente> ESTADOS_TERMINALES = List.of(
            EstadoExpediente.Finalizado, EstadoExpediente.Rechazado);

    public ApiReportesController(ExpedienteRepository expedienteRepo,
            ActividadPOIRepository actividadRepo,
            NecesidadPAPRepository necesidadRepo,
            TechoPresupuestalRepository techoRepo) {
        this.expedienteRepo = expedienteRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.techoRepo = techoRepo;
    }

    @GetMapping("/anual/{año}")
    public ResponseEntity<?> reporteAnual(@PathVariable Integer año) {
        Map<String, Object> reporte = new LinkedHashMap<>();
        reporte.put("año", año);

        var techoOpt = techoRepo.findByAño(año);
        if (techoOpt.isPresent()) {
            TechoPresupuestal techo = techoOpt.get();
            reporte.put("techo", Map.of(
                    "id", techo.getId(),
                    "montoTotal", techo.getMontoTotal(),
                    "montoUtilizado", techo.getMontoUtilizado(),
                    "saldo", techo.getMontoTotal().subtract(techo.getMontoUtilizado()),
                    "activo", techo.getActivo(),
                    "planificado", techo.getPlanificado()
            ));
        }

        List<ActividadPOI> actividades = actividadRepo.findAll().stream()
                .filter(a -> a.getTechoPresupuestal() != null
                        && a.getTechoPresupuestal().getAño().equals(año))
                .collect(Collectors.toList());
        reporte.put("actividades", actividades);

        List<Expediente> expedientes = expedienteRepo.findAll().stream()
                .filter(e -> e.getActividadPOI() != null
                        && e.getActividadPOI().getTechoPresupuestal() != null
                        && e.getActividadPOI().getTechoPresupuestal().getAño().equals(año))
                .collect(Collectors.toList());
        reporte.put("totalExpedientes", expedientes.size());
        reporte.put("expedientesPorEstado", Arrays.stream(EstadoExpediente.values())
                .collect(Collectors.toMap(Enum::name,
                        e -> expedientes.stream().filter(x -> x.getEstado() == e).count())));

        BigDecimal totalCosto = expedientes.stream()
                .map(Expediente::getCostoEstimado)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        reporte.put("costoTotalEstimado", totalCosto);

        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/expedientes")
    public ResponseEntity<?> reporteExpedientes(@RequestParam(required = false) Integer anio) {
        List<Expediente> lista;
        if (anio != null) {
            lista = expedienteRepo.findAll().stream()
                    .filter(e -> e.getCreatedAt() != null
                            && e.getCreatedAt().getYear() == anio)
                    .collect(Collectors.toList());
        } else {
            lista = expedienteRepo.findAll();
        }

        List<Map<String, Object>> result = lista.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("codigo", e.getCodigo());
            m.put("urgencia", e.getUrgencia());
            m.put("estado", e.getEstado());
            m.put("descripcion", e.getDescripcion());
            m.put("costoEstimado", e.getCostoEstimado());
            m.put("fechaLimite", e.getFechaLimite());
            m.put("solicitante", e.getSolicitante() != null ? e.getSolicitante().getNombre() : null);
            m.put("actividad", e.getActividadPOI() != null ? e.getActividadPOI().getCodigo() : null);
            m.put("necesidad", e.getNecesidadPAP() != null ? e.getNecesidadPAP().getNombre() : null);
            m.put("naturaleza", e.getNaturaleza());
            m.put("createdAt", e.getCreatedAt());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/poi")
    public ResponseEntity<?> reportePOI(@RequestParam(required = false) Integer anio) {
        List<ActividadPOI> lista;
        if (anio != null) {
            lista = actividadRepo.findAll().stream()
                    .filter(a -> a.getTechoPresupuestal() != null
                            && a.getTechoPresupuestal().getAño().equals(anio))
                    .collect(Collectors.toList());
        } else {
            lista = actividadRepo.findAll();
        }

        List<Map<String, Object>> result = lista.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("codigo", a.getCodigo());
            m.put("nombre", a.getNombre());
            m.put("presupuestoAsignado", a.getPresupuestoAsignado());
            m.put("saldoComprometido", a.getSaldoComprometido());
            m.put("saldoEjecutado", a.getSaldoEjecutado());
            m.put("disponible", a.getPresupuestoAsignado()
                    .subtract(a.getSaldoComprometido()).subtract(a.getSaldoEjecutado()));
            m.put("estado", a.getEstado());
            m.put("fechaLimite", a.getFechaLimite());
            m.put("planificado", a.getPlanificado());
            m.put("techoAño", a.getTechoPresupuestal() != null ? a.getTechoPresupuestal().getAño() : null);
            m.put("totalExpedientes", expedienteRepo.findByActividadPOI_Id(a.getId()).size());
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/poi/{id}")
    public ResponseEntity<?> detallePOI(@PathVariable Long id) {
        var actividad = actividadRepo.findById(id);
        if (actividad.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Actividad no encontrada"));
        }
        ActividadPOI a = actividad.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", a.getId());
        result.put("codigo", a.getCodigo());
        result.put("nombre", a.getNombre());
        result.put("presupuestoAsignado", a.getPresupuestoAsignado());
        result.put("saldoComprometido", a.getSaldoComprometido());
        result.put("saldoEjecutado", a.getSaldoEjecutado());
        result.put("estado", a.getEstado());

        List<NecesidadPAP> necesidades = necesidadRepo.findByActividadPOI_Id(id);
        result.put("necesidades", necesidades);

        List<Expediente> expedientes = expedienteRepo.findByActividadPOI_Id(id);
        result.put("expedientes", expedientes);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/pap")
    public ResponseEntity<?> reportePAP(@RequestParam(required = false) Integer anio) {
        List<NecesidadPAP> lista;
        if (anio != null) {
            lista = necesidadRepo.findAll().stream()
                    .filter(n -> n.getActividadPOI() != null
                            && n.getActividadPOI().getTechoPresupuestal() != null
                            && n.getActividadPOI().getTechoPresupuestal().getAño().equals(anio))
                    .collect(Collectors.toList());
        } else {
            lista = necesidadRepo.findAll();
        }

        List<Map<String, Object>> result = lista.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("nombre", n.getNombre());
            m.put("cantidad", n.getCantidad());
            m.put("precioEstimado", n.getPrecioEstimado());
            m.put("montoTotal", n.getPrecioEstimado().multiply(BigDecimal.valueOf(n.getCantidad())));
            m.put("cantidadDisponible", n.getCantidadDisponible());
            m.put("montoDisponible", n.getMontoDisponible());
            m.put("tipo", n.getTipo());
            m.put("unidad", n.getUnidad());
            m.put("oficinaLaboratorio", n.getOficinaLaboratorio());
            m.put("actividadCodigo", n.getActividadPOI() != null ? n.getActividadPOI().getCodigo() : null);
            m.put("actividadNombre", n.getActividadPOI() != null ? n.getActividadPOI().getNombre() : null);
            return m;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    @GetMapping("/pap/{id}")
    public ResponseEntity<?> detallePAP(@PathVariable Long id) {
        var necesidadOpt = necesidadRepo.findById(id);
        if (necesidadOpt.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Necesidad no encontrada"));
        }
        NecesidadPAP n = necesidadOpt.get();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", n.getId());
        result.put("nombre", n.getNombre());
        result.put("cantidad", n.getCantidad());
        result.put("precioEstimado", n.getPrecioEstimado());
        result.put("cantidadDisponible", n.getCantidadDisponible());
        result.put("montoDisponible", n.getMontoDisponible());
        result.put("tipo", n.getTipo());
        result.put("unidad", n.getUnidad());

        List<Expediente> expedientes = expedienteRepo.findByNecesidadPAP_Id(id);
        result.put("expedientes", expedientes);

        if (n.getActividadPOI() != null) {
            result.put("actividad", Map.of(
                    "id", n.getActividadPOI().getId(),
                    "codigo", n.getActividadPOI().getCodigo(),
                    "nombre", n.getActividadPOI().getNombre()
            ));
        }

        return ResponseEntity.ok(result);
    }
}
