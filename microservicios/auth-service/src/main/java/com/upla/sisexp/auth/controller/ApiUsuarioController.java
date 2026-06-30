package com.upla.sisexp.auth.controller;

import com.upla.sisexp.auth.model.Usuario;
import com.upla.sisexp.auth.service.UsuarioService;
import com.upla.sisexp.common.enums.RolUsuario;
import com.upla.sisexp.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class ApiUsuarioController {

    private final UsuarioService usuarioService;

    public ApiUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(usuarioService.obtener(id));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            Usuario u = usuarioService.crear(
                (String) body.get("nombre"),
                (String) body.get("email"),
                (String) body.get("password"),
                RolUsuario.valueOf((String) body.get("rol")),
                body.get("horarioRestringido") != null ? (Boolean) body.get("horarioRestringido") : true
            );
            return ResponseEntity.ok(u);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Usuario u = usuarioService.editar(id,
                (String) body.get("nombre"),
                (String) body.get("email"),
                body.get("rol") != null ? RolUsuario.valueOf((String) body.get("rol")) : null,
                body.get("horarioRestringido") != null ? (Boolean) body.get("horarioRestringido") : true
            );
            return ResponseEntity.ok(u);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivo(@PathVariable Long id) {
        try {
            usuarioService.toggleActivo(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        try {
            usuarioService.cambiarPassword(id, body.get("password"));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
