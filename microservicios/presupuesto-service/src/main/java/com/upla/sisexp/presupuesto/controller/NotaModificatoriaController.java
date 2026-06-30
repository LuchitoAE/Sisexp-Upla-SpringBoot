package com.upla.sisexp.presupuesto.controller;

import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notas-modificatorias")
public class NotaModificatoriaController {

    @GetMapping
    public Map<String, Object> listar() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("notas", List.of());
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("error", "No implementado aun");
        return result;
    }

    @PostMapping
    public Map<String, Object> crear(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("error", "No implementado aun");
        return result;
    }
}
