package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.common.enums.EstadoActividad;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class ActividadPOIService {
    private final ActividadPOIRepository actividadRepo;
    private final TechoPresupuestalRepository techoRepo;
    public ActividadPOIService(ActividadPOIRepository actividadRepo, TechoPresupuestalRepository techoRepo) {
        this.actividadRepo = actividadRepo;
        this.techoRepo = techoRepo;
    }

    public List<ActividadPOI> listar() { return actividadRepo.findAll(); }
    public List<ActividadPOI> listarPorTecho(Long techoId) { return actividadRepo.findByTechoPresupuestalId(techoId); }
    public ActividadPOI obtener(Long id) { return actividadRepo.findById(id).orElseThrow(() -> new BusinessException("Actividad no encontrada")); }

    @Transactional
    public ActividadPOI crear(String codigo, String nombre, BigDecimal presupuestoAsignado, LocalDate fechaLimite, Long techoId) {
        ActividadPOI a = new ActividadPOI();
        a.setCodigo(codigo); a.setNombre(nombre);
        a.setPresupuestoAsignado(presupuestoAsignado); a.setFechaLimite(fechaLimite);
        a.setTechoPresupuestalId(techoId);
        return actividadRepo.save(a);
    }

    @Transactional
    public ActividadPOI editar(Long id, String codigo, String nombre, BigDecimal presupuestoAsignado, LocalDate fechaLimite) {
        ActividadPOI a = obtener(id);
        if (codigo != null) a.setCodigo(codigo);
        if (nombre != null) a.setNombre(nombre);
        if (presupuestoAsignado != null) a.setPresupuestoAsignado(presupuestoAsignado);
        if (fechaLimite != null) a.setFechaLimite(fechaLimite);
        return actividadRepo.save(a);
    }

    @Transactional
    public void eliminar(Long id) { actividadRepo.delete(obtener(id)); }

    @Transactional
    public void planificarPOI(Long techoId) {
        TechoPresupuestal techo = techoRepo.findById(techoId)
            .orElseThrow(() -> new BusinessException("Techo no encontrado"));
        List<ActividadPOI> actividades = actividadRepo.findByTechoPresupuestalId(techoId);
        for (ActividadPOI a : actividades) { a.setPlanificado(true); }
        actividadRepo.saveAll(actividades);
        techo.setPlanificado(true);
        techoRepo.save(techo);
    }

    @Transactional
    public void desplanificarPOI(Long techoId) {
        TechoPresupuestal techo = techoRepo.findById(techoId)
            .orElseThrow(() -> new BusinessException("Techo no encontrado"));
        List<ActividadPOI> actividades = actividadRepo.findByTechoPresupuestalId(techoId);
        for (ActividadPOI a : actividades) { a.setPlanificado(false); }
        actividadRepo.saveAll(actividades);
        techo.setPlanificado(false);
        techoRepo.save(techo);
    }

    @Transactional
    public void finalizarPAP(Long id) {
        ActividadPOI a = obtener(id);
        a.setEstado(EstadoActividad.Cerrado);
        actividadRepo.save(a);
    }

    @Transactional
    public void desbloquearPAP(Long id) {
        ActividadPOI a = obtener(id);
        a.setEstado(EstadoActividad.Pendiente);
        actividadRepo.save(a);
    }
}
