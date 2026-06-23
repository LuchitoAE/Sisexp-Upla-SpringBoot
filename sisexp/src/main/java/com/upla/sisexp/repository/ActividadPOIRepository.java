package com.upla.sisexp.repository;

import com.upla.sisexp.enums.EstadoActividad;
import com.upla.sisexp.model.ActividadPOI;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActividadPOIRepository extends JpaRepository<ActividadPOI, Long> {

    List<ActividadPOI> findByTechoPresupuestal_Id(Long techoPresupuestalId);

    List<ActividadPOI> findByEstado(EstadoActividad estado);

    long count();
}
