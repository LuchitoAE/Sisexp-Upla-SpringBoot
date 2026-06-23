package com.upla.sisexp.service;

import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.ActividadPOI;
import com.upla.sisexp.model.NecesidadPAP;
import com.upla.sisexp.repository.ActividadPOIRepository;
import com.upla.sisexp.repository.NecesidadPAPRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class NecesidadPAPService {

    private final NecesidadPAPRepository necesidadRepo;
    private final ActividadPOIRepository actividadRepo;

    public NecesidadPAPService(NecesidadPAPRepository necesidadRepo,
            ActividadPOIRepository actividadRepo) {
        this.necesidadRepo = necesidadRepo;
        this.actividadRepo = actividadRepo;
    }

    public List<NecesidadPAP> listar() {
        List<NecesidadPAP> list = necesidadRepo.findAll();
        list.forEach(n -> {
            if (n.getActividadPOI() != null) {
                n.getActividadPOI().getCodigo();
            }
        });
        return list;
    }

    public List<NecesidadPAP> listarPorActividad(Long actividadId) {
        return necesidadRepo.findByActividadPOI_Id(actividadId);
    }

    @Transactional
    public NecesidadPAP crear(String nombre, Integer cantidad, BigDecimal precioEstimado,
            String unidad, String oficinaLaboratorio, Naturaleza tipo,
            String clasificadorGasto, Long actividadPoiId) {
        ActividadPOI actividad = actividadRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));

        NecesidadPAP n = new NecesidadPAP();
        n.setNombre(nombre);
        n.setCantidad(cantidad);
        n.setPrecioEstimado(precioEstimado);
        n.setUnidad(unidad);
        n.setOficinaLaboratorio(oficinaLaboratorio);
        n.setTipo(tipo);
        n.setClasificadorGasto(clasificadorGasto);
        n.setActividadPOI(actividad);
        n.setCantidadDisponible(cantidad);
        n.setMontoDisponible(precioEstimado.multiply(BigDecimal.valueOf(cantidad)));
        return necesidadRepo.save(n);
    }

    @Transactional
    public NecesidadPAP editar(Long id, String nombre, Integer cantidad,
            BigDecimal precioEstimado, String unidad, String oficinaLaboratorio,
            Naturaleza tipo, String clasificadorGasto) {
        NecesidadPAP n = necesidadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        n.setNombre(nombre);
        n.setCantidad(cantidad);
        n.setPrecioEstimado(precioEstimado);
        n.setUnidad(unidad);
        n.setOficinaLaboratorio(oficinaLaboratorio);
        n.setTipo(tipo);
        n.setClasificadorGasto(clasificadorGasto);
        n.setMontoDisponible(precioEstimado.multiply(BigDecimal.valueOf(cantidad)));
        n.setCantidadDisponible(cantidad);
        return necesidadRepo.save(n);
    }

    @Transactional
    public void eliminar(Long id) {
        NecesidadPAP n = necesidadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        necesidadRepo.delete(n);
    }
}
