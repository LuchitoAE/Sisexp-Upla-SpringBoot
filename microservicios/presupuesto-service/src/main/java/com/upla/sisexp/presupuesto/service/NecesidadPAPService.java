package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class NecesidadPAPService {
    private final NecesidadPAPRepository necesidadRepo;
    public NecesidadPAPService(NecesidadPAPRepository necesidadRepo) { this.necesidadRepo = necesidadRepo; }

    public List<NecesidadPAP> listar() { return necesidadRepo.findAll(); }
    public List<NecesidadPAP> listarPorActividad(Long actividadId) { return necesidadRepo.findByActividadPOIId(actividadId); }
    public NecesidadPAP obtener(Long id) { return necesidadRepo.findById(id).orElseThrow(() -> new BusinessException("Necesidad no encontrada")); }

    @Transactional
    public NecesidadPAP crear(String nombre, int cantidad, BigDecimal precioEstimado, String unidad,
            String oficina, Naturaleza tipo, String clasificador, Long actividadPoiId) {
        NecesidadPAP n = new NecesidadPAP();
        n.setNombre(nombre); n.setCantidad(cantidad); n.setPrecioEstimado(precioEstimado);
        n.setUnidad(unidad); n.setOficinaLaboratorio(oficina); n.setTipo(tipo);
        n.setClasificadorGasto(clasificador);
        BigDecimal costoTotal = precioEstimado.multiply(BigDecimal.valueOf(cantidad));
        n.setCantidadDisponible(cantidad); n.setMontoDisponible(costoTotal);
        n.setActividadPOIId(actividadPoiId);
        return necesidadRepo.save(n);
    }

    @Transactional
    public void eliminar(Long id) { necesidadRepo.delete(obtener(id)); }

    @Transactional
    public NecesidadPAP editar(Long id, String nombre, Integer cantidad, BigDecimal precioEstimado,
            String unidad, String oficina, String clasificador) {
        NecesidadPAP n = obtener(id);
        if (nombre != null) n.setNombre(nombre);
        if (cantidad != null) { n.setCantidad(cantidad); n.setCantidadDisponible(cantidad); }
        if (precioEstimado != null) {
            n.setPrecioEstimado(precioEstimado);
            n.setMontoDisponible(precioEstimado.multiply(BigDecimal.valueOf(n.getCantidad())));
        }
        if (unidad != null) n.setUnidad(unidad);
        if (oficina != null) n.setOficinaLaboratorio(oficina);
        if (clasificador != null) n.setClasificadorGasto(clasificador);
        return necesidadRepo.save(n);
    }
}
