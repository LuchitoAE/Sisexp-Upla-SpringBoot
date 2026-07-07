package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.common.enums.EstadoNota;
import com.upla.sisexp.common.enums.Naturaleza;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.NotaModificatoria;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import com.upla.sisexp.presupuesto.repository.NotaModificatoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class NotaModificatoriaService {

    private final NotaModificatoriaRepository notaRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;

    public NotaModificatoriaService(NotaModificatoriaRepository notaRepo,
                                     ActividadPOIRepository actividadRepo,
                                     NecesidadPAPRepository necesidadRepo) {
        this.notaRepo = notaRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
    }

    public List<NotaModificatoria> listarTodas() {
        return notaRepo.findAll();
    }

    public Optional<NotaModificatoria> buscarPorId(Long id) {
        return notaRepo.findById(id);
    }

    @Transactional
    public NotaModificatoria crear(NotaModificatoria nota) {
        long count = notaRepo.count();
        nota.setCodigo("NOTA-2026-" + String.format("%03d", count + 1));
        nota.setEstado(EstadoNota.pendiente);
        return notaRepo.save(nota);
    }

    @Transactional
    public NotaModificatoria configurar(Long id, Long actividadOrigenId, BigDecimal monto,
                                         String clasificador, Naturaleza tipo) {
        NotaModificatoria nota = notaRepo.findById(id)
            .orElseThrow(() -> new BusinessException("Nota no encontrada"));

        if (nota.getTipo().name().contains("actividad") && actividadOrigenId != null) {
            ActividadPOI origen = actividadRepo.findById(actividadOrigenId)
                .orElseThrow(() -> new BusinessException("Actividad origen no encontrada"));
            BigDecimal disponible = origen.getPresupuestoAsignado()
                .subtract(origen.getSaldoEjecutado())
                .subtract(origen.getSaldoComprometido());
            if (monto.compareTo(disponible) > 0)
                throw new BusinessException("Monto excede el disponible en actividad origen: " + disponible);
            origen.setSaldoComprometido(origen.getSaldoComprometido().add(monto));
            actividadRepo.save(origen);
            nota.setActividadOrigenId(actividadOrigenId);
        }

        if (nota.getTipo().name().contains("item")) {
            Long actId = nota.getActividadExistenteId();
            if (actId != null) {
                ActividadPOI dest = actividadRepo.findById(actId)
                    .orElseThrow(() -> new BusinessException("Actividad destino no encontrada"));
                BigDecimal disponible = dest.getPresupuestoAsignado()
                    .subtract(dest.getSaldoEjecutado())
                    .subtract(dest.getSaldoComprometido());
                if (monto.compareTo(disponible) > 0)
                    throw new BusinessException("Monto excede el disponible en actividad destino: " + disponible);
                dest.setSaldoComprometido(dest.getSaldoComprometido().add(monto));
                actividadRepo.save(dest);

                NecesidadPAP pap = new NecesidadPAP();
                pap.setNombre(nota.getNuevoNombre());
                pap.setCantidad(1);
                pap.setPrecioEstimado(monto);
                pap.setUnidad("servicio");
                pap.setOficinaLaboratorio(nota.getOrigen());
                pap.setTipo(tipo);
                pap.setClasificadorGasto(clasificador);
                pap.setActividadPOIId(actId);
                pap.setCantidadDisponible(1);
                pap.setMontoDisponible(monto);
                necesidadRepo.save(pap);
            }
        }

        nota.setMontoTransferir(monto);
        nota.setNuevoClasificadorGasto(clasificador);
        nota.setNuevoTipo(tipo);
        nota.setEstado(EstadoNota.configurada);
        nota.setUpdatedAt(LocalDateTime.now());
        return notaRepo.save(nota);
    }

    @Transactional
    public NotaModificatoria rechazar(Long id, String observacion) {
        NotaModificatoria nota = notaRepo.findById(id)
            .orElseThrow(() -> new BusinessException("Nota no encontrada"));
        nota.setEstado(EstadoNota.rechazada);
        nota.setObservacionAdmin(observacion);
        nota.setUpdatedAt(LocalDateTime.now());
        return notaRepo.save(nota);
    }
}
