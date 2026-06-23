package com.upla.sisexp.controller;

import com.upla.sisexp.model.Expediente;
import com.upla.sisexp.repository.ExpedienteRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/rastreo")
public class RastreoController {

    private final ExpedienteRepository expedienteRepo;

    public RastreoController(ExpedienteRepository expedienteRepo) {
        this.expedienteRepo = expedienteRepo;
    }

    @GetMapping
    public String mostrarFormulario() {
        return "rastreo";
    }

    @GetMapping("/buscar")
    public String buscar(@RequestParam String codigo, Model model) {
        Optional<Expediente> exp = expedienteRepo.findByCodigo(codigo.toUpperCase());
        if (exp.isPresent()) {
            model.addAttribute("expediente", exp.get());
        } else {
            model.addAttribute("noEncontrado", true);
        }
        model.addAttribute("codigoBuscado", codigo);
        return "rastreo";
    }
}
