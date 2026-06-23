package com.upla.sisexp.repository;

import com.upla.sisexp.enums.EstadoNota;
import com.upla.sisexp.model.NotaModificatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotaModificatoriaRepository extends JpaRepository<NotaModificatoria, Long> {

    List<NotaModificatoria> findByEstado(EstadoNota estado);

    List<NotaModificatoria> findBySolicitante_Id(Long solicitanteId);
}
