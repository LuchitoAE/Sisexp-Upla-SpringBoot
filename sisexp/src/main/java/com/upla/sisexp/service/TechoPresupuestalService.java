package com.upla.sisexp.service;

import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.model.TechoPresupuestal;
import com.upla.sisexp.model.Usuario;
import com.upla.sisexp.repository.TechoPresupuestalRepository;
import com.upla.sisexp.repository.UsuarioRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TechoPresupuestalService {

    private final TechoPresupuestalRepository techoRepo;
    private final UsuarioRepository usuarioRepo;

    public TechoPresupuestalService(TechoPresupuestalRepository techoRepo,
            UsuarioRepository usuarioRepo) {
        this.techoRepo = techoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    public List<TechoPresupuestal> listar() {
        return techoRepo.findAll(Sort.by(Sort.Direction.DESC, "año"));
    }

    @Transactional
    public TechoPresupuestal crear(Integer año, BigDecimal montoTotal, Long creadoPorId) {
        if (techoRepo.findByAño(año).isPresent()) {
            throw new BusinessException("Ya existe un techo para el año " + año);
        }
        Usuario creador = usuarioRepo.findById(creadoPorId)
                .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

        TechoPresupuestal t = new TechoPresupuestal();
        t.setAño(año);
        t.setMontoTotal(montoTotal);
        t.setCreadoPor(creador);
        return techoRepo.save(t);
    }

    @Transactional
    public TechoPresupuestal editar(Long id, Integer año, BigDecimal montoTotal) {
        TechoPresupuestal t = techoRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Techo no encontrado"));

        var existente = techoRepo.findByAño(año);
        if (existente.isPresent() && !existente.get().getId().equals(id)) {
            throw new BusinessException("Ya existe un techo para el año " + año);
        }

        t.setAño(año);
        t.setMontoTotal(montoTotal);
        return techoRepo.save(t);
    }

    @Transactional
    public void toggleActivo(Long id) {
        TechoPresupuestal t = techoRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Techo no encontrado"));
        t.setActivo(!t.getActivo());
        techoRepo.save(t);
    }

    @Transactional
    public void togglePlanificado(Long id) {
        TechoPresupuestal t = techoRepo.findById(id)
                .orElseThrow(() -> new BusinessException("Techo no encontrado"));
        if (!t.getPlanificado() && t.getMontoUtilizado().compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException("No se puede cerrar un techo con monto utilizado");
        }
        t.setPlanificado(!t.getPlanificado());
        techoRepo.save(t);
    }
}
