package com.upla.sisexp.service;

import com.upla.sisexp.dto.CambiarEstadoDTO;
import com.upla.sisexp.dto.DocumentoDTO;
import com.upla.sisexp.dto.ExpedienteFormDTO;
import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.enums.TipoNotificacion;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import com.upla.sisexp.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.*;

@Service
public class ExpedienteService {

    private final ExpedienteRepository expedienteRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final UsuarioRepository usuarioRepo;
    private final DocumentoAdjuntoRepository documentoRepo;
    private final SeguimientoLogRepository seguimientoRepo;
    private final NotificacionService notificacionService;
    private final BusinessRulesService businessRules;
    private final BusinessValidationsService businessValidations;

    private static final Map<EstadoExpediente, Set<EstadoExpediente>> TRANSICIONES = Map.of(
            EstadoExpediente.Borrador, Set.of(EstadoExpediente.En_revision),
            EstadoExpediente.En_revision, Set.of(EstadoExpediente.Aprobado, EstadoExpediente.Rechazado, EstadoExpediente.Observado),
            EstadoExpediente.Observado, Set.of(EstadoExpediente.En_revision),
            EstadoExpediente.Aprobado, Set.of(EstadoExpediente.Finalizado, EstadoExpediente.Derivado),
            EstadoExpediente.Derivado, Set.of(EstadoExpediente.Finalizado)
    );

    public ExpedienteService(ExpedienteRepository expedienteRepo,
            ActividadPOIRepository actividadRepo, NecesidadPAPRepository necesidadRepo,
            UsuarioRepository usuarioRepo, DocumentoAdjuntoRepository documentoRepo,
            SeguimientoLogRepository seguimientoRepo,
            NotificacionService notificacionService,
            BusinessRulesService businessRules,
            BusinessValidationsService businessValidations) {
        this.expedienteRepo = expedienteRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.usuarioRepo = usuarioRepo;
        this.documentoRepo = documentoRepo;
        this.seguimientoRepo = seguimientoRepo;
        this.notificacionService = notificacionService;
        this.businessRules = businessRules;
        this.businessValidations = businessValidations;
    }

