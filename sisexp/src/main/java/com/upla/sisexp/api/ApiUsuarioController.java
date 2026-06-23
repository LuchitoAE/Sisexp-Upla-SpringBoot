package com.upla.sisexp.api;

import com.upla.sisexp.enums.RolUsuario;
import com.upla.sisexp.exception.BusinessException;
import com.upla.sisexp.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('Administrador')")
public class ApiUsuarioController {

    private final UsuarioService usuarioService;

    public ApiUsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public ResponseEntity<?> listar() {
        return ResponseEntity.ok(usuarioService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return usuarioService.listar().stream()
                .filter(u -> u.getId().equals(id))
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Map<String, Object> body) {
        try {
            var u = usuarioService.crear(
                    (String) body.get("nombre"),
                    (String) body.get("email"),
                    (String) body.get("password"),
                    RolUsuario.valueOf((String) body.get("rol")),
                    body.containsKey("horarioRestringido")
                            ? Boolean.parseBoolean(body.get("horarioRestringido").toString()) : true);
            return ResponseEntity.ok(u);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editar(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            var usuarioExistente = usuarioService.listar().stream()
                    .filter(u -> u.getId().equals(id)).findFirst()
                    .orElseThrow(() -> new BusinessException("Usuario no encontrado"));

            String nombre = body.containsKey("nombre") && body.get("nombre") != null
                    ? (String) body.get("nombre") : usuarioExistente.getNombre();
            String email = body.containsKey("email") && body.get("email") != null
                    ? (String) body.get("email") : usuarioExistente.getEmail();
            RolUsuario rol = body.containsKey("rol") && body.get("rol") != null
                    ? RolUsuario.valueOf((String) body.get("rol")) : usuarioExistente.getRol();
            boolean horarioRestringido = body.containsKey("horarioRestringido")
                    ? Boolean.parseBoolean(body.get("horarioRestringido").toString())
                    : usuarioExistente.getHorarioRestringido();

            var u = usuarioService.editar(id, nombre, email, rol, horarioRestringido);
            return ResponseEntity.ok(u);
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/cambiar-password")
    public ResponseEntity<?> cambiarPassword(@PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            usuarioService.cambiarPassword(id, (String) body.get("password"));
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivoPut(@PathVariable Long id) {
        return _toggleActivo(id);
    }

    @PatchMapping("/{id}/toggle-activo")
    public ResponseEntity<?> toggleActivoPatch(@PathVariable Long id) {
        return _toggleActivo(id);
    }

    private ResponseEntity<?> _toggleActivo(Long id) {
        try {
            usuarioService.toggleActivo(id);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
