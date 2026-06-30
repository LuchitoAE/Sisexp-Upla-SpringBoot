package com.upla.sisexp.expediente.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    @GetMapping
    public Map<String, Object> index() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("expedientes", List.of());
        result.put("conteo", List.of());
        return result;
    }
}
