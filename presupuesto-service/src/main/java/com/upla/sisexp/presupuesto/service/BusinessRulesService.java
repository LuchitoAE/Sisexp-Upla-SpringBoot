package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class BusinessRulesService {
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final TechoPresupuestalRepository techoRepo;

    public BusinessRulesService(ActividadPOIRepository actividadRepo, NecesidadPAPRepository necesidadRepo,
            TechoPresupuestalRepository techoRepo) {
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.techoRepo = techoRepo;
    }

    public void reservarSaldo(Long actividadPoiId, BigDecimal costo) {
        ActividadPOI a = actividadRepo.findById(actividadPoiId).orElseThrow();
        a.setSaldoComprometido(a.getSaldoComprometido().add(costo));
        actividadRepo.save(a);
    }

    public void ejecutarSaldo(Long actividadPoiId, BigDecimal costo, Long necesidadPapId, int cantidad) {
        ActividadPOI a = actividadRepo.findById(actividadPoiId).orElseThrow();
        a.setSaldoComprometido(a.getSaldoComprometido().subtract(costo).max(BigDecimal.ZERO));
        a.setSaldoEjecutado(a.getSaldoEjecutado().add(costo));
        actividadRepo.save(a);

        TechoPresupuestal techo = techoRepo.findById(a.getTechoPresupuestalId()).orElse(null);
        if (techo != null) {
            techo.setMontoUtilizado(techo.getMontoUtilizado().add(costo));
            techoRepo.save(techo);
        }

        if (necesidadPapId != null && cantidad > 0) {
            NecesidadPAP n = necesidadRepo.findById(necesidadPapId).orElseThrow();
            n.setCantidadEjecutada(n.getCantidadEjecutada() + cantidad);
            n.setMontoEjecutado(n.getMontoEjecutado().add(costo));
            necesidadRepo.save(n);
        }
    }

    public void liberarSaldo(Long actividadPoiId, BigDecimal costo) {
        ActividadPOI a = actividadRepo.findById(actividadPoiId).orElseThrow();
        a.setSaldoComprometido(a.getSaldoComprometido().subtract(costo).max(BigDecimal.ZERO));
        actividadRepo.save(a);
    }

    public void reservarSaldoPAP(Long necesidadPapId, int cantidad, BigDecimal costo) {
        NecesidadPAP n = necesidadRepo.findById(necesidadPapId).orElseThrow();
        n.setCantidadDisponible(n.getCantidadDisponible() - cantidad);
        n.setMontoDisponible(n.getMontoDisponible().subtract(costo).max(BigDecimal.ZERO));
        necesidadRepo.save(n);
    }

    public void liberarSaldoPAP(Long necesidadPapId, int cantidad, BigDecimal costo) {
        NecesidadPAP n = necesidadRepo.findById(necesidadPapId).orElseThrow();
        n.setCantidadDisponible(n.getCantidadDisponible() + cantidad);
        n.setMontoDisponible(n.getMontoDisponible().add(costo));
        necesidadRepo.save(n);
    }
}
