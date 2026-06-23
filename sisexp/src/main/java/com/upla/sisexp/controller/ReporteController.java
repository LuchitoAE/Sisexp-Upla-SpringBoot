package com.upla.sisexp.controller;

import com.upla.sisexp.service.ReporteService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

@Controller
@RequestMapping("/reportes")
public class ReporteController {

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion','Director','Decanato')")
    public String index(Model model) {
        model.addAttribute("expedientes", reporteService.expedientes());
        model.addAttribute("poi", reporteService.poiGeneral());
        model.addAttribute("pap", reporteService.papGeneral());
        model.addAttribute("techos", reporteService.informeAnual());
        model.addAttribute("conteoEstados", reporteService.getConteoPorEstado());
        return "reportes/index";
    }

    @GetMapping("/export/expedientes")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion','Director','Decanato')")
    public void exportarExpedientes(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=expedientes.csv");
        PrintWriter w = response.getWriter();
        w.println("Codigo,Actividad,Item,Solicitante,Estado,Urgencia,Costo,Fecha Limite");
        for (var exp : reporteService.expedientes()) {
            w.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csv(exp.getCodigo()),
                    csv(exp.getActividadPOI().getCodigo()),
                    csv(exp.getNecesidadPAP().getNombre()),
                    csv(exp.getSolicitante().getNombre()),
                    exp.getEstado(),
                    exp.getUrgencia(),
                    exp.getCostoEstimado(),
                    exp.getFechaLimite() != null ? exp.getFechaLimite().toString() : "");
        }
    }

    @GetMapping("/export/poi")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion','Director','Decanato')")
    public void exportarPOI(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=poi.csv");
        PrintWriter w = response.getWriter();
        w.println("Codigo,Nombre,Techo,Presupuesto,Comprometido,Ejecutado,Disponible,% Ejecucion,Estado");
        for (var item : reporteService.poiGeneral()) {
            w.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csv((String) item.get("codigo")),
                    csv((String) item.get("nombre")),
                    item.get("techoAño"),
                    item.get("presupuesto"),
                    item.get("saldoComprometido"),
                    item.get("saldoEjecutado"),
                    item.get("disponible"),
                    item.get("pctEjecucion"),
                    item.get("estado"));
        }
    }

    @GetMapping("/export/pap")
    @PreAuthorize("hasAnyRole('Administrador','Coordinacion','Director','Decanato')")
    public void exportarPAP(HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=pap.csv");
        PrintWriter w = response.getWriter();
        w.println("Nombre,Actividad,Cantidad,Precio,Tipo,Disponible,Ejecutado,Monto Disp.");
        for (var item : reporteService.papGeneral()) {
            w.printf("%s,%s,%s,%s,%s,%s,%s,%s%n",
                    csv((String) item.get("nombre")),
                    csv((String) item.get("actividadCodigo")),
                    item.get("cantidad"),
                    item.get("precioEstimado"),
                    item.get("tipo"),
                    item.get("cantidadDisponible"),
                    item.get("cantidadEjecutada"),
                    item.get("montoDisponible"));
        }
    }

    private String csv(String val) {
        if (val == null) return "";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
