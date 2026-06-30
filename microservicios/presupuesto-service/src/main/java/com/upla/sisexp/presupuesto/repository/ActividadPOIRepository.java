package com.upla.sisexp.presupuesto.repository;

import com.upla.sisexp.presupuesto.model.ActividadPOI;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActividadPOIRepository extends JpaRepository<ActividadPOI, Long> {
    List<ActividadPOI> findByTechoPresupuestalId(Long techoId);
}
