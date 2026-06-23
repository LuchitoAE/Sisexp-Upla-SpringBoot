package com.upla.sisexp.repository;

import com.upla.sisexp.model.TechoPresupuestal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechoPresupuestalRepository extends JpaRepository<TechoPresupuestal, Long> {

    Optional<TechoPresupuestal> findByAño(Integer año);

    List<TechoPresupuestal> findByActivoTrue();
}
