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
import java.math.RoundingMode;
import java.util.*;

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
            CellStyle header = headerStyle(wb);
            CellStyle money = moneyStyle(wb);

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();

            Sheet s1 = wb.createSheet("Resumen Anual " + anio);
            sheetTitle(s1, wb, "SISEXP-UPLA — Informe Anual " + anio);
            Row r0 = s1.createRow(1);
            createCell(r0, 0, "Indicador", header); createCell(r0, 1, "Valor", header);
            s1.setColumnWidth(0, 14000); s1.setColumnWidth(1, 8000);

            int row = 2;
            BigDecimal montoTotal = techo != null ? techo.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal ejercido = techo != null ? techo.getMontoUtilizado() : BigDecimal.ZERO;

            row = addRow(s1, row, "Presupuesto Total", "S/ " + montoTotal.setScale(2, RoundingMode.HALF_UP), null);
            row = addRow(s1, row, "Ejecutado", "S/ " + ejercido.setScale(2, RoundingMode.HALF_UP), null);
            row = addRow(s1, row, "Actividades POI", String.valueOf(actividades.size()), null);
            row = addRow(s1, row, "Pendientes", String.valueOf(actividades.stream().filter(a -> !a.getPlanificado()).count()), null);

            int totalExp = 0; double totalCosto = 0;
            try {
                List<Map<String, Object>> expedientes = restTemplate.exchange(
                    "http://expediente-service:8083/api/expedientes",
                    HttpMethod.GET, null, new ParameterizedTypeReference<List<Map<String, Object>>>() {}
                ).getBody();
                if (expedientes != null) {
                    for (Map<String, Object> e : expedientes) {
                        String codigo = (String) e.get("codigo");
                        if (codigo != null && codigo.contains("-" + anio + "-")) {
                            totalExp++;
                            Object costo = e.get("costoEstimado");
                            if (costo != null) totalCosto += ((Number) costo).doubleValue();
                        }
                    }
                }
            } catch (Exception ignored) {}
            addRow(s1, row, "Expedientes", String.valueOf(totalExp), null);

            Sheet s2 = wb.createSheet("Actividades POI");
            Row r2h = s2.createRow(1);
            String[] actH = {"Codigo", "Nombre", "Estado", "Presupuesto", "Ejecutado", "Disponible", "PAP"};
            for (int i = 0; i < actH.length; i++) { createCell(r2h, i, actH[i], header); s2.setColumnWidth(i, 5000); }
            s2.setColumnWidth(1, 12000);
            int ar = 2;
            for (ActividadPOI a : actividades) {
                Row arw = s2.createRow(ar++);
                createCell(arw, 0, a.getCodigo(), null);
                createCell(arw, 1, a.getNombre(), null);
                createCell(arw, 2, a.getEstado().name(), null);
                createCell(arw, 3, a.getPresupuestoAsignado().toString(), money);
                createCell(arw, 4, a.getSaldoEjecutado().toString(), money);
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                createCell(arw, 5, disp.toString(), money);
                createCell(arw, 6, a.getPlanificado() ? "Cerrado" : "Abierto", null);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel anual", e); }
    }

    public byte[] exportarExcelExpedientes(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("Expedientes " + anio);
            CellStyle header = headerStyle(wb);
            sheetTitle(s, wb, "SISEXP-UPLA — Reporte de Expedientes " + anio);
            Row rh = s.createRow(1);
            String[] hdrs = {"Codigo", "Estado", "Urgencia", "Naturaleza", "Cant. Sol.", "Costo", "Descripcion"};
            for (int i = 0; i < hdrs.length; i++) { createCell(rh, i, hdrs[i], header); s.setColumnWidth(i, 5000); }
            s.setColumnWidth(6, 14000);

            int r = 2;
            try {
                List<Map<String, Object>> all = restTemplate.exchange(
                    "http://expediente-service:8083/api/expedientes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (all != null) {
                    for (Map<String, Object> e : all) {
                        String codigo = (String) e.get("codigo");
                        if (codigo == null || !codigo.contains("-" + anio + "-")) continue;
                        Row rw = s.createRow(r++);
                        createCell(rw, 0, codigo, null);
                        createCell(rw, 1, (String) e.get("estado"), null);
                        createCell(rw, 2, (String) e.get("urgencia"), null);
                        createCell(rw, 3, (String) e.get("naturaleza"), null);
                        createCell(rw, 4, String.valueOf(e.getOrDefault("cantidadSolicitada", "1")), null);
                        Object costo = e.get("costoEstimado");
                        createCell(rw, 5, costo != null ? ((Number) costo).toString() : "0", null);
                        createCell(rw, 6, (String) e.get("descripcion"), null);
                    }
                }
            } catch (Exception ignored) {}

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel de expedientes", e); }
    }

    public byte[] exportarExcelPOI(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("POI General " + anio);
            CellStyle header = headerStyle(wb);
            sheetTitle(s, wb, "SISEXP-UPLA — POI General " + anio);
            Row rh = s.createRow(1);
            String[] hdrs = {"Codigo", "Nombre", "Estado", "Presupuesto", "Ejecutado", "Comprometido", "Disponible", "PAP"};
            for (int i = 0; i < hdrs.length; i++) { createCell(rh, i, hdrs[i], header); s.setColumnWidth(i, 4500); }
            s.setColumnWidth(1, 12000);

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            int r = 2;
            for (ActividadPOI a : actividades) {
                Row rw = s.createRow(r++);
                createCell(rw, 0, a.getCodigo(), null);
                createCell(rw, 1, a.getNombre(), null);
                createCell(rw, 2, a.getEstado().name(), null);
                createCell(rw, 3, a.getPresupuestoAsignado().toString(), null);
                createCell(rw, 4, a.getSaldoEjecutado().toString(), null);
                createCell(rw, 5, a.getSaldoComprometido().toString(), null);
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                createCell(rw, 6, disp.toString(), null);
                createCell(rw, 7, a.getPlanificado() ? "Cerrado" : "Abierto", null);
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel POI", e); }
    }

    public byte[] exportarExcelPAP(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet s = wb.createSheet("PAP General " + anio);
            CellStyle header = headerStyle(wb);
            sheetTitle(s, wb, "SISEXP-UPLA — PAP General " + anio);
            Row rh = s.createRow(1);
            String[] hdrs = {"Item", "Actividad", "Tipo", "Plan.", "Disp.", "Ejec.", "P. Unitario"};
            for (int i = 0; i < hdrs.length; i++) { createCell(rh, i, hdrs[i], header); s.setColumnWidth(i, 4500); }
            s.setColumnWidth(0, 10000); s.setColumnWidth(1, 9000);

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            int r = 2;
            for (ActividadPOI act : actividades) {
                List<NecesidadPAP> needs = necesidadRepo.findByActividadPOIId(act.getId());
                for (NecesidadPAP n : needs) {
                    Row rw = s.createRow(r++);
                    createCell(rw, 0, n.getNombre(), null);
                    createCell(rw, 1, act.getCodigo(), null);
                    createCell(rw, 2, n.getTipo().name(), null);
                    createCell(rw, 3, String.valueOf(n.getCantidad()), null);
                    createCell(rw, 4, String.valueOf(n.getCantidadDisponible() != null ? n.getCantidadDisponible() : 0), null);
                    createCell(rw, 5, String.valueOf(n.getCantidadEjecutada() != null ? n.getCantidadEjecutada() : 0), null);
                    createCell(rw, 6, n.getPrecioEstimado().toString(), null);
                }
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            wb.write(bos);
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando Excel PAP", e); }
    }

    // ==================== PDF (Simple HTML) ====================

    public byte[] exportarPDFAnual(int anio) {
        return generarPDFHtml("SISEXP-UPLA — Informe Anual " + anio, anio, "anual");
    }

    public byte[] exportarPDFExpedientes(int anio) {
        return generarPDFHtml("SISEXP-UPLA — Reporte de Expedientes " + anio, anio, "expedientes");
    }

    public byte[] exportarPDFPOI(int anio) {
        return generarPDFHtml("SISEXP-UPLA — POI General " + anio, anio, "poi");
    }

    public byte[] exportarPDFPAP(int anio) {
        return generarPDFHtml("SISEXP-UPLA — PAP General " + anio, anio, "pap");
    }

    private byte[] generarPDFHtml(String title, int anio, String tipo) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>").append(title).append("</title>");
        html.append("<style>body{font-family:Arial,sans-serif;margin:30px;color:#1e293b;font-size:11pt;}");
        html.append("h1{font-size:18pt;color:#0f172a;margin-bottom:4px;text-align:center;}");
        html.append(".sub{color:#64748b;font-size:10pt;margin-bottom:20px;text-align:center;}");
        html.append("table{width:100%;border-collapse:collapse;margin:10px 0 18px;font-size:10pt;}");
        html.append("th{background:#f1f5f9;color:#475569;padding:6px 8px;text-align:left;font-weight:600;border-bottom:2px solid #cbd5e1;}");
        html.append("td{padding:6px 8px;border-bottom:1px solid #f1f5f9;}");
        html.append(".footer{margin-top:30px;font-size:9pt;color:#94a3b8;text-align:center;}");
        html.append("</style></head><body>");
        html.append("<h1>").append(title).append("</h1>");
        html.append("<div class='sub'>Generado: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("</div>");

        TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
        List<ActividadPOI> actividades = techo != null ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();

        html.append("<table><thead><tr>");
        if (tipo.equals("anual") || tipo.equals("poi")) {
            html.append("<th>Codigo</th><th>Nombre</th><th>Estado</th><th>Presupuesto</th><th>Ejecutado</th><th>Disponible</th><th>PAP</th>");
            html.append("</tr></thead><tbody>");
            for (ActividadPOI a : actividades) {
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                html.append("<tr><td>").append(a.getCodigo()).append("</td><td>").append(a.getNombre()).append("</td>");
                html.append("<td>").append(a.getEstado().name()).append("</td>");
                html.append("<td>S/ ").append(a.getPresupuestoAsignado().setScale(2, RoundingMode.HALF_UP)).append("</td>");
                html.append("<td>S/ ").append(a.getSaldoEjecutado().setScale(2, RoundingMode.HALF_UP)).append("</td>");
                html.append("<td>S/ ").append(disp.setScale(2, RoundingMode.HALF_UP)).append("</td>");
                html.append("<td>").append(a.getPlanificado() ? "Cerrado" : "Abierto").append("</td></tr>");
            }
        } else if (tipo.equals("pap")) {
            html.append("<th>Item</th><th>Actividad</th><th>Tipo</th><th>Plan.</th><th>Disp.</th><th>Ejec.</th><th>P. Unit.</th>");
            html.append("</tr></thead><tbody>");
            for (ActividadPOI act : actividades) {
                List<NecesidadPAP> needs = necesidadRepo.findByActividadPOIId(act.getId());
                for (NecesidadPAP n : needs) {
                    html.append("<tr><td>").append(n.getNombre()).append("</td><td>").append(act.getCodigo()).append("</td>");
                    html.append("<td>").append(n.getTipo().name()).append("</td>");
                    html.append("<td>").append(n.getCantidad()).append("</td>");
                    html.append("<td>").append(n.getCantidadDisponible() != null ? n.getCantidadDisponible() : 0).append("</td>");
                    html.append("<td>").append(n.getCantidadEjecutada() != null ? n.getCantidadEjecutada() : 0).append("</td>");
                    html.append("<td>S/ ").append(n.getPrecioEstimado().setScale(2, RoundingMode.HALF_UP)).append("</td></tr>");
                }
            }
        } else { // expedientes
            html.append("<th>Codigo</th><th>Estado</th><th>Urgencia</th><th>Naturaleza</th><th>Descripcion</th>");
            html.append("</tr></thead><tbody>");
            try {
                List<Map<String, Object>> all = restTemplate.exchange(
                    "http://expediente-service:8083/api/expedientes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (all != null) {
                    for (Map<String, Object> e : all) {
                        String codigo = (String) e.get("codigo");
                        if (codigo == null || !codigo.contains("-" + anio + "-")) continue;
                        html.append("<tr><td>").append(codigo).append("</td>");
                        html.append("<td>").append(e.get("estado")).append("</td>");
                        html.append("<td>").append(e.get("urgencia")).append("</td>");
                        html.append("<td>").append(e.get("naturaleza")).append("</td>");
                        html.append("<td>").append(e.getOrDefault("descripcion", "")).append("</td></tr>");
                    }
                }
            } catch (Exception ignored) {}
        }
        html.append("</tbody></table>");
        html.append("<div class='footer'>SISEXP-UPLA — Universidad Peruana Los Andes</div>");
        html.append("</body></html>");
        return html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    // ==================== HELPERS ====================

    private void sheetTitle(Sheet s, XSSFWorkbook wb, String title) {
        Row r = s.createRow(0);
        Cell c = r.createCell(0);
        c.setCellValue(title);
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14);
        cs.setFont(f);
        c.setCellStyle(cs);
        s.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
    }

    private CellStyle headerStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 11); f.setColor(IndexedColors.WHITE.getIndex());
        cs.setFont(f); cs.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex()); cs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        cs.setBorderBottom(BorderStyle.THIN); cs.setBorderTop(BorderStyle.THIN);
        cs.setBorderLeft(BorderStyle.THIN); cs.setBorderRight(BorderStyle.THIN);
        cs.setAlignment(HorizontalAlignment.CENTER);
        return cs;
    }

    private CellStyle moneyStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        cs.setAlignment(HorizontalAlignment.RIGHT);
        return cs;
    }

    private void createCell(Row row, int col, String value, CellStyle style) {
        Cell c = row.createCell(col);
        c.setCellValue(value != null ? value : "");
        if (style != null) c.setCellStyle(style);
    }

    private int addRow(Sheet s, int row, String label, String value, CellStyle style) {
        Row r = s.createRow(row);
        createCell(r, 0, label, null);
        createCell(r, 1, value, style);
        return row + 1;
    }
}
