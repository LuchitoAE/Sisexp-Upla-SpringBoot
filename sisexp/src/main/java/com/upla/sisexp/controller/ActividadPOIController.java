package com.upla.sisexp.controller;

import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.service.ActividadPOIService;
import com.upla.sisexp.service.TechoPresupuestalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/actividades")
public class ActividadPOIController {

    private final ActividadPOIService actividadService;
    private final TechoPresupuestalService techoService;

    public ActividadPOIController(ActividadPOIService actividadService,
            TechoPresupuestalService techoService) {
        this.actividadService = actividadService;
        this.techoService = techoService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listar(@RequestParam(required = false) Long techoId, Model model) {
        if (techoId != null) {
            model.addAttribute("actividades", actividadService.listarPorTecho(techoId));
        } else {
            model.addAttribute("actividades", actividadService.listar());
        }
        model.addAttribute("techos", techoService.listar());
        model.addAttribute("techoSeleccionado", techoId);
        return "poi/lista";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String crear(@RequestParam String codigo, @RequestParam String nombre,
            @RequestParam BigDecimal presupuesto,
            @RequestParam(required = false) LocalDate fechaLimite,
            @RequestParam(required = false) Long techoId, RedirectAttributes redirect) {
        try {
            actividadService.crear(codigo, nombre, presupuesto, fechaLimite, techoId);
            redirect.addFlashAttribute("success", "Actividad " + codigo + " creada");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actividades";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String editar(@PathVariable Long id, @RequestParam String codigo,
            @RequestParam String nombre, @RequestParam BigDecimal presupuesto,
            @RequestParam(required = false) LocalDate fechaLimite,
            RedirectAttributes redirect) {
        try {
            actividadService.editar(id, codigo, nombre, presupuesto, fechaLimite);
            redirect.addFlashAttribute("success", "Actividad actualizada");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/actividades";
    }
}
