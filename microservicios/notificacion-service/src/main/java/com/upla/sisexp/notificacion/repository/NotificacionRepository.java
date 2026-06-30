package com.upla.sisexp.notificacion.repository;

import com.upla.sisexp.notificacion.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {
    List<Notificacion> findByUsuarioIdOrderByCreatedAtDesc(Long usuarioId);
    long countByUsuarioIdAndLeidaFalse(Long usuarioId);
}
