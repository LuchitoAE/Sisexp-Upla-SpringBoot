package com.upla.sisexp.controller;

import com.upla.sisexp.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("kpis", dashboardService.getKPIs());
        model.addAttribute("saldos", dashboardService.getSaldos());
        model.addAttribute("alertas", dashboardService.getAlertas());
        return "dashboard";
    }

    @GetMapping("/horario-cerrado")
    public String horarioCerrado() {
        return "horario-cerrado";
    }
}