    public List<Expediente> listar(CustomUserDetails user) {
        String rol = user.getRol();
        if (rol.equals("Laboratorio")) {
            return expedienteRepo.findBySolicitante_IdOrderByCreatedAtDesc(user.getId());
        }
        return expedienteRepo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Expediente crear(ExpedienteFormDTO dto, CustomUserDetails user) {
        ActividadPOI actividad = actividadRepo.findById(dto.getActividadPoiId())
                .orElseThrow(() -> new BusinessException("Actividad POI no encontrada"));
        NecesidadPAP necesidad = necesidadRepo.findById(dto.getNecesidadPapId())
                .orElseThrow(() -> new BusinessException("Necesidad PAP no encontrada"));
        Usuario solicitante = usuarioRepo.findById(user.getId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        Expediente exp = new Expediente();
        exp.setCodigo(businessRules.generarNumeroExpediente());
        exp.setActividadPOI(actividad);
        exp.setNecesidadPAP(necesidad);
        exp.setSolicitante(solicitante);
        exp.setUrgencia(dto.getUrgencia());
        exp.setNaturaleza(dto.getNaturaleza() != null ? dto.getNaturaleza() : necesidad.getTipo());
        exp.setDescripcion(dto.getDescripcion());
        exp.setCantidadSolicitada(dto.getCantidadSolicitada());
        exp.setFechaLimite(dto.getFechaLimite());

        businessValidations.validarActividadActiva(dto.getActividadPoiId());
        businessRules.validarFechaLimite(dto.getActividadPoiId());

        BigDecimal costoEstimado = dto.getCostoEstimado() != null
                && dto.getCostoEstimado().compareTo(BigDecimal.ZERO) > 0
                ? dto.getCostoEstimado()
                : businessRules.obtenerCostoNecesidad(dto.getNecesidadPapId(), dto.getCantidadSolicitada());

        businessRules.validarSaldoDisponible(dto.getActividadPoiId(), costoEstimado);
        businessValidations.validarLimiteMontoPorRol(solicitante.getRol(), costoEstimado);
        businessValidations.validarTopeExpediente(dto.getActividadPoiId(), costoEstimado);

        if (dto.getNaturaleza() != null) {
            businessRules.validarCorrespondenciaNaturaleza(dto.getNecesidadPapId(), dto.getNaturaleza());
        }
        exp.setCostoEstimado(costoEstimado);

        Expediente saved = expedienteRepo.save(exp);

        crearLog(saved, null, EstadoExpediente.Borrador, solicitante,
                "Expediente creado");

        return saved;
    }

    public Expediente obtenerConLogs(Long id) {
        Expediente exp = expedienteRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Expediente no encontrado"));
        return exp;
    }

    public List<SeguimientoLog> getHistorial(Long expedienteId) {
        return seguimientoRepo.findByExpediente_IdOrderByCreatedAtDesc(expedienteId);
    }

    public List<DocumentoAdjunto> getDocumentos(Long expedienteId) {
        return documentoRepo.findByExpediente_Id(expedienteId);
    }

    @Transactional
    public Expediente actualizarEstado(Long id, CambiarEstadoDTO dto, CustomUserDetails user) {
        Expediente exp = expedienteRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Expediente no encontrado"));

        EstadoExpediente estadoActual = exp.getEstado();
        EstadoExpediente nuevoEstado = dto.getNuevoEstado();

        Set<EstadoExpediente> permitidos = TRANSICIONES.getOrDefault(estadoActual, Set.of());
        if (!permitidos.contains(nuevoEstado)) {
            throw new BusinessException("Transicion invalida: de " + estadoActual + " a " + nuevoEstado);
        }

        Usuario usuario = usuarioRepo.findById(user.getId())
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        exp.setEstado(nuevoEstado);
        if (dto.getObservacion() != null && !dto.getObservacion().isBlank()) {
            exp.setObservacion(dto.getObservacion());
        }

        if (nuevoEstado == EstadoExpediente.Aprobado) {
            exp.setAprobadoPor(usuario);
        }

        Expediente saved = expedienteRepo.save(exp);

        ejecutarReglasPresupuestales(exp, estadoActual, nuevoEstado);

        crearLog(saved, estadoActual, nuevoEstado, usuario,
                dto.getObservacion() != null ? dto.getObservacion() : null);

        String codigo = saved.getCodigo();
        Long solicitanteId = saved.getSolicitante().getId();
        String obs = dto.getObservacion() != null ? dto.getObservacion() : "";

        if (nuevoEstado == EstadoExpediente.Aprobado) {
            notificacionService.crearNotificacion(solicitanteId,
                    "Su expediente " + codigo + " ha sido aprobado",
                    TipoNotificacion.aprobacion, saved);
        } else if (nuevoEstado == EstadoExpediente.Rechazado) {
            notificacionService.crearNotificacion(solicitanteId,
                    "Su expediente " + codigo + " ha sido rechazado. Motivo: " + obs,
                    TipoNotificacion.rechazo, saved);
        } else if (nuevoEstado == EstadoExpediente.Observado) {
            notificacionService.crearNotificacion(solicitanteId,
                    "Su expediente " + codigo + " tiene observaciones: " + obs,
                    TipoNotificacion.observacion, saved);
        }

        return saved;
    }

    @Transactional
    public DocumentoAdjunto subirDocumento(Long expedienteId, DocumentoDTO dto) {
        Expediente exp = expedienteRepo.findById(expedienteId)
                .orElseThrow(() -> new BusinessException("Expediente no encontrado"));

        if (dto.getArchivo() == null || dto.getArchivo().isEmpty()) {
            throw new BusinessException("El archivo es obligatorio");
        }

        try {
            DocumentoAdjunto doc = new DocumentoAdjunto();
            doc.setExpediente(exp);
            doc.setTipo(dto.getTipo());
            doc.setNombreOriginal(dto.getArchivo().getOriginalFilename());
            doc.setNombreArchivo(UUID.randomUUID().toString());
            doc.setMimeType(dto.getArchivo().getContentType());
            doc.setTamaño((int) dto.getArchivo().getSize());
            return documentoRepo.save(doc);
        } catch (Exception e) {
            throw new BusinessException("Error al subir el documento: " + e.getMessage());
        }
    }

    @Transactional
    public void eliminarDocumento(Long documentoId) {
        DocumentoAdjunto doc = documentoRepo.findById(documentoId)
                .orElseThrow(() -> new BusinessException("Documento no encontrado"));
        documentoRepo.delete(doc);
    }

    public Map<String, Object> getDisponibilidad(Long actividadId, Long necesidadId) {
        ActividadPOI actividad = actividadRepo.findById(actividadId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        NecesidadPAP necesidad = necesidadRepo.findById(necesidadId)
                .orElseThrow(() -> new BusinessException("Necesidad no encontrada"));

        BigDecimal disponibleAct = actividad.getPresupuestoAsignado()
                .subtract(actividad.getSaldoComprometido())
                .subtract(actividad.getSaldoEjecutado());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("saldoDisponibleActividad", disponibleAct);
        result.put("cantidadDisponiblePAP", necesidad.getCantidadDisponible());
        result.put("montoDisponiblePAP", necesidad.getMontoDisponible());
        result.put("precioUnitario", necesidad.getPrecioEstimado());
        return result;
    }

    public List<NecesidadPAP> getNecesidadesPorActividad(Long actividadId) {
        return necesidadRepo.findByActividadPOI_Id(actividadId);
    }

    public List<ActividadPOI> getActividadesActivas() {
        return actividadRepo.findAll();
    }

    private void ejecutarReglasPresupuestales(Expediente exp, EstadoExpediente anterior,
            EstadoExpediente nuevo) {
        Long poiId = exp.getActividadPOI().getId();
        Long papId = exp.getNecesidadPAP().getId();
        BigDecimal costo = exp.getCostoEstimado();
        int cantidad = exp.getCantidadSolicitada();

        if (anterior == EstadoExpediente.Borrador && nuevo == EstadoExpediente.En_revision) {
            businessRules.reservarSaldo(poiId, costo);
            businessRules.reservarSaldoPAP(papId, cantidad, costo);
        } else if (anterior == EstadoExpediente.En_revision && nuevo == EstadoExpediente.Aprobado) {
            businessRules.ejecutarSaldo(poiId, costo, papId, cantidad);
        } else if (anterior == EstadoExpediente.En_revision
                && (nuevo == EstadoExpediente.Rechazado || nuevo == EstadoExpediente.Observado)) {
            businessRules.liberarSaldo(poiId, costo);
            businessRules.liberarSaldoPAP(papId, cantidad, costo);
        } else if (anterior == EstadoExpediente.Observado && nuevo == EstadoExpediente.En_revision) {
            businessRules.reservarSaldo(poiId, costo);
            businessRules.reservarSaldoPAP(papId, cantidad, costo);
        }
    }

    private void crearLog(Expediente exp, EstadoExpediente estadoAnterior,
            EstadoExpediente estadoNuevo, Usuario usuario, String observacion) {
        SeguimientoLog log = new SeguimientoLog();
        log.setExpediente(exp);
        log.setEstadoAnterior(estadoAnterior != null ? estadoAnterior.name() : null);
        log.setEstadoNuevo(estadoNuevo.name());
        log.setUsuario(usuario);
        log.setObservacion(observacion);
        seguimientoRepo.save(log);
    }
}
