package com.upla.sisexp.service;

import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReporteService {

    private final ExpedienteRepository expedienteRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final TechoPresupuestalRepository techoRepo;

    public ReporteService(ExpedienteRepository expedienteRepo,
            ActividadPOIRepository actividadRepo,
            NecesidadPAPRepository necesidadRepo,
            TechoPresupuestalRepository techoRepo) {
        this.expedienteRepo = expedienteRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.techoRepo = techoRepo;
    }

    public List<Expediente> expedientes() {
        return expedienteRepo.findAll();
    }

    public List<Map<String, Object>> poiGeneral() {
        List<ActividadPOI> actividades = actividadRepo.findAll();
        return actividades.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", a.getCodigo());
            m.put("nombre", a.getNombre());
            m.put("techoAño", a.getTechoPresupuestal().getAño());
            m.put("presupuesto", a.getPresupuestoAsignado());
            m.put("saldoComprometido", a.getSaldoComprometido());
            m.put("saldoEjecutado", a.getSaldoEjecutado());
            BigDecimal disponible = a.getPresupuestoAsignado()
                    .subtract(a.getSaldoComprometido())
                    .subtract(a.getSaldoEjecutado());
            m.put("disponible", disponible);
            m.put("estado", a.getEstado().name());
            if (a.getPresupuestoAsignado().compareTo(BigDecimal.ZERO) > 0) {
                m.put("pctEjecucion", a.getSaldoEjecutado()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(a.getPresupuestoAsignado(), 1, RoundingMode.HALF_UP));
            } else {
                m.put("pctEjecucion", BigDecimal.ZERO);
            }
            return m;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> papGeneral() {
        List<NecesidadPAP> necesidades = necesidadRepo.findAll();
        return necesidades.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("nombre", n.getNombre());
            m.put("actividadCodigo", n.getActividadPOI().getCodigo());
            m.put("cantidad", n.getCantidad());
            m.put("precioEstimado", n.getPrecioEstimado());
            m.put("tipo", n.getTipo().name());
            m.put("cantidadDisponible", n.getCantidadDisponible());
            m.put("cantidadEjecutada", n.getCantidadEjecutada());
            m.put("montoDisponible", n.getMontoDisponible());
            m.put("montoEjecutado", n.getMontoEjecutado());
            m.put("unidad", n.getUnidad());
            return m;
        }).collect(Collectors.toList());
    }

    public List<TechoPresupuestal> informeAnual() {
        return techoRepo.findAll();
    }

    public Map<String, Long> getConteoPorEstado() {
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (EstadoExpediente e : EstadoExpediente.values()) {
            conteo.put(e.name(), expedienteRepo.countByEstado(e));
        }
        conteo.put("Vencidos", expedienteRepo.countByFechaLimiteBeforeAndEstadoNotIn(
                LocalDate.now(), List.of(EstadoExpediente.Finalizado, EstadoExpediente.Rechazado)));
        return conteo;
    }
}
