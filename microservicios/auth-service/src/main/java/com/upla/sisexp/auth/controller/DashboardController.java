package com.upla.sisexp.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @GetMapping("/alertas")
    public Map<String, Object> alertas() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", 0);
        result.put("verdes", 0);
        result.put("amarillas", 0);
        result.put("rojas", 0);
        result.put("items", List.of());
        return result;
    }

    @GetMapping("/saldos")
    public Map<String, Object> saldos() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("techos", List.of());
        result.put("actividades", List.of());
        return result;
    }
}
