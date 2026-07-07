package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            BigDecimal montoTotal = techo != null ? techo.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal ejercido = techo != null ? techo.getMontoUtilizado() : BigDecimal.ZERO;
            BigDecimal saldo = montoTotal.subtract(ejercido);

            Sheet s = wb.createSheet("Informe Anual " + anio);
            Row titleRow = s.createRow(0);
            titleRow.createCell(0).setCellValue("SISEXP-UPLA — Informe Anual " + anio);
            titleRow.getCell(0).setCellStyle(titleStyle(wb));
            s.addMergedRegion(new CellRangeAddress(0, 0, 0, 3));

            Row r2 = s.createRow(2); r2.createCell(0).setCellValue("Indicador"); r2.getCell(0).setCellStyle(hdr);
            r2.createCell(1).setCellValue("Valor"); r2.getCell(1).setCellStyle(hdr);
            s.setColumnWidth(0, 12000); s.setColumnWidth(1, 6000);

            int row = 3;
            row = xlRow(s, row, "Presupuesto Total", "S/ " + montoTotal.setScale(2, java.math.RoundingMode.HALF_UP));
            row = xlRow(s, row, "Ejecutado", "S/ " + ejercido.setScale(2, java.math.RoundingMode.HALF_UP));
            row = xlRow(s, row, "Disponible", "S/ " + saldo.setScale(2, java.math.RoundingMode.HALF_UP));

            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            row = xlRow(s, row, "Actividades POI", String.valueOf(actividades.size()));
            row = xlRow(s, row, "Pendientes", String.valueOf(actividades.stream().filter(a -> !a.getPlanificado()).count()));
            row = xlRow(s, row, "Planificadas", String.valueOf(actividades.stream().filter(ActividadPOI::getPlanificado).count()));

            // Expedientes cross-service
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
            xlRow(s, row, "Expedientes del año", String.valueOf(totalExp));

            // Sheet 2: Actividades
            Sheet s2 = wb.createSheet("Actividades POI");
            Row h2 = s2.createRow(0);
            String[] ah = {"Código","Nombre","Estado","Presupuesto","Ejecutado","Comprometido","Disponible","PAP"};
            for (int i = 0; i < ah.length; i++) { h2.createCell(i).setCellValue(ah[i]); h2.getCell(i).setCellStyle(hdr); }
            for (int i = 0; i < ah.length; i++) s2.setColumnWidth(i, 4500);
            s2.setColumnWidth(1, 10000);

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
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel anual: " + e.getMessage(), e); }
    }

    public byte[] exportarExcelExpedientes(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle hdr = headerStyle(wb);
            CellStyle num = numberStyle(wb);
            Sheet s = wb.createSheet("Expedientes " + anio);
            Row th = s.createRow(0);
            String[] h = {"Código","Estado","Urgencia","Naturaleza","Cant. Sol.","Costo","Descripción"};
            for (int i = 0; i < h.length; i++) { th.createCell(i).setCellValue(h[i]); th.getCell(i).setCellStyle(hdr); }
            for (int i = 0; i < h.length; i++) s.setColumnWidth(i, 4000);
            s.setColumnWidth(6, 12000);

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
            CellStyle hdr = headerStyle(wb);
            CellStyle num = numberStyle(wb);
            Sheet s = wb.createSheet("POI General " + anio);
            String[] h = {"Código","Nombre","Estado","Presupuesto","Ejecutado","Comprometido","Disponible","PAP"};
            Row th = s.createRow(0);
            for (int i = 0; i < h.length; i++) { th.createCell(i).setCellValue(h[i]); th.getCell(i).setCellStyle(hdr); }
            for (int i = 0; i < h.length; i++) s.setColumnWidth(i, 4500);
            s.setColumnWidth(1, 10000);

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
            CellStyle hdr = headerStyle(wb);
            CellStyle num = numberStyle(wb);
            Sheet s = wb.createSheet("PAP General " + anio);
            String[] h = {"Ítem","Actividad","Tipo","Cant.Plan.","Cant.Disp.","Cant.Ejec.","P.Unitario","Subtotal"};
            Row th = s.createRow(0);
            for (int i = 0; i < h.length; i++) { th.createCell(i).setCellValue(h[i]); th.getCell(i).setCellStyle(hdr); }
            for (int i = 0; i < h.length; i++) s.setColumnWidth(i, 4000);
            s.setColumnWidth(0, 10000); s.setColumnWidth(1, 8000);

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

    // ==================== PDF (HTML estilizado) ====================

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
        h.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>").append(titulo).append("</title>");
        h.append("<style>");
        h.append("body{font-family:Arial,sans-serif;margin:25px;color:#1e293b;font-size:10pt;}");
        h.append("h1{font-size:16pt;color:#0f172a;text-align:center;margin-bottom:4px;}");
        h.append(".sub{color:#64748b;font-size:9pt;text-align:center;margin-bottom:18px;}");
        h.append("table{width:100%;border-collapse:collapse;margin:8px 0 14px;font-size:9pt;}");
        h.append("th{background:#2563eb;color:#fff;padding:5px 6px;text-align:left;font-weight:600;}");
        h.append("td{padding:4px 6px;border-bottom:1px solid #e2e8f0;}");
        h.append("tr:nth-child(even){background:#f8fafc;}");
        h.append(".kpi{display:inline-block;margin:0 16px 10px 0;}");
        h.append(".kpi-val{font-size:15pt;font-weight:800;color:#0f172a;}");
        h.append(".kpi-label{font-size:8pt;color:#64748b;}");
        h.append(".footer{margin-top:24px;font-size:8pt;color:#94a3b8;text-align:center;border-top:1px solid #e2e8f0;padding-top:8px;}");
        h.append("@media print{body{margin:15px;}.footer{position:fixed;bottom:0;}}");
        h.append("</style></head><body>");
        h.append("<h1>").append(titulo).append("</h1>");
        h.append("<div class='sub'>SISEXP-UPLA — Generado: ")
            .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
            .append("</div>");

        TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
        List<ActividadPOI> actividades = techo != null
            ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();

        switch (tipo) {
            case "anual":
                BigDecimal mt = techo != null ? techo.getMontoTotal() : BigDecimal.ZERO;
                BigDecimal ej = techo != null ? techo.getMontoUtilizado() : BigDecimal.ZERO;
                h.append("<div>");
                h.append("<div class='kpi'><div class='kpi-val'>S/ ").append(fmt(mt)).append("</div><div class='kpi-label'>Presupuesto</div></div>");
                h.append("<div class='kpi'><div class='kpi-val'>S/ ").append(fmt(ej)).append("</div><div class='kpi-label'>Ejecutado</div></div>");
                h.append("<div class='kpi'><div class='kpi-val'>").append(actividades.size()).append("</div><div class='kpi-label'>Actividades</div></div>");
                h.append("</div>");

                h.append("<table><thead><tr><th>Código</th><th>Nombre</th><th>Estado</th><th>Presupuesto</th><th>Ejecutado</th><th>Disponible</th><th>PAP</th></tr></thead><tbody>");
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
                h.append("<table><thead><tr><th>Código</th><th>Estado</th><th>Urgencia</th><th>Naturaleza</th><th>Descripción</th></tr></thead><tbody>");
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
                h.append("<table><thead><tr><th>Código</th><th>Nombre</th><th>Estado</th><th>Presupuesto</th><th>Ejecutado</th><th>Comprometido</th><th>Disponible</th><th>PAP</th></tr></thead><tbody>");
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
                h.append("<table><thead><tr><th>Ítem</th><th>Actividad</th><th>Tipo</th><th>Cant.Plan.</th><th>Cant.Disp.</th><th>Cant.Ejec.</th><th>P.Unitario</th><th>Subtotal</th></tr></thead><tbody>");
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

        h.append("<div class='footer'>SISEXP-UPLA — Universidad Peruana Los Andes</div>");
        h.append("</body></html>");
        return h.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ==================== HELPERS ====================

    private String fmt(BigDecimal n) { return n.setScale(2, java.math.RoundingMode.HALF_UP).toString(); }

    private CellStyle titleStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14);
        cs.setFont(f);
        return cs;
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(f); cs.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setBorderBottom(BorderStyle.THIN); cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
        cs.setAlignment(HorizontalAlignment.CENTER);
        return cs;
    }

    private CellStyle numberStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        cs.setAlignment(HorizontalAlignment.RIGHT);
        return cs;
    }

    private int xlRow(Sheet s, int r, String label, String value) {
        Row row = s.createRow(r);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
        return r + 1;
    }

    private void xlCell(Row r, int col, double val, CellStyle style) {
        Cell c = r.createCell(col);
        c.setCellValue(val);
        if (style != null) c.setCellStyle(style);
    }
}
