package com.upla.sisexp.service;

import com.upla.sisexp.enums.EstadoActividad;
import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.enums.RolUsuario;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.*;
import com.upla.sisexp.repository.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BusinessValidationsService {

    private final UsuarioRepository usuarioRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final DocumentoAdjuntoRepository documentoRepo;
    private final TechoPresupuestalRepository techoRepo;

    public BusinessValidationsService(UsuarioRepository usuarioRepo,
            ActividadPOIRepository actividadRepo, NecesidadPAPRepository necesidadRepo,
            DocumentoAdjuntoRepository documentoRepo, TechoPresupuestalRepository techoRepo) {
        this.usuarioRepo = usuarioRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.documentoRepo = documentoRepo;
        this.techoRepo = techoRepo;
    }

    public void checkLoginAttempts(Usuario usuario) {
        if (usuario.getBloqueadoHasta() != null) {
            if (usuario.getBloqueadoHasta().isAfter(LocalDateTime.now())) {
                throw new BusinessException("Cuenta bloqueada hasta: " + usuario.getBloqueadoHasta());
            }
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(null);
            usuarioRepo.save(usuario);
        }
    }

    public void registerFailedAttempt(Usuario usuario) {
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);
        if (usuario.getIntentosFallidos() >= 5) {
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(30));
        }
        usuarioRepo.save(usuario);
    }

    public boolean validarInmutabilidad(Expediente e) {
        return List.of(EstadoExpediente.Aprobado,
                EstadoExpediente.Finalizado,
                EstadoExpediente.Derivado).contains(e.getEstado());
    }

    public BigDecimal getLimiteMontoPorRol(RolUsuario rol) {
        return switch (rol) {
            case Administrador, Coordinacion -> null;
            case Director -> new BigDecimal("15000");
            case Secretaria, Laboratorio -> new BigDecimal("5000");
            case Decanato -> BigDecimal.ZERO;
        };
    }

    public void validarLimiteMontoPorRol(RolUsuario rol, BigDecimal costo) {
        BigDecimal limite = getLimiteMontoPorRol(rol);
        if (limite != null && costo.compareTo(limite) > 0) {
            throw new BusinessException("Su rol (" + rol + ") tiene un limite de S/ " + limite
                    + " por expediente. Costo estimado: S/ " + costo);
        }
    }

    public void validarActividadActiva(Long actividadPoiId) {
        ActividadPOI a = actividadRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        if (a.getEstado() == EstadoActividad.Cerrado) {
            throw new BusinessException("La actividad " + a.getCodigo() + " esta cerrada");
        }
        if (a.getPlanificado()) {
            throw new BusinessException("La actividad " + a.getCodigo() + " esta planificada (cerrada)");
        }
    }

    public void validarPAPObligatorio(Long actividadPoiId) {
        List<NecesidadPAP> necesidades = necesidadRepo.findByActividadPOI_Id(actividadPoiId);
        if (necesidades.isEmpty()) {
            throw new BusinessException("La actividad no tiene necesidades PAP registradas");
        }
    }

    public void validarTopeExpediente(Long actividadPoiId, BigDecimal costo) {
        ActividadPOI a = actividadRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        BigDecimal disponible = a.getPresupuestoAsignado()
                .subtract(a.getSaldoComprometido())
                .subtract(a.getSaldoEjecutado());
        BigDecimal tope = disponible.multiply(new BigDecimal("0.8"));
        if (costo.compareTo(tope) > 0) {
            throw new BusinessException("Un expediente no puede consumir mas del 80% del saldo disponible (S/ "
                    + tope + "). Costo: S/ " + costo);
        }
    }

    public void validarPeriodoFiscal(Long actividadPoiId) {
        ActividadPOI a = actividadRepo.findById(actividadPoiId)
                .orElseThrow(() -> new BusinessException("Actividad no encontrada"));
        TechoPresupuestal techo = a.getTechoPresupuestal();
        if (techo != null && techo.getPlanificado()) {
            throw new BusinessException("El techo del año " + techo.getAño() + " esta cerrado");
        }
    }

    public void validarDocumentoObligatorio(Long expedienteId) {
        List<DocumentoAdjunto> docs = documentoRepo.findByExpediente_Id(expedienteId);
        if (docs.isEmpty()) {
            throw new BusinessException("Debe adjuntar al menos un documento antes de enviar a revision");
        }
    }

    public void validarEdicionBloqueada(Expediente e) {
        if (e.getEstado() != EstadoExpediente.Borrador) {
            throw new BusinessException("Solo se pueden editar expedientes en estado Borrador. Estado actual: " + e.getEstado());
        }
    }

    public void validarTechoCerrado(Long techoPresupuestalId) {
        TechoPresupuestal t = techoRepo.findById(techoPresupuestalId)
                .orElseThrow(() -> new BusinessException("Techo no encontrado"));
        if (t.getPlanificado()) {
            throw new BusinessException("El techo del año " + t.getAño() + " esta cerrado");
        }
    }
}
