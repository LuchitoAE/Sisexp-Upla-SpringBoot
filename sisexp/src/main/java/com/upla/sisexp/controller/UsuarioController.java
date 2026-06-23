package com.upla.sisexp.controller;

import com.upla.sisexp.enums.RolUsuario;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.service.UsuarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('Administrador')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("roles", RolUsuario.values());
        return "usuarios/lista";
    }

    @PostMapping
    public String crear(@RequestParam String nombre, @RequestParam String email,
            @RequestParam String password, @RequestParam RolUsuario rol,
            @RequestParam(defaultValue = "true") boolean horarioRestringido,
            RedirectAttributes redirect) {
        try {
            usuarioService.crear(nombre, email, password, rol, horarioRestringido);
            redirect.addFlashAttribute("success", "Usuario creado correctamente");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/editar")
    public String editar(@PathVariable Long id, @RequestParam String nombre,
            @RequestParam String email, @RequestParam RolUsuario rol,
            @RequestParam(defaultValue = "true") boolean horarioRestringido,
            RedirectAttributes redirect) {
        try {
            usuarioService.editar(id, nombre, email, rol, horarioRestringido);
            redirect.addFlashAttribute("success", "Usuario actualizado");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/password")
    public String cambiarPassword(@PathVariable Long id, @RequestParam String nuevaPassword,
            RedirectAttributes redirect) {
        usuarioService.cambiarPassword(id, nuevaPassword);
        redirect.addFlashAttribute("success", "Contraseña actualizada");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/toggle")
    public String toggleActivo(@PathVariable Long id, RedirectAttributes redirect) {
        usuarioService.toggleActivo(id);
        redirect.addFlashAttribute("success", "Estado modificado");
        return "redirect:/usuarios";
    }
}
