package com.upla.sisexp.presupuesto.controller;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.context.ApplicationContext;

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

    @PostMapping("/reset-presupuesto")
    @Transactional
    public ResponseEntity<?> reset() {
        try {
            em.createNativeQuery("DELETE FROM necesidades_pap").executeUpdate();
            em.createNativeQuery("DELETE FROM notas_modificatorias").executeUpdate();
            em.createNativeQuery("DELETE FROM actividades_poi").executeUpdate();
            em.createNativeQuery("DELETE FROM techos_presupuestales").executeUpdate();

            com.upla.sisexp.presupuesto.config.DataInitializer seeder = ctx.getBean(com.upla.sisexp.presupuesto.config.DataInitializer.class);
            seeder.run();
            return ResponseEntity.ok(Map.of("ok", true, "message", "DB reseteada y re-sembrada"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
