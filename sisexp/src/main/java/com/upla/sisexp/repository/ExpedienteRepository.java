package com.upla.sisexp.repository;

import com.upla.sisexp.enums.EstadoExpediente;
import com.upla.sisexp.model.Expediente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Long> {

    Optional<Expediente> findByCodigo(String codigo);

    Optional<Expediente> findTopByCodigoStartingWithOrderByCodigoDesc(String prefix);

    List<Expediente> findAllByOrderByCreatedAtDesc();

    List<Expediente> findBySolicitante_IdOrderByCreatedAtDesc(Long solicitanteId);

    long countByEstado(EstadoExpediente estado);

    List<Expediente> findByEstado(EstadoExpediente estado);

    long countByFechaLimiteBeforeAndEstadoNotIn(LocalDate fecha, Collection<EstadoExpediente> estados);

    List<Expediente> findByFechaLimiteBeforeAndEstadoNotIn(LocalDate fecha, Collection<EstadoExpediente> estados);

    List<Expediente> findByFechaLimiteBetweenAndEstadoNotIn(LocalDate start, LocalDate end, Collection<EstadoExpediente> estados);

    List<Expediente> findByEstadoNotIn(Collection<EstadoExpediente> estados);

    List<Expediente> findByActividadPOI_Id(Long actividadId);

    List<Expediente> findByNecesidadPAP_Id(Long necesidadId);
}
