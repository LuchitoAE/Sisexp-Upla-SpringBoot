package com.upla.sisexp.expediente.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class ResetController {

    @PersistenceContext
    private EntityManager em;
    private final ApplicationContext ctx;

    public ResetController(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @PostMapping("/reset-expedientes")
    @Transactional
    public ResponseEntity<?> reset() {
        try {
            em.createNativeQuery("DELETE FROM seguimiento_logs").executeUpdate();
            em.createNativeQuery("DELETE FROM documentos_adjuntos").executeUpdate();
            em.createNativeQuery("DELETE FROM expedientes").executeUpdate();

            com.upla.sisexp.expediente.config.DataInitializer seeder = ctx.getBean(com.upla.sisexp.expediente.config.DataInitializer.class);
            seeder.run();
            return ResponseEntity.ok(Map.of("ok", true, "message", "DB expedientes reseteada y re-sembrada"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
