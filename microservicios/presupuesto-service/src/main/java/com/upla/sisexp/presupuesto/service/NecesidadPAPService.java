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
}
