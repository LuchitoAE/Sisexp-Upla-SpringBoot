package com.upla.sisexp.presupuesto.repository;

import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechoPresupuestalRepository extends JpaRepository<TechoPresupuestal, Long> {
    Optional<TechoPresupuestal> findByAño(Integer año);
}
