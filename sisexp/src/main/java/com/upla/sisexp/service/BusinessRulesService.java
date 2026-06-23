package com.upla.sisexp.service;

import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;

@Service
@Transactional
public class BusinessRulesService {

    private final ActividadPOIRepository actividadPOIRepo;
    private final NecesidadPAPRepository necesidadPAPRepo;
    private final ExpedienteRepository expedienteRepo;
    private final TechoPresupuestalRepository techoRepo;

    public BusinessRulesService(ActividadPOIRepository actividadPOIRepo,
            NecesidadPAPRepository necesidadPAPRepo,
            ExpedienteRepository expedienteRepo,
            TechoPresupuestalRepository techoRepo) {
        this.actividadPOIRepo = actividadPOIRepo;
        this.necesidadPAPRepo = necesidadPAPRepo;
        this.expedienteRepo = expedienteRepo;
        this.techoRepo = techoRepo;
    }

    public void validarFechaLimite(Long actividadPoiId) {
        ActividadPOI actividad = actividadPOIRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        if (actividad.getFechaLimite() == null) return;
        if (LocalDate.now().isAfter(actividad.getFechaLimite())) {
            throw new BusinessException("Fecha limite vencida: "
                    + actividad.getFechaLimite() + ". No se pueden crear expedientes para esta actividad.");
        }
    }

    public BigDecimal validarSaldoDisponible(Long actividadPoiId, BigDecimal costo) {
        ActividadPOI a = actividadPOIRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        BigDecimal disponible = a.getPresupuestoAsignado()
                .subtract(a.getSaldoComprometido())
                .subtract(a.getSaldoEjecutado());
        if (costo.compareTo(disponible) > 0) {
            throw new BusinessException("Saldo insuficiente. Disponible: S/ " + disponible
                    + ". Solicitado: S/ " + costo);
        }
        return disponible;
    }

    public BigDecimal obtenerCostoNecesidad(Long necesidadPapId, int cantidadSolicitada) {
        NecesidadPAP n = necesidadPAPRepo.findById(necesidadPapId)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        return n.getPrecioEstimado().multiply(BigDecimal.valueOf(cantidadSolicitada));
    }

    public void reservarSaldo(Long actividadPoiId, BigDecimal costo) {
        ActividadPOI a = actividadPOIRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        a.setSaldoComprometido(a.getSaldoComprometido().add(costo));
        actividadPOIRepo.save(a);
    }

    public void liberarSaldo(Long actividadPoiId, BigDecimal costo) {
        ActividadPOI a = actividadPOIRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        BigDecimal nuevo = a.getSaldoComprometido().subtract(costo);
        a.setSaldoComprometido(nuevo.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : nuevo);
        actividadPOIRepo.save(a);
    }

    public void ejecutarSaldo(Long actividadPoiId, BigDecimal costo,
            Long necesidadPapId, int cantidadSolicitada) {
        ActividadPOI a = actividadPOIRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        a.setSaldoComprometido(a.getSaldoComprometido().subtract(costo).max(BigDecimal.ZERO));
        a.setSaldoEjecutado(a.getSaldoEjecutado().add(costo));
        actividadPOIRepo.save(a);

        TechoPresupuestal techo = a.getTechoPresupuestal();
        if (techo != null) {
            techo.setMontoUtilizado(techo.getMontoUtilizado().add(costo));
            techoRepo.save(techo);
        }

        if (necesidadPapId != null && cantidadSolicitada > 0) {
            ejecutarSaldoPAP(necesidadPapId, cantidadSolicitada, costo);
        }
    }

    public void reservarSaldoPAP(Long necesidadPapId, int cantidadSolicitada, BigDecimal costo) {
        if (necesidadPapId == null) return;
        NecesidadPAP n = necesidadPAPRepo.findById(necesidadPapId)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        if (cantidadSolicitada > n.getCantidadDisponible()) {
            throw new BusinessException("Cantidad solicitada (" + cantidadSolicitada
                    + ") excede la disponible (" + n.getCantidadDisponible() + ")");
        }
        n.setCantidadDisponible(n.getCantidadDisponible() - cantidadSolicitada);
        n.setMontoDisponible(n.getMontoDisponible().subtract(costo).max(BigDecimal.ZERO));
        necesidadPAPRepo.save(n);
    }

    public void liberarSaldoPAP(Long necesidadPapId, int cantidadSolicitada, BigDecimal costo) {
        if (necesidadPapId == null) return;
        NecesidadPAP n = necesidadPAPRepo.findById(necesidadPapId)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        n.setCantidadDisponible(n.getCantidadDisponible() + cantidadSolicitada);
        n.setMontoDisponible(n.getMontoDisponible().add(costo));
        necesidadPAPRepo.save(n);
    }

    public void ejecutarSaldoPAP(Long necesidadPapId, int cantidadSolicitada, BigDecimal costo) {
        if (necesidadPapId == null) return;
        NecesidadPAP n = necesidadPAPRepo.findById(necesidadPapId)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        n.setCantidadEjecutada(n.getCantidadEjecutada() + cantidadSolicitada);
        n.setMontoEjecutado(n.getMontoEjecutado().add(costo));
        necesidadPAPRepo.save(n);
    }

    public void validarCorrespondenciaNaturaleza(Long necesidadPapId, Naturaleza naturaleza) {
        NecesidadPAP n = necesidadPAPRepo.findById(necesidadPapId)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));
        if (n.getTipo() != naturaleza) {
            throw new BusinessException("El item PAP es de tipo " + n.getTipo()
                    + " pero se solicita como " + naturaleza);
        }
    }

    public String generarNumeroExpediente() {
        int año = Year.now().getValue();
        String prefix = "EXP-" + año + "-";
        return expedienteRepo.findTopByCodigoStartingWithOrderByCodigoDesc(prefix)
                .map(Expediente::getCodigo)
                .map(cod -> {
                    int seq = Integer.parseInt(cod.substring(cod.lastIndexOf('-') + 1)) + 1;
                    return prefix + String.format("%04d", seq);
                })
                .orElse(prefix + "0001");
    }
}
