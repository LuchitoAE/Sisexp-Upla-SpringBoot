package com.upla.sisexp.notificacion.service;

import com.upla.sisexp.common.enums.TipoNotificacion;
import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.notificacion.model.Notificacion;
import com.upla.sisexp.notificacion.repository.NotificacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacionService {
    private final NotificacionRepository notificacionRepo;
    public NotificacionService(NotificacionRepository notificacionRepo) { this.notificacionRepo = notificacionRepo; }

    public List<Notificacion> listarPorUsuario(Long usuarioId) { return notificacionRepo.findByUsuarioIdOrderByCreatedAtDesc(usuarioId); }
    public long contarNoLeidas(Long usuarioId) { return notificacionRepo.countByUsuarioIdAndLeidaFalse(usuarioId); }

    @Transactional
    public Notificacion crear(Long usuarioId, String mensaje, TipoNotificacion tipo, Long expedienteId) {
        Notificacion n = new Notificacion();
        n.setUsuarioId(usuarioId); n.setMensaje(mensaje); n.setTipo(tipo); n.setExpedienteId(expedienteId);
        return notificacionRepo.save(n);
    }

    @Transactional
    public void marcarLeida(Long id) {
        Notificacion n = notificacionRepo.findById(id).orElseThrow(() -> new BusinessException("Notificacion no encontrada"));
        n.setLeida(true); notificacionRepo.save(n);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        List<Notificacion> lista = notificacionRepo.findByUsuarioIdOrderByCreatedAtDesc(usuarioId);
        lista.forEach(n -> n.setLeida(true));
        notificacionRepo.saveAll(lista);
    }
}
