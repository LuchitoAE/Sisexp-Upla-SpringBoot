package com.upla.sisexp.expediente.service;

import com.upla.sisexp.common.enums.EstadoExpediente;
import com.upla.sisexp.common.enums.TipoDocumento;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.expediente.config.RabbitMQConfig;
import com.upla.sisexp.expediente.model.*;
import com.upla.sisexp.expediente.repository.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;

@Service
public class ExpedienteService {
    private final ExpedienteRepository expedienteRepo;
    private final DocumentoAdjuntoRepository documentoRepo;
    private final SeguimientoLogRepository seguimientoRepo;
    private final RabbitTemplate rabbitTemplate;

    private static final Map<EstadoExpediente, Set<EstadoExpediente>> TRANSICIONES = Map.of(
        EstadoExpediente.Borrador, Set.of(EstadoExpediente.En_revision),
        EstadoExpediente.En_revision, Set.of(EstadoExpediente.Aprobado, EstadoExpediente.Rechazado, EstadoExpediente.Observado),
        EstadoExpediente.Observado, Set.of(EstadoExpediente.En_revision),
        EstadoExpediente.Aprobado, Set.of(EstadoExpediente.Finalizado, EstadoExpediente.Derivado),
        EstadoExpediente.Derivado, Set.of(EstadoExpediente.Finalizado)
    );

    public ExpedienteService(ExpedienteRepository expedienteRepo, DocumentoAdjuntoRepository documentoRepo,
            SeguimientoLogRepository seguimientoRepo, RabbitTemplate rabbitTemplate) {
        this.expedienteRepo = expedienteRepo; this.documentoRepo = documentoRepo;
        this.seguimientoRepo = seguimientoRepo; this.rabbitTemplate = rabbitTemplate;
    }

    public List<Expediente> listar() { return expedienteRepo.findAll(); }
    public List<Expediente> listarPorSolicitante(Long solicitanteId) { return expedienteRepo.findBySolicitanteId(solicitanteId); }

    @Transactional(readOnly = true)
    public Expediente obtenerConLogs(Long id) {
        Expediente exp = expedienteRepo.findById(id).orElseThrow(() -> new BusinessException("Expediente no encontrado"));
        exp.getDocumentos().size(); exp.getLogs().size();
        return exp;
    }

    @Transactional
    public Expediente crear(Long actividadPoiId, Long necesidadPapId, Long solicitanteId,
            String urgencia, String naturaleza, String descripcion, int cantidad, BigDecimal costo) {
        Expediente e = new Expediente();
        e.setCodigo(generarNumero());
        e.setActividadPOIId(actividadPoiId);
        e.setNecesidadPAPId(necesidadPapId);
        e.setSolicitanteId(solicitanteId);
        e.setUrgencia(com.upla.sisexp.common.enums.Urgencia.valueOf(urgencia));
        e.setNaturaleza(naturaleza != null ? com.upla.sisexp.common.enums.Naturaleza.valueOf(naturaleza) : null);
        e.setDescripcion(descripcion);
        e.setCantidadSolicitada(cantidad);
        e.setCostoEstimado(costo);
        Expediente saved = expedienteRepo.save(e);
        crearLog(saved, null, saved.getEstado().name(), solicitanteId, "Expediente creado");
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_EXPEDIENTE_EVENTOS,
            Map.of("tipo", "expediente.creado", "expedienteId", saved.getId(), "codigo", saved.getCodigo(), "solicitanteId", solicitanteId));
        return saved;
    }

    @Transactional
    public Expediente actualizarEstado(Long id, String nuevoEstadoStr, String observacion, Long usuarioId) {
        Expediente exp = obtenerConLogs(id);
        EstadoExpediente estadoActual = exp.getEstado();
        EstadoExpediente nuevoEstado = EstadoExpediente.valueOf(nuevoEstadoStr.replace(' ', '_'));
        Set<EstadoExpediente> permitidos = TRANSICIONES.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new BusinessException("Transicion invalida: de " + estadoActual + " a " + nuevoEstado);
        }
        exp.setEstado(nuevoEstado);
        if (observacion != null && !observacion.isBlank()) exp.setObservacion(observacion);
        if (nuevoEstado == EstadoExpediente.Aprobado) exp.setAprobadoPorId(usuarioId);
        Expediente saved = expedienteRepo.save(exp);
        crearLog(saved, estadoActual.name(), nuevoEstado.name(), usuarioId, observacion);
        rabbitTemplate.convertAndSend(RabbitMQConfig.QUEUE_EXPEDIENTE_EVENTOS,
            Map.of("tipo", "expediente.cambiado", "expedienteId", saved.getId(), "codigo", saved.getCodigo(),
                "estadoAnterior", estadoActual.name(), "estadoNuevo", nuevoEstado.name(), "usuarioId", usuarioId));
        return saved;
    }

    @Transactional
    public DocumentoAdjunto subirDocumento(Long expedienteId, TipoDocumento tipo, MultipartFile archivo) {
        Expediente exp = expedienteRepo.findById(expedienteId).orElseThrow();
        DocumentoAdjunto doc = new DocumentoAdjunto();
        doc.setExpediente(exp); doc.setTipo(tipo);
        doc.setNombreOriginal(archivo.getOriginalFilename());
        doc.setNombreArchivo(UUID.randomUUID().toString());
        doc.setMimeType(archivo.getContentType());
        doc.setTamaño((int) archivo.getSize());
        return documentoRepo.save(doc);
    }

    private String generarNumero() {
        int año = Year.now().getValue();
        String prefix = "EXP-" + año + "-";
        return expedienteRepo.findTopByCodigoStartingWithOrderByCodigoDesc(prefix)
            .map(Expediente::getCodigo)
            .map(cod -> prefix + String.format("%04d", Integer.parseInt(cod.substring(cod.lastIndexOf('-') + 1)) + 1))
            .orElse(prefix + "0001");
    }

    private void crearLog(Expediente exp, String anterior, String nuevo, Long usuarioId, String obs) {
        SeguimientoLog log = new SeguimientoLog();
        log.setExpediente(exp); log.setEstadoAnterior(anterior);
        log.setEstadoNuevo(nuevo); log.setUsuarioId(usuarioId); log.setObservacion(obs);
        seguimientoRepo.save(log);
    }
}
