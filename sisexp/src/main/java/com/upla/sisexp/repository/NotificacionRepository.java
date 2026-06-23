package com.upla.sisexp.repository;

import com.upla.sisexp.model.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuario_IdAndLeidaFalse(Long usuarioId);

    @Query("SELECT n FROM Notificacion n LEFT JOIN FETCH n.expediente WHERE n.usuario.id = ?1 ORDER BY n.createdAt DESC")
    List<Notificacion> findByUsuario_IdOrderByCreatedAtDesc(Long usuarioId);

    long countByUsuario_IdAndLeidaFalse(Long usuarioId);
}
