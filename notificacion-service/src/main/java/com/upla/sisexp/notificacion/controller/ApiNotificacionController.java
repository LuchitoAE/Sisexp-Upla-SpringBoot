package com.upla.sisexp.notificacion.controller;

import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.notificacion.service.NotificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
public class ApiNotificacionController {
    private final NotificacionService notificacionService;
    public ApiNotificacionController(NotificacionService notificacionService) { this.notificacionService = notificacionService; }

    @GetMapping
    public ResponseEntity<?> listar(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(notificacionService.listarPorUsuario(usuarioId));
    }

    @GetMapping("/count")
    public ResponseEntity<?> count(@RequestParam Long usuarioId) {
        return ResponseEntity.ok(Map.of("count", notificacionService.contarNoLeidas(usuarioId)));
    }

    @PutMapping("/{id}/leer")
    public ResponseEntity<?> marcarLeida(@PathVariable Long id) {
        try { notificacionService.marcarLeida(id); return ResponseEntity.ok(Map.of("ok", true)); }
        catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @PutMapping("/leer-todas")
    public ResponseEntity<?> marcarTodas(@RequestParam Long usuarioId) {
        notificacionService.marcarTodasLeidas(usuarioId);
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
