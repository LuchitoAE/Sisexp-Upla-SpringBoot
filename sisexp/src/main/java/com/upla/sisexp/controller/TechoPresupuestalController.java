package com.upla.sisexp.controller;

import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.TechoPresupuestalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/techos")
public class TechoPresupuestalController {

    private final TechoPresupuestalService techoService;

    public TechoPresupuestalController(TechoPresupuestalService techoService) {
        this.techoService = techoService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("techos", techoService.listar());
        return "techos/lista";
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String crear(@RequestParam Integer año,
            @RequestParam BigDecimal montoTotal,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirect) {
        try {
            techoService.crear(año, montoTotal, user.getId());
            redirect.addFlashAttribute("success", "Techo " + año + " creado correctamente");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/techos";
    }

    @PostMapping("/{id}/editar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String editar(@PathVariable Long id,
            @RequestParam Integer año,
            @RequestParam BigDecimal montoTotal,
            RedirectAttributes redirect) {
        try {
            techoService.editar(id, año, montoTotal);
            redirect.addFlashAttribute("success", "Techo " + año + " actualizado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/techos";
    }

    @PostMapping("/{id}/toggle")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String toggleActivo(@PathVariable Long id, RedirectAttributes redirect) {
        techoService.toggleActivo(id);
        redirect.addFlashAttribute("success", "Estado modificado");
        return "redirect:/techos";
    }

    @PostMapping("/{id}/planificar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String togglePlanificado(@PathVariable Long id, RedirectAttributes redirect) {
        try {
            techoService.togglePlanificado(id);
            redirect.addFlashAttribute("success", "Estado de planificacion modificado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/techos";
    }
}
