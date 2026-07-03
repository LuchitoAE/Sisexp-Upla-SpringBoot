package com.upla.sisexp.presupuesto.repository;

import com.upla.sisexp.presupuesto.model.NotaModificatoria;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotaModificatoriaRepository extends JpaRepository<NotaModificatoria, Long> {
    long countByEstado(String estado);
    List<NotaModificatoria> findByEstadoOrderByCreatedAtDesc(String estado);
}
