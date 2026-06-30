package com.upla.sisexp.expediente.repository;

import com.upla.sisexp.expediente.model.DocumentoAdjunto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {
    List<DocumentoAdjunto> findByExpediente_Id(Long expedienteId);
}
