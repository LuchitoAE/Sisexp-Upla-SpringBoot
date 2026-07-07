package com.upla.sisexp.presupuesto.controller;

import com.upla.sisexp.common.exception.BusinessException;
import com.upla.sisexp.presupuesto.service.BusinessRulesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/internal/saldos")
public class ApiSaldoInternoController {
    private final BusinessRulesService businessRules;
    public ApiSaldoInternoController(BusinessRulesService businessRules) { this.businessRules = businessRules; }

    @PostMapping("/reservar")
    public ResponseEntity<?> reservar(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("actividadPoiId") == null) return ResponseEntity.badRequest().body(Map.of("error", "actividadPoiId requerido"));
            Long poiId = Long.valueOf(body.get("actividadPoiId").toString());
            BigDecimal costo = body.get("costo") != null ? new BigDecimal(body.get("costo").toString()) : BigDecimal.ZERO;
            Long papId = body.get("necesidadPapId") != null ? Long.valueOf(body.get("necesidadPapId").toString()) : null;
            int cant = body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad").toString()) : 0;
            businessRules.reservarSaldo(poiId, costo);
            if (papId != null) businessRules.reservarSaldoPAP(papId, cant, costo);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (NumberFormatException e) { return ResponseEntity.badRequest().body(Map.of("error", "Formato numerico invalido")); }
    }

    @PostMapping("/ejecutar")
    public ResponseEntity<?> ejecutar(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("actividadPoiId") == null) return ResponseEntity.badRequest().body(Map.of("error", "actividadPoiId requerido"));
            Long poiId = Long.valueOf(body.get("actividadPoiId").toString());
            BigDecimal costo = body.get("costo") != null ? new BigDecimal(body.get("costo").toString()) : BigDecimal.ZERO;
            Long papId = body.get("necesidadPapId") != null ? Long.valueOf(body.get("necesidadPapId").toString()) : null;
            int cant = body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad").toString()) : 0;
            businessRules.ejecutarSaldo(poiId, costo, papId, cant);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (NumberFormatException e) { return ResponseEntity.badRequest().body(Map.of("error", "Formato numerico invalido")); }
    }

    @PostMapping("/liberar")
    public ResponseEntity<?> liberar(@RequestBody Map<String, Object> body) {
        try {
            if (body.get("actividadPoiId") == null) return ResponseEntity.badRequest().body(Map.of("error", "actividadPoiId requerido"));
            Long poiId = Long.valueOf(body.get("actividadPoiId").toString());
            BigDecimal costo = body.get("costo") != null ? new BigDecimal(body.get("costo").toString()) : BigDecimal.ZERO;
            Long papId = body.get("necesidadPapId") != null ? Long.valueOf(body.get("necesidadPapId").toString()) : null;
            int cant = body.get("cantidad") != null ? Integer.parseInt(body.get("cantidad").toString()) : 0;
            businessRules.liberarSaldo(poiId, costo);
            if (papId != null) businessRules.liberarSaldoPAP(papId, cant, costo);
            return ResponseEntity.ok(Map.of("ok", true));
        } catch (BusinessException e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
        catch (NumberFormatException e) { return ResponseEntity.badRequest().body(Map.of("error", "Formato numerico invalido")); }
    }
}
