package com.upla.sisexp.expediente.repository;

import com.upla.sisexp.common.enums.EstadoExpediente;
import com.upla.sisexp.expediente.model.Expediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {
    Optional<Expediente> findByCodigo(String codigo);
    List<Expediente> findBySolicitanteId(Long solicitanteId);
    List<Expediente> findByActividadPOIId(Long actividadId);
    long countByEstado(EstadoExpediente estado);
    long countBySolicitanteId(Long solicitanteId);
    @Query("SELECT e FROM Expediente e WHERE e.codigo LIKE :prefix% ORDER BY e.codigo DESC")
    Optional<Expediente> findTopByCodigoStartingWithOrderByCodigoDesc(@Param("prefix") String prefix);
}
