package com.upla.sisexp.auth.controller;

import com.upla.sisexp.auth.model.Usuario;
import com.upla.sisexp.auth.repository.UsuarioRepository;
import com.upla.sisexp.auth.service.AuthService;
import com.upla.sisexp.common.exception.BusinessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class ApiAuthController {

    private final AuthService authService;
    private final UsuarioRepository usuarioRepo;

    public ApiAuthController(AuthService authService, UsuarioRepository usuarioRepo) {
        this.authService = authService;
        this.usuarioRepo = usuarioRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            return ResponseEntity.ok(authService.login(body.get("email"), body.get("password")));
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal Usuario usuario) {
        if (usuario == null) return ResponseEntity.status(401).body(Map.of("error", "No autenticado"));
        return ResponseEntity.ok(Map.of(
            "id", usuario.getId(), "nombre", usuario.getNombre(),
            "email", usuario.getEmail(), "rol", usuario.getRol().name(),
            "activo", usuario.getActivo(), "horarioRestringido", usuario.getHorarioRestringido()
        ));
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validate(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of("valid", false));
        }
        String token = authHeader.substring(7);
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
}
