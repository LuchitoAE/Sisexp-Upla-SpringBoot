package com.upla.sisexp.service;

import com.upla.sisexp.enums.TipoNotificacion;
import com.upla.sisexp.model.Expediente;
import com.upla.sisexp.model.Notificacion;
import com.upla.sisexp.model.Usuario;
import com.upla.sisexp.repository.NotificacionRepository;
import com.upla.sisexp.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepo;
    private final UsuarioRepository usuarioRepo;

    public NotificacionService(NotificacionRepository notificacionRepo,
            UsuarioRepository usuarioRepo) {
        this.notificacionRepo = notificacionRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<Notificacion> listarPorUsuario(Long usuarioId) {
        return notificacionRepo.findByUsuario_IdOrderByCreatedAtDesc(usuarioId);
    }

    public long contarNoLeidas(Long usuarioId) {
        return notificacionRepo.countByUsuario_IdAndLeidaFalse(usuarioId);
    }

    @Transactional
    public Notificacion marcarLeida(Long id) {
        Notificacion n = notificacionRepo.findById(id).orElseThrow();
        n.setLeida(true);
        return notificacionRepo.save(n);
    }

    @Transactional
    public void marcarTodasLeidas(Long usuarioId) {
        List<Notificacion> noLeidas = notificacionRepo.findByUsuario_IdAndLeidaFalse(usuarioId);
        noLeidas.forEach(n -> n.setLeida(true));
        notificacionRepo.saveAll(noLeidas);
    }

    @Transactional
    public Notificacion crearNotificacion(Long usuarioId, String mensaje,
            TipoNotificacion tipo, Expediente expediente) {
        Usuario usuario = usuarioRepo.findById(usuarioId).orElseThrow();
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setExpediente(expediente);
        n.setLeida(false);
        return notificacionRepo.save(n);
    }
}
