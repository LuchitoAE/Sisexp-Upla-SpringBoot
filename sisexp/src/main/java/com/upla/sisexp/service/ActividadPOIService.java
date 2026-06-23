package com.upla.sisexp.service;

import com.upla.sisexp.enums.EstadoActividad;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.ActividadPOI;
import com.upla.sisexp.model.TechoPresupuestal;
import com.upla.sisexp.repository.ActividadPOIRepository;
import com.upla.sisexp.repository.TechoPresupuestalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ActividadPOIService {

    private final ActividadPOIRepository actividadRepo;
    private final TechoPresupuestalRepository techoRepo;

    public ActividadPOIService(ActividadPOIRepository actividadRepo,
            TechoPresupuestalRepository techoRepo) {
        this.actividadRepo = actividadRepo;
        this.techoRepo = techoRepo;
    }

    public List<ActividadPOI> listar() {
        List<ActividadPOI> list = actividadRepo.findAll();
        list.forEach(a -> {
            if (a.getTechoPresupuestal() != null) {
                a.getTechoPresupuestal().getAño();
            }
        });
        return list;
    }

    public List<ActividadPOI> listarPorTecho(Long techoId) {
        return actividadRepo.findByTechoPresupuestal_Id(techoId);
    }

    @Transactional
    public ActividadPOI crear(String codigo, String nombre, BigDecimal presupuesto,
            LocalDate fechaLimite, Long techoId) {
        if (techoId == null) {
            throw new BusinessException("Debe seleccionar un techo presupuestal");
        }
        TechoPresupuestal techo = techoRepo.findById(techoId)
                .orElseThrow(() -> new BusinessException("Techo no encontrado"));

        ActividadPOI a = new ActividadPOI();
        a.setCodigo(codigo);
        a.setNombre(nombre);
        a.setPresupuestoAsignado(presupuesto);
        a.setFechaLimite(fechaLimite);
        a.setTechoPresupuestal(techo);
        a.setEstado(EstadoActividad.Pendiente);
        return actividadRepo.save(a);
    }

    @Transactional
    public ActividadPOI editar(Long id, String codigo, String nombre,
            BigDecimal presupuesto, LocalDate fechaLimite) {
        ActividadPOI a = actividadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        a.setCodigo(codigo);
        a.setNombre(nombre);
        a.setPresupuestoAsignado(presupuesto);
        a.setFechaLimite(fechaLimite);
        return actividadRepo.save(a);
    }

    public ActividadPOI obtener(Long id) {
        return actividadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
    }

    @Transactional
    public void eliminar(Long id) {
        ActividadPOI a = actividadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        actividadRepo.delete(a);
    }

    @Transactional
    public void finalizarPAP(Long id) {
        ActividadPOI a = actividadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        a.setPlanificado(true);
        a.setEstado(EstadoActividad.Cerrado);
        actividadRepo.save(a);
    }

    @Transactional
    public void desbloquearPAP(Long id) {
        ActividadPOI a = actividadRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        a.setPlanificado(false);
        a.setEstado(EstadoActividad.Pendiente);
        actividadRepo.save(a);
    }
}
