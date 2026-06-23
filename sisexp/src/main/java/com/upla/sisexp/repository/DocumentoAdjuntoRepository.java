package com.upla.sisexp.repository;

import com.upla.sisexp.model.DocumentoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {

    List<DocumentoAdjunto> findByExpediente_Id(Long expedienteId);
}
