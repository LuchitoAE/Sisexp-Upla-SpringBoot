package com.upla.sisexp.controller;

import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.NotificacionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping("/notificaciones")
    @Transactional(readOnly = true)
    public String listar(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("notificaciones", notificacionService.listarPorUsuario(user.getId()));
        return "notificaciones/lista";
    }

    @PostMapping("/notificaciones/{id}/leida")
    public String marcarLeida(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails user) {
        notificacionService.marcarLeida(id);
        return "redirect:/notificaciones";
    }

    @PostMapping("/notificaciones/marcar-todas")
    public String marcarTodas(@AuthenticationPrincipal CustomUserDetails user) {
        notificacionService.marcarTodasLeidas(user.getId());
        return "redirect:/notificaciones";
    }

}
