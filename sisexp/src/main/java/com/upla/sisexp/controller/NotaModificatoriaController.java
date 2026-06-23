package com.upla.sisexp.controller;

import com.upla.sisexp.enums.Naturaleza;
import com.upla.sisexp.enums.TipoNota;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.ActividadPOIService;
import com.upla.sisexp.service.NotaModificatoriaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/notas")
public class NotaModificatoriaController {

    private final NotaModificatoriaService notaService;
    private final ActividadPOIService actividadService;

    public NotaModificatoriaController(NotaModificatoriaService notaService,
            ActividadPOIService actividadService) {
        this.notaService = notaService;
        this.actividadService = actividadService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("notas", notaService.listar());
        model.addAttribute("actividades", actividadService.listar());
        model.addAttribute("tiposNota", TipoNota.values());
        model.addAttribute("naturalezas", Naturaleza.values());
        return "notas/lista";
    }

    @PostMapping
    public String crear(@RequestParam TipoNota tipo, @RequestParam Long actividadExistenteId,
            @RequestParam String nuevoNombre, @RequestParam String justificacion,
            @RequestParam(required = false) BigDecimal costoEstimado,
            @RequestParam(required = false) MultipartFile archivo,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirect) {
        try {
            notaService.crear(tipo, actividadExistenteId, nuevoNombre, justificacion,
                    costoEstimado, archivo, user.getId());
            redirect.addFlashAttribute("success", "Nota modificatoria creada");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notas";
    }

    @PostMapping("/{id}/configurar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String configurar(@PathVariable Long id, @RequestParam Long actividadOrigenId,
            @RequestParam BigDecimal montoTransferir,
            @RequestParam(required = false) String clasificadorGasto,
            @RequestParam Naturaleza tipo, @RequestParam(required = false) String observacion,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirect) {
        try {
            notaService.configurar(id, actividadOrigenId, montoTransferir,
                    clasificadorGasto, tipo, observacion, user.getId());
            redirect.addFlashAttribute("success", "Nota configurada correctamente");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notas";
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion')")
    public String rechazar(@PathVariable Long id, @RequestParam String observacion,
            @AuthenticationPrincipal CustomUserDetails user,
            RedirectAttributes redirect) {
        try {
            notaService.rechazar(id, observacion, user.getId());
            redirect.addFlashAttribute("success", "Nota rechazada");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/notas";
    }
}
