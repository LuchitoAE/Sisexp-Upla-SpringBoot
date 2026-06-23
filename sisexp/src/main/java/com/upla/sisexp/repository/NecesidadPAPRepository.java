package com.upla.sisexp.repository;

import com.upla.sisexp.model.NecesidadPAP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NecesidadPAPRepository extends JpaRepository<NecesidadPAP, Long> {

    List<NecesidadPAP> findByActividadPOI_Id(Long actividadPoiId);

    List<NecesidadPAP> findByActividadPOI_IdAndCantidadDisponibleGreaterThan(Long actividadPoiId, Integer cantidadDisponible);
}
