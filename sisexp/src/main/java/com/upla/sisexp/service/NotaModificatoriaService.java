package com.upla.sisexp.service;

import com.upla.sisexp.enums.EstadoNota;
import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.TipoNota;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class NotaModificatoriaService {

    private final NotaModificatoriaRepository notaRepo;
    private final UsuarioRepository usuarioRepo;
    private final ActividadPOIRepository actividadRepo;

    public NotaModificatoriaService(NotaModificatoriaRepository notaRepo,
            UsuarioRepository usuarioRepo, ActividadPOIRepository actividadRepo) {
        this.notaRepo = notaRepo;
        this.usuarioRepo = usuarioRepo;
        this.actividadRepo = actividadRepo;
    }

    public List<NotaModificatoria> listar() {
        return notaRepo.findAll();
    }

    @Transactional
    public NotaModificatoria crear(TipoNota tipo, Long actividadExistenteId,
            String nuevoNombre, String justificacion, BigDecimal costoEstimado,
            MultipartFile archivo, Long solicitanteId) {
        return crear(tipo, actividadExistenteId, null, nuevoNombre, justificacion,
                costoEstimado, archivo, solicitanteId);
    }

    @Transactional
    public NotaModificatoria crear(TipoNota tipo, Long actividadExistenteId,
            String origen, String nuevoNombre, String justificacion,
            BigDecimal costoEstimado, MultipartFile archivo, Long solicitanteId) {

        Usuario solicitante = usuarioRepo.findById(solicitanteId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));
        ActividadPOI actividad = actividadRepo.findById(actividadExistenteId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));

        NotaModificatoria nota = new NotaModificatoria();
        nota.setCodigo("NM-" + System.currentTimeMillis());
        nota.setSolicitante(solicitante);
        nota.setTipo(tipo);
        nota.setOrigen(origen);
        nota.setActividadExistente(actividad);
        nota.setNuevoNombre(nuevoNombre);
        nota.setJustificacion(justificacion);
        nota.setCostoEstimadoReferencial(costoEstimado != null ? costoEstimado : BigDecimal.ZERO);
        nota.setEstado(EstadoNota.pendiente);

        if (archivo != null && !archivo.isEmpty()) {
            nota.setNombreOriginalArchivo(archivo.getOriginalFilename());
            nota.setNombreArchivo(UUID.randomUUID().toString());
        }

        return notaRepo.save(nota);
    }

    @Transactional
    public NotaModificatoria configurar(Long notaId, Long actividadOrigenId,
            BigDecimal montoTransferir, String clasificadorGasto,
            Naturaleza tipo, String observacion, Long aprobadoPorId) {

        NotaModificatoria nota = notaRepo.findById(notaId)
                .orElseThrow(() -> new BusinessException("Nota no encontrada"));
        ActividadPOI origen = actividadRepo.findById(actividadOrigenId)
                .orElseThrow(() -> new BusinessException("Actividad origen no encontrada"));
        Usuario aprobadoPor = usuarioRepo.findById(aprobadoPorId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        nota.setActividadOrigen(origen);
        nota.setMontoTransferir(montoTransferir);
        nota.setNuevoClasificadorGasto(clasificadorGasto);
        nota.setNuevoTipo(tipo);
        nota.setObservacionAdmin(observacion);
        nota.setAprobadoPor(aprobadoPor);
        nota.setEstado(EstadoNota.configurada);

        return notaRepo.save(nota);
    }

    @Transactional
    public NotaModificatoria rechazar(Long notaId, String observacion, Long aprobadoPorId) {
        NotaModificatoria nota = notaRepo.findById(notaId)
                .orElseThrow(() -> new BusinessException("Nota no encontrada"));
        Usuario aprobadoPor = usuarioRepo.findById(aprobadoPorId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        nota.setEstado(EstadoNota.rechazada);
        nota.setObservacionAdmin(observacion);
        nota.setAprobadoPor(aprobadoPor);

        return notaRepo.save(nota);
    }
}
