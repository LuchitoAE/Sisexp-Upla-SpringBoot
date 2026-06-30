package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TechoPresupuestalService {
    private final TechoPresupuestalRepository techoRepo;
    public TechoPresupuestalService(TechoPresupuestalRepository techoRepo) { this.techoRepo = techoRepo; }

    public List<TechoPresupuestal> listar() {
        return techoRepo.findAll(Sort.by(Sort.Direction.DESC, "año"));
    }

    public TechoPresupuestal obtener(Long id) {
        return techoRepo.findById(id).orElseThrow(() -> new BusinessException("Techo no encontrado"));
    }

    @Transactional
    public TechoPresupuestal crear(Integer año, BigDecimal montoTotal, Long creadoPorId) {
        if (techoRepo.findByAño(año).isPresent()) throw new BusinessException("El año " + año + " ya existe");
        if (montoTotal.compareTo(BigDecimal.ZERO) <= 0) throw new BusinessException("El monto debe ser mayor a 0");
        TechoPresupuestal t = new TechoPresupuestal();
        t.setAño(año); t.setMontoTotal(montoTotal); t.setCreadoPorId(creadoPorId);
        return techoRepo.save(t);
    }

    @Transactional
    public TechoPresupuestal editar(Long id, Integer año, BigDecimal montoTotal) {
        TechoPresupuestal t = obtener(id);
        if (t.getPlanificado()) throw new BusinessException("Techo cerrado, no se puede editar");
        if (año != null) t.setAño(año);
        if (montoTotal != null) t.setMontoTotal(montoTotal);
        return techoRepo.save(t);
    }

    @Transactional
    public void toggleActivo(Long id) {
        TechoPresupuestal t = obtener(id);
        t.setActivo(!t.getActivo());
        techoRepo.save(t);
    }
}
