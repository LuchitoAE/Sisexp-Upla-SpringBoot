package com.upla.sisexp.expediente.repository;

import com.upla.sisexp.expediente.model.SeguimientoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeguimientoLogRepository extends JpaRepository<SeguimientoLog, Long> {
    List<SeguimientoLog> findByExpediente_IdOrderByCreatedAtDesc(Long expedienteId);
}
