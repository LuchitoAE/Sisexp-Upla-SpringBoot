package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class ExportService {

    private final TechoPresupuestalRepository techoRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final RestTemplate restTemplate;

    public ExportService(TechoPresupuestalRepository techoRepo, ActividadPOIRepository actividadRepo,
                         NecesidadPAPRepository necesidadRepo, RestTemplate restTemplate) {
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.restTemplate = restTemplate;
    }

    // ==================== EXCEL ====================

    public byte[] exportarExcelAnual(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle hdr = headerStyle(wb);
            CellStyle num = numberStyle(wb);
            CellStyle title = titleStyle(wb);
            CellStyle subtitle = subtitleStyle(wb);
            CellStyle totalRow = totalStyle(wb);

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            BigDecimal montoTotal = techo != null ? techo.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal ejercido = techo != null ? techo.getMontoUtilizado() : BigDecimal.ZERO;
            BigDecimal saldo = montoTotal.subtract(ejercido);

            Sheet s = wb.createSheet("Informe Anual " + anio);

            Row r0 = s.createRow(0);
            r0.createCell(0).setCellValue("UNIVERSIDAD PERUANA LOS ANDES");
            r0.getCell(0).setCellStyle(title);
            s.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));

            Row r0b = s.createRow(1);
            r0b.createCell(0).setCellValue("SISEXP-UPLA — Sistema de Gestion de Expedientes");
            r0b.getCell(0).setCellStyle(subtitle);
            s.addMergedRegion(new CellRangeAddress(1, 1, 0, 5));

            Row r0c = s.createRow(2);
            r0c.createCell(0).setCellValue("Informe Anual " + anio + " — Generado: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            r0c.getCell(0).setCellStyle(dataStyle(wb));
            s.addMergedRegion(new CellRangeAddress(2, 2, 0, 5));

            int row = 4;
            Row rh = s.createRow(row++);
            rh.createCell(0).setCellValue("Indicador"); rh.getCell(0).setCellStyle(hdr);
            rh.createCell(1).setCellValue("Valor"); rh.getCell(1).setCellStyle(hdr);
            s.setColumnWidth(0, 15000); s.setColumnWidth(1, 8000);

            row = xlRow(s, row, "Presupuesto Total", fmt(montoTotal), totalRow);
            row = xlRow(s, row, "Ejecutado (" + (montoTotal.compareTo(BigDecimal.ZERO) > 0
                ? Math.round(ejercido.divide(montoTotal, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100) : 0) + "%)",
                fmt(ejercido), num);
            row = xlRow(s, row, "Disponible", fmt(saldo), null);

            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            row = xlRow(s, row, "Actividades POI", String.valueOf(actividades.size()), null);
            row = xlRow(s, row, "Pendientes", String.valueOf(actividades.stream().filter(a -> !a.getPlanificado()).count()), null);
            row = xlRow(s, row, "Planificadas", String.valueOf(actividades.stream().filter(ActividadPOI::getPlanificado).count()), null);

            int totalExp = 0;
            try {
                List<Map<String, Object>> exps = restTemplate.exchange(
                    "http://expediente-service:8083/api/expedientes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (exps != null) {
                    for (Map<String, Object> e : exps) {
                        String codigo = (String) e.get("codigo");
                        if (codigo != null && codigo.contains("-" + anio + "-")) totalExp++;
                    }
                }
            } catch (Exception ignored) {}
            xlRow(s, row, "Expedientes del ano", String.valueOf(totalExp), null);

            Sheet s2 = wb.createSheet("Actividades POI");
            Row s2h = s2.createRow(0);
            String[] ah = {"Codigo","Nombre","Estado","Presupuesto (S/)","Ejecutado (S/)","Comprometido (S/)","Disponible (S/)","PAP"};
            for (int i = 0; i < ah.length; i++) { s2h.createCell(i).setCellValue(ah[i]); s2h.getCell(i).setCellStyle(hdr); }
            s2.setColumnWidth(0, 5000); s2.setColumnWidth(1, 11000); s2.setColumnWidth(2, 4500);
            for (int i = 3; i < 8; i++) s2.setColumnWidth(i, 4800);

            int ar = 1;
            for (ActividadPOI a : actividades) {
                Row rw = s2.createRow(ar++);
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                rw.createCell(0).setCellValue(a.getCodigo());
                rw.createCell(1).setCellValue(a.getNombre());
                rw.createCell(2).setCellValue(a.getEstado().name());
                xlCell(rw, 3, a.getPresupuestoAsignado().doubleValue(), num);
                xlCell(rw, 4, a.getSaldoEjecutado().doubleValue(), num);
                xlCell(rw, 5, a.getSaldoComprometido().doubleValue(), num);
                xlCell(rw, 6, disp.doubleValue(), num);
                rw.createCell(7).setCellValue(a.getPlanificado() ? "Cerrado" : "Abierto");
                if (ar % 2 == 0) for (int i = 0; i < 8; i++) rw.getCell(i).setCellStyle(stripeStyle(wb, num));
            }
            s2.createFreezePane(0, 1);

            Row ft = s2.createRow(ar + 1);
            ft.createCell(0).setCellValue("SISEXP-UPLA — Universidad Peruana Los Andes");
            ft.getCell(0).setCellStyle(subtitle);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel anual: " + e.getMessage(), e); }
    }

    public byte[] exportarExcelExpedientes(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle hdr = headerStyle(wb); CellStyle num = numberStyle(wb);
            Sheet s = wb.createSheet("Expedientes " + anio);
            String[] h = {"Codigo","Estado","Urgencia","Naturaleza","Cant. Sol.","Costo (S/)","Descripcion"};
            Row th = s.createRow(0);
            for (int i = 0; i < h.length; i++) { th.createCell(i).setCellValue(h[i]); th.getCell(i).setCellStyle(hdr); }
            s.setColumnWidth(0, 5000); s.setColumnWidth(1, 4000); s.setColumnWidth(2, 4000);
            s.setColumnWidth(3, 4000); s.setColumnWidth(4, 3500); s.setColumnWidth(5, 4500);
            s.setColumnWidth(6, 15000); s.createFreezePane(0, 1);

            int r = 1;
            try {
                List<Map<String, Object>> exps = restTemplate.exchange(
                    "http://expediente-service:8083/api/expedientes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (exps != null) {
                    for (Map<String, Object> e : exps) {
                        String codigo = (String) e.get("codigo");
                        if (codigo == null || !codigo.contains("-" + anio + "-")) continue;
                        Row rw = s.createRow(r++);
                        rw.createCell(0).setCellValue(codigo);
                        rw.createCell(1).setCellValue(String.valueOf(e.get("estado")));
                        rw.createCell(2).setCellValue(String.valueOf(e.get("urgencia")));
                        rw.createCell(3).setCellValue(String.valueOf(e.get("naturaleza")));
                        rw.createCell(4).setCellValue(String.valueOf(e.getOrDefault("cantidadSolicitada", "")));
                        Object costo = e.get("costoEstimado");
                        xlCell(rw, 5, costo != null ? ((Number)costo).doubleValue() : 0, num);
                        rw.createCell(6).setCellValue(String.valueOf(e.getOrDefault("descripcion", "")));
                    }
                }
            } catch (Exception ignored) {}
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel expedientes: " + e.getMessage(), e); }
    }

    public byte[] exportarExcelPOI(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle hdr = headerStyle(wb); CellStyle num = numberStyle(wb);
            Sheet s = wb.createSheet("POI General " + anio);
            String[] h = {"Codigo","Nombre","Estado","Presupuesto (S/)","Ejecutado (S/)","Comprometido (S/)","Disponible (S/)","PAP"};
            Row th = s.createRow(0);
            for (int i = 0; i < h.length; i++) { th.createCell(i).setCellValue(h[i]); th.getCell(i).setCellStyle(hdr); }
            for (int i = 0; i < h.length; i++) s.setColumnWidth(i, 4800);
            s.setColumnWidth(1, 11000); s.createFreezePane(0, 1);

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            int r = 1;
            for (ActividadPOI a : actividades) {
                Row rw = s.createRow(r++);
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                rw.createCell(0).setCellValue(a.getCodigo());
                rw.createCell(1).setCellValue(a.getNombre());
                rw.createCell(2).setCellValue(a.getEstado().name());
                xlCell(rw, 3, a.getPresupuestoAsignado().doubleValue(), num);
                xlCell(rw, 4, a.getSaldoEjecutado().doubleValue(), num);
                xlCell(rw, 5, a.getSaldoComprometido().doubleValue(), num);
                xlCell(rw, 6, disp.doubleValue(), num);
                rw.createCell(7).setCellValue(a.getPlanificado() ? "Cerrado" : "Abierto");
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel POI: " + e.getMessage(), e); }
    }

    public byte[] exportarExcelPAP(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle hdr = headerStyle(wb); CellStyle num = numberStyle(wb);
            Sheet s = wb.createSheet("PAP General " + anio);
            String[] h = {"Item","Actividad","Tipo","Cant.Plan.","Cant.Disp.","Cant.Ejec.","P.Unitario (S/)","Subtotal (S/)"};
            Row th = s.createRow(0);
            for (int i = 0; i < h.length; i++) { th.createCell(i).setCellValue(h[i]); th.getCell(i).setCellStyle(hdr); }
            s.setColumnWidth(0, 12000); s.setColumnWidth(1, 8000);
            for (int i = 2; i < h.length; i++) s.setColumnWidth(i, 4500);
            s.createFreezePane(0, 1);

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            int r = 1;
            for (ActividadPOI act : actividades) {
                for (NecesidadPAP n : necesidadRepo.findByActividadPOIId(act.getId())) {
                    Row rw = s.createRow(r++);
                    rw.createCell(0).setCellValue(n.getNombre());
                    rw.createCell(1).setCellValue(act.getCodigo());
                    rw.createCell(2).setCellValue(n.getTipo().name());
                    rw.createCell(3).setCellValue(n.getCantidad());
                    rw.createCell(4).setCellValue(n.getCantidadDisponible() != null ? n.getCantidadDisponible() : 0);
                    rw.createCell(5).setCellValue(n.getCantidadEjecutada() != null ? n.getCantidadEjecutada() : 0);
                    xlCell(rw, 6, n.getPrecioEstimado().doubleValue(), num);
                    xlCell(rw, 7, n.getPrecioEstimado().multiply(BigDecimal.valueOf(n.getCantidad())).doubleValue(), num);
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel PAP: " + e.getMessage(), e); }
    }

    // ==================== PDF (HTML con marca de agua UPLA) ====================

    public byte[] exportarPDF(String tipo, int anio) {
        String titulo;
        switch (tipo) {
            case "anual": titulo = "Informe Anual " + anio; break;
            case "expedientes": titulo = "Reporte de Expedientes " + anio; break;
            case "poi": titulo = "POI General " + anio; break;
            case "pap": titulo = "PAP General " + anio; break;
            default: titulo = "Reporte " + anio;
        }

        StringBuilder h = new StringBuilder();
        h.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>").append(titulo).append("</title><style>");
        h.append("*{margin:0;padding:0;box-sizing:border-box;}");
        h.append("body{font-family:'Segoe UI',Arial,sans-serif;margin:30px;color:#1e293b;font-size:10pt;");
        h.append("background:#fff;position:relative;min-height:100vh;}");
        h.append("body::before{content:'SISEXP-UPLA';position:fixed;top:50%;left:50%;");
        h.append("transform:translate(-50%,-50%) rotate(-35deg);font-size:90pt;font-weight:900;");
        h.append("color:rgba(30,58,95,0.04);white-space:nowrap;pointer-events:none;z-index:0;}");
        h.append(".header{text-align:center;border-bottom:3px solid #1e3a5f;padding-bottom:12px;margin-bottom:18px;position:relative;z-index:1;}");
        h.append(".header h1{font-size:14pt;color:#1e3a5f;margin:0;text-transform:uppercase;letter-spacing:1px;}");
        h.append(".header .sub{font-size:9pt;color:#64748b;margin-top:3px;}");
        h.append(".header .gold{color:#c9a84c;font-weight:700;}");
        h.append(".kpi-row{display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;position:relative;z-index:1;}");
        h.append(".kpi{flex:1;min-width:100px;background:#f8fafc;border-radius:8px;padding:10px 14px;border-left:3px solid #1e3a5f;}");
        h.append(".kpi-val{font-size:18pt;font-weight:800;color:#0f172a;}");
        h.append(".kpi-label{font-size:8pt;color:#64748b;text-transform:uppercase;letter-spacing:.5px;}");
        h.append("table{width:100%;border-collapse:collapse;margin:10px 0;font-size:9pt;position:relative;z-index:1;}");
        h.append("th{background:#1e3a5f;color:#fff;padding:6px 8px;text-align:left;font-weight:600;font-size:9pt;text-transform:uppercase;letter-spacing:.3px;}");
        h.append("td{padding:5px 8px;border-bottom:1px solid #e2e8f0;}");
        h.append("tr:nth-child(even) td{background:#f8fafc;}");
        h.append(".footer{text-align:center;font-size:8pt;color:#94a3b8;margin-top:24px;");
        h.append("border-top:2px solid #c9a84c;padding-top:10px;position:relative;z-index:1;}");
        h.append("@media print{body{margin:15px;}body::before{font-size:70pt;}.footer{position:fixed;bottom:0;left:0;right:0;}}");
        h.append("</style></head><body>");

        h.append("<div class='header'>");
        h.append("<h1>Universidad Peruana Los Andes</h1>");
        h.append("<div class='sub'><span class='gold'>SISEXP-UPLA</span> — Sistema de Gestion de Expedientes</div>");
        h.append("<div class='sub'>").append(titulo).append(" | Generado: ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</div>");
        h.append("</div>");

        TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
        List<ActividadPOI> actividades = techo != null
            ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();

        switch (tipo) {
            case "anual":
                BigDecimal mt = techo != null ? techo.getMontoTotal() : BigDecimal.ZERO;
                BigDecimal ej = techo != null ? techo.getMontoUtilizado() : BigDecimal.ZERO;
                int pct = mt.compareTo(BigDecimal.ZERO) > 0
                    ? Math.round(ej.divide(mt, 4, java.math.RoundingMode.HALF_UP).floatValue() * 100) : 0;
                h.append("<div class='kpi-row'>");
                h.append("<div class='kpi'><div class='kpi-val'>S/ ").append(fmt(mt)).append("</div><div class='kpi-label'>Presupuesto Total</div></div>");
                h.append("<div class='kpi'><div class='kpi-val'>").append(pct).append("%</div><div class='kpi-label'>Ejecucion</div></div>");
                h.append("<div class='kpi'><div class='kpi-val'>").append(actividades.size()).append("</div><div class='kpi-label'>Actividades POI</div></div>");
                h.append("<div class='kpi'><div class='kpi-val'>S/ ").append(fmt(mt.subtract(ej))).append("</div><div class='kpi-label'>Saldo Disponible</div></div>");
                h.append("</div>");
                h.append("<table><thead><tr><th>Codigo</th><th>Nombre</th><th>Estado</th><th>Presupuesto</th><th>Ejecutado</th><th>Disponible</th><th>PAP</th></tr></thead><tbody>");
                for (ActividadPOI a : actividades) {
                    BigDecimal d = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                    h.append("<tr><td>").append(a.getCodigo()).append("</td><td>").append(a.getNombre()).append("</td>");
                    h.append("<td>").append(a.getEstado().name()).append("</td>");
                    h.append("<td>S/ ").append(fmt(a.getPresupuestoAsignado())).append("</td>");
                    h.append("<td>S/ ").append(fmt(a.getSaldoEjecutado())).append("</td>");
                    h.append("<td>S/ ").append(fmt(d)).append("</td>");
                    h.append("<td>").append(a.getPlanificado() ? "Cerrado" : "Abierto").append("</td></tr>");
                }
                h.append("</tbody></table>");
                break;

            case "expedientes":
                h.append("<table><thead><tr><th>Codigo</th><th>Estado</th><th>Urgencia</th><th>Naturaleza</th><th>Descripcion</th></tr></thead><tbody>");
                try {
                    List<Map<String, Object>> exps = restTemplate.exchange(
                        "http://expediente-service:8083/api/expedientes", HttpMethod.GET, null,
                        new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                    if (exps != null) {
                        for (Map<String, Object> e : exps) {
                            String codigo = (String) e.get("codigo");
                            if (codigo == null || !codigo.contains("-" + anio + "-")) continue;
                            h.append("<tr><td>").append(codigo).append("</td><td>").append(e.get("estado")).append("</td>");
                            h.append("<td>").append(e.get("urgencia")).append("</td><td>").append(e.get("naturaleza")).append("</td>");
                            h.append("<td>").append(e.getOrDefault("descripcion", "")).append("</td></tr>");
                        }
                    }
                } catch (Exception ignored) {}
                h.append("</tbody></table>");
                break;

            case "poi":
                h.append("<table><thead><tr><th>Codigo</th><th>Nombre</th><th>Estado</th><th>Presupuesto</th><th>Ejecutado</th><th>Comprometido</th><th>Disponible</th><th>PAP</th></tr></thead><tbody>");
                for (ActividadPOI a : actividades) {
                    BigDecimal d = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                    h.append("<tr><td>").append(a.getCodigo()).append("</td><td>").append(a.getNombre()).append("</td>");
                    h.append("<td>").append(a.getEstado().name()).append("</td>");
                    h.append("<td>S/ ").append(fmt(a.getPresupuestoAsignado())).append("</td>");
                    h.append("<td>S/ ").append(fmt(a.getSaldoEjecutado())).append("</td>");
                    h.append("<td>S/ ").append(fmt(a.getSaldoComprometido())).append("</td>");
                    h.append("<td>S/ ").append(fmt(d)).append("</td>");
                    h.append("<td>").append(a.getPlanificado() ? "Cerrado" : "Abierto").append("</td></tr>");
                }
                h.append("</tbody></table>");
                break;

            case "pap":
                h.append("<table><thead><tr><th>Item</th><th>Actividad</th><th>Tipo</th><th>Cant.Plan.</th><th>Cant.Disp.</th><th>Cant.Ejec.</th><th>P.Unitario</th><th>Subtotal</th></tr></thead><tbody>");
                for (ActividadPOI act : actividades) {
                    for (NecesidadPAP n : necesidadRepo.findByActividadPOIId(act.getId())) {
                        h.append("<tr><td>").append(n.getNombre()).append("</td><td>").append(act.getCodigo()).append("</td>");
                        h.append("<td>").append(n.getTipo().name()).append("</td>");
                        h.append("<td>").append(n.getCantidad()).append("</td>");
                        h.append("<td>").append(n.getCantidadDisponible() != null ? n.getCantidadDisponible() : 0).append("</td>");
                        h.append("<td>").append(n.getCantidadEjecutada() != null ? n.getCantidadEjecutada() : 0).append("</td>");
                        h.append("<td>S/ ").append(fmt(n.getPrecioEstimado())).append("</td>");
                        h.append("<td>S/ ").append(fmt(n.getPrecioEstimado().multiply(BigDecimal.valueOf(n.getCantidad())))).append("</td></tr>");
                    }
                }
                h.append("</tbody></table>");
                break;
        }

        h.append("<div class='footer'>SISEXP-UPLA &copy; ").append(java.time.Year.now()).append(" — Universidad Peruana Los Andes | Sistema de Gestion de Expedientes</div>");
        h.append("</body></html>");
        return h.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ==================== EXCEL HELPERS ====================

    private String fmt(BigDecimal n) { return n.setScale(2, java.math.RoundingMode.HALF_UP).toString(); }

    private CellStyle titleStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 16);
        f.setColor(IndexedColors.DARK_BLUE.getIndex()); cs.setFont(f);
        cs.setAlignment(HorizontalAlignment.CENTER);
        return cs;
    }

    private CellStyle subtitleStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setItalic(true); f.setFontHeightInPoints((short) 9);
        f.setColor(IndexedColors.GREY_50_PERCENT.getIndex()); cs.setFont(f);
        cs.setAlignment(HorizontalAlignment.CENTER);
        return cs;
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex()); f.setFontHeightInPoints((short) 11);
        cs.setFont(f);
        cs.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setBorderBottom(BorderStyle.THIN); cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
        cs.setAlignment(HorizontalAlignment.CENTER); cs.setVerticalAlignment(VerticalAlignment.CENTER);
        return cs;
    }

    private CellStyle dataStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setFontHeightInPoints((short) 10); cs.setFont(f);
        return cs;
    }

    private CellStyle numberStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        cs.setAlignment(HorizontalAlignment.RIGHT);
        return cs;
    }

    private CellStyle totalStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.DARK_BLUE.getIndex());
        cs.setFont(f); cs.setAlignment(HorizontalAlignment.RIGHT);
        return cs;
    }

    private CellStyle stripeStyle(XSSFWorkbook wb, CellStyle numStyle) {
        CellStyle cs = wb.createCellStyle();
        cs.cloneStyleFrom(numStyle);
        cs.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cs;
    }

    private int xlRow(Sheet s, int r, String label, String value, CellStyle style) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(label);
        Cell v = row.createCell(1); v.setCellValue(value);
        if (style != null) v.setCellStyle(style);
        return r + 1;
    }

    private void xlCell(Row r, int col, double val, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(val);
        if (style != null) c.setCellStyle(style);
    }
}
