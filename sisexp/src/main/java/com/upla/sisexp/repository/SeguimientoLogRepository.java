package com.upla.sisexp.repository;

import com.upla.sisexp.model.SeguimientoLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeguimientoLogRepository extends JpaRepository<SeguimientoLog, Long> {

    List<SeguimientoLog> findByExpediente_IdOrderByCreatedAtDesc(Long expedienteId);
}
