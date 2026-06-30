package com.upla.sisexp.presupuesto.repository;

import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NecesidadPAPRepository extends JpaRepository<NecesidadPAP, Long> {
    List<NecesidadPAP> findByActividadPOIId(Long actividadId);
}
