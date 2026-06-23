package com.upla.sisexp.controller;

import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.service.ActividadPOIService;
import com.upla.sisexp.service.NecesidadPAPService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/necesidades")
public class NecesidadPAPController {

    private final NecesidadPAPService necesidadService;
    private final ActividadPOIService actividadService;

    public NecesidadPAPController(NecesidadPAPService necesidadService,
            ActividadPOIService actividadService) {
        this.necesidadService = necesidadService;
        this.actividadService = actividadService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listar(@RequestParam(required = false) Long actividadId, Model model) {
        if (actividadId != null) {
            model.addAttribute("necesidades", necesidadService.listarPorActividad(actividadId));
        } else {
            model.addAttribute("necesidades", necesidadService.listar());
        }
        model.addAttribute("actividades", actividadService.listar());
        model.addAttribute("actividadSeleccionada", actividadId);
        model.addAttribute("naturalezas", Naturaleza.values());
        return "pap/lista";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String crear(@RequestParam String nombre, @RequestParam Integer cantidad,
            @RequestParam BigDecimal precioEstimado, @RequestParam(required = false) String unidad,
            @RequestParam(required = false) String oficinaLaboratorio,
            @RequestParam Naturaleza tipo, @RequestParam(required = false) String clasificadorGasto,
            @RequestParam Long actividadPoiId, RedirectAttributes redirect) {
        try {
            necesidadService.crear(nombre, cantidad, precioEstimado, unidad,
                    oficinaLaboratorio, tipo, clasificadorGasto, actividadPoiId);
            redirect.addFlashAttribute("success", "Necesidad PAP creada");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/necesidades";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String editar(@PathVariable Long id, @RequestParam String nombre,
            @RequestParam Integer cantidad, @RequestParam BigDecimal precioEstimado,
            @RequestParam(required = false) String unidad,
            @RequestParam(required = false) String oficinaLaboratorio,
            @RequestParam Naturaleza tipo, @RequestParam(required = false) String clasificadorGasto,
            RedirectAttributes redirect) {
        try {
            necesidadService.editar(id, nombre, cantidad, precioEstimado, unidad,
                    oficinaLaboratorio, tipo, clasificadorGasto);
            redirect.addFlashAttribute("success", "Necesidad actualizada");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/necesidades";
    }
}
