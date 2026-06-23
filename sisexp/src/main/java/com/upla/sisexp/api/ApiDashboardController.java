package com.upla.sisexp.api;

import com.upla.sisexp.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class ApiDashboardController {

    private final DashboardService dashboardService;

    public ApiDashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<?> kpis() {
        return ResponseEntity.ok(dashboardService.getKPIs());
    }

    @GetMapping("/saldos")
    public ResponseEntity<?> saldos() {
        return ResponseEntity.ok(dashboardService.getSaldos());
    }

    @GetMapping("/alertas")
    public ResponseEntity<?> alertas() {
        return ResponseEntity.ok(dashboardService.getAlertas());
    }
}
