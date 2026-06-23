package com.upla.sisexp.api;

import com.upla.sisexp.security.CustomUserDetails;
import com.upla.sisexp.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class ApiNotificacionController {

    private final NotificacionService notificacionService;

    public ApiNotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<?> listar(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(
                notificacionService.listarPorUsuario(user.getId()));
    }

    @GetMapping("/no-leidas")
    public ResponseEntity<?> contarNoLeidas(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(Map.of("count",
                notificacionService.contarNoLeidas(user.getId())));
    }

    @GetMapping("/count")
    public ResponseEntity<?> contarNoLeidasAlias(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(Map.of("count",
                notificacionService.contarNoLeidas(user.getId())));
    }

    @PostMapping("/{id}/leida")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<?> marcarLeidaPut(@PathVariable Long id) {
        return ResponseEntity.ok(notificacionService.marcarLeida(id));
    }

    @PostMapping("/marcar-todas-leidas")
    public ResponseEntity<?> marcarTodasLeidas(@AuthenticationPrincipal CustomUserDetails user) {
        notificacionService.marcarTodasLeidas(user.getId());
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @PutMapping("/read-all")
    public ResponseEntity<?> marcarTodasLeidasPut(@AuthenticationPrincipal CustomUserDetails user) {
        notificacionService.marcarTodasLeidas(user.getId());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
