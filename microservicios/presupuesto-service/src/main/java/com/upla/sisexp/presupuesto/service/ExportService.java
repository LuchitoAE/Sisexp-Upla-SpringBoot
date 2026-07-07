package com.upla.sisexp.presupuesto.service;

import com.upla.sisexp.presupuesto.model.ActividadPOI;
import com.upla.sisexp.presupuesto.model.NecesidadPAP;
import com.upla.sisexp.presupuesto.model.TechoPresupuestal;
import com.upla.sisexp.presupuesto.repository.ActividadPOIRepository;
import com.upla.sisexp.presupuesto.repository.NecesidadPAPRepository;
import com.upla.sisexp.presupuesto.repository.TechoPresupuestalRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExportService {

    private final TechoPresupuestalRepository techoRepo;
    private final ActividadPOIRepository actividadRepo;
    private final NecesidadPAPRepository necesidadRepo;
    private final RestTemplate restTemplate;

    private static final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public ExportService(TechoPresupuestalRepository techoRepo, ActividadPOIRepository actividadRepo,
                         NecesidadPAPRepository necesidadRepo, RestTemplate restTemplate) {
        this.techoRepo = techoRepo;
        this.actividadRepo = actividadRepo;
        this.necesidadRepo = necesidadRepo;
        this.restTemplate = restTemplate;
    }

    private String fmt(BigDecimal n) { return "S/ " + n.setScale(2, RoundingMode.HALF_UP).toString(); }

    // ==================== EXCEL ====================

    public byte[] exportarExcelAnual(int anio) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb, "#2563eb");
            CellStyle money = moneyStyle(wb);
            CellStyle pct = pctStyle(wb);

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
            int pctEjec = montoTotal.compareTo(BigDecimal.ZERO) > 0
                ? Math.round(montoTotal.subtract(montoTotal.subtract(ejercido)).divide(montoTotal, 4, RoundingMode.HALF_UP).floatValue() * 100)
                : 0;

            row = addRow(s1, row, "Presupuesto Total", fmt(montoTotal), null);
            row = addRow(s1, row, "% Ejecucion", pctEjec + "%", pct);
            row = addRow(s1, row, "Ejecutado", fmt(ejercido), null);
            row = addRow(s1, row, "Actividades POI", String.valueOf(actividades.size()), null);
            row = addRow(s1, row, "Pendientes", String.valueOf(actividades.stream().filter(a -> !a.getPlanificado()).count()), null);

            // Expedientes
            int totalExp = 0;
            double totalCosto = 0;
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

            // Sheet 2: Actividades POI
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
            CellStyle header = headerStyle(wb, "#2563eb");
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
            CellStyle header = headerStyle(wb, "#16a34a");
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
            CellStyle header = headerStyle(wb, "#d97706");
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

    // ==================== PDF ====================

    public byte[] exportarPDFAnual(int anio) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, bos);
            doc.open();
            addPdfHeader(doc, "Informe Anual " + anio, "SISEXP-UPLA");
            doc.add(new Paragraph(" "));

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();

            BigDecimal montoTotal = techo != null ? techo.getMontoTotal() : BigDecimal.ZERO;
            BigDecimal ejercido = techo != null ? techo.getMontoUtilizado() : BigDecimal.ZERO;
            BigDecimal disponible = montoTotal.subtract(ejercido);

            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(100);
            resumen.setWidths(new float[]{3, 1});
            addPdfRow(resumen, boldCell("Indicador"), boldCell("Valor"));
            addPdfRow(resumen, "Presupuesto Total", fmt(montoTotal));
            addPdfRow(resumen, "Ejecutado", fmt(ejercido));
            addPdfRow(resumen, "Disponible", fmt(disponible));
            addPdfRow(resumen, "Actividades POI", String.valueOf(actividades.size()));
            addPdfRow(resumen, "Pendientes", String.valueOf(actividades.stream().filter(a -> !a.getPlanificado()).count()));
            addPdfRow(resumen, "Cerradas", String.valueOf(actividades.stream().filter(ActividadPOI::getPlanificado).count()));
            doc.add(resumen);
            doc.add(new Paragraph(" "));

            // Tabla de actividades
            doc.add(new Paragraph("Actividades POI", boldFont()));
            PdfPTable t = new PdfPTable(6);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{2, 3, 1.5f, 2, 2, 2});
            String[] th = {"Codigo", "Nombre", "Estado", "Presupuesto", "Ejecutado", "Disponible"};
            for (String h : th) t.addCell(boldCell(h));
            for (ActividadPOI a : actividades) {
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                t.addCell(a.getCodigo()); t.addCell(a.getNombre());
                t.addCell(a.getEstado().name()); t.addCell(fmt(a.getPresupuestoAsignado()));
                t.addCell(fmt(a.getSaldoEjecutado())); t.addCell(fmt(disp));
            }
            doc.add(t);

            addPdfFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando PDF anual", e); }
    }

    public byte[] exportarPDFExpedientes(int anio) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, bos);
            doc.open();
            addPdfHeader(doc, "Reporte de Expedientes " + anio, "SISEXP-UPLA");
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(6);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{2.5f, 1.5f, 2, 1.5f, 2, 4});
            String[] th = {"Codigo", "Estado", "Urgencia", "Naturaleza", "Costo", "Descripcion"};
            for (String h : th) t.addCell(boldCell(h));

            try {
                List<Map<String, Object>> all = restTemplate.exchange(
                    "http://expediente-service:8083/api/expedientes", HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
                if (all != null) {
                    for (Map<String, Object> e : all) {
                        String codigo = (String) e.get("codigo");
                        if (codigo == null || !codigo.contains("-" + anio + "-")) continue;
                        t.addCell(codigo);
                        t.addCell(String.valueOf(e.get("estado")));
                        t.addCell(String.valueOf(e.get("urgencia")));
                        t.addCell(String.valueOf(e.get("naturaleza")));
                        Object costo = e.get("costoEstimado");
                        t.addCell(costo != null ? "S/ " + ((Number) costo).toString() : "S/ 0");
                        t.addCell(String.valueOf(e.getOrDefault("descripcion", "")));
                    }
                }
            } catch (Exception ignored) {}
            doc.add(t);
            addPdfFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando PDF de expedientes", e); }
    }

    public byte[] exportarPDFPOI(int anio) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, bos);
            doc.open();
            addPdfHeader(doc, "POI General " + anio, "SISEXP-UPLA");
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(7);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{2, 3.5f, 1.5f, 2, 2, 2, 1.5f});
            String[] th = {"Codigo", "Nombre", "Estado", "Presupuesto", "Ejecutado", "Disponible", "PAP"};
            for (String h : th) t.addCell(boldCell(h));

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            for (ActividadPOI a : actividades) {
                BigDecimal disp = a.getPresupuestoAsignado().subtract(a.getSaldoEjecutado()).subtract(a.getSaldoComprometido());
                t.addCell(a.getCodigo()); t.addCell(a.getNombre());
                t.addCell(a.getEstado().name()); t.addCell(fmt(a.getPresupuestoAsignado()));
                t.addCell(fmt(a.getSaldoEjecutado())); t.addCell(fmt(disp));
                t.addCell(a.getPlanificado() ? "Cerrado" : "Abierto");
            }
            doc.add(t);
            addPdfFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando PDF POI", e); }
    }

    public byte[] exportarPDFPAP(int anio) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4.rotate(), 30, 30, 30, 30);
            PdfWriter.getInstance(doc, bos);
            doc.open();
            addPdfHeader(doc, "PAP General " + anio, "SISEXP-UPLA");
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(7);
            t.setWidthPercentage(100);
            t.setWidths(new float[]{3, 2.5f, 1.5f, 1, 1, 1, 2});
            String[] th = {"Item", "Actividad", "Tipo", "Plan.", "Disp.", "Ejec.", "P. Unit."};
            for (String h : th) t.addCell(boldCell(h));

            TechoPresupuestal techo = techoRepo.findByAño(anio).orElse(null);
            List<ActividadPOI> actividades = techo != null
                ? actividadRepo.findByTechoPresupuestalId(techo.getId()) : List.of();
            for (ActividadPOI act : actividades) {
                List<NecesidadPAP> needs = necesidadRepo.findByActividadPOIId(act.getId());
                for (NecesidadPAP n : needs) {
                    t.addCell(n.getNombre());
                    t.addCell(act.getCodigo());
                    t.addCell(n.getTipo().name());
                    t.addCell(String.valueOf(n.getCantidad()));
                    t.addCell(String.valueOf(n.getCantidadDisponible() != null ? n.getCantidadDisponible() : 0));
                    t.addCell(String.valueOf(n.getCantidadEjecutada() != null ? n.getCantidadEjecutada() : 0));
                    t.addCell(fmt(n.getPrecioEstimado()));
                }
            }
            doc.add(t);
            addPdfFooter(doc);
            doc.close();
            return bos.toByteArray();
        } catch (Exception e) { throw new RuntimeException("Error generando PDF PAP", e); }
    }

    // ==================== HELPERS ====================

    private void addPdfHeader(Document doc, String title, String subtitle) throws DocumentException {
        Paragraph p = new Paragraph(title, new Font(Font.HELVETICA, 16, Font.BOLD, new Color(15, 23, 42)));
        p.setAlignment(Element.ALIGN_CENTER);
        doc.add(p);
        Paragraph sub = new Paragraph(subtitle + " — Generado: " +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
            new Font(Font.HELVETICA, 9, Font.ITALIC, Color.GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        doc.add(sub);
    }

    private void addPdfFooter(Document doc) {
        Paragraph f = new Paragraph("SISEXP-UPLA — Universidad Peruana Los Andes",
            new Font(Font.HELVETICA, 8, Font.ITALIC, Color.GRAY));
        f.setAlignment(Element.ALIGN_CENTER);
        doc.add(new Paragraph(" "));
        doc.add(f);
    }

    private static Font boldFont() { return new Font(Font.HELVETICA, 10, Font.BOLD); }
    private PdfPCell boldCell(String text) { return new PdfPCell(new Phrase(text, boldFont())); }

    private void addPdfRow(PdfPTable table, String label, String value) {
        table.addCell(label);
        table.addCell(value);
    }
    private void addPdfRow(PdfPTable table, PdfPCell label, PdfPCell value) {
        table.addCell(label);
        table.addCell(value);
    }

    // Excel helpers
    private void sheetTitle(Sheet s, XSSFWorkbook wb, String title) {
        Row r = s.createRow(0);
        Cell c = r.createCell(0);
        c.setCellValue(title);
        CellStyle cs = wb.createCellStyle();
        Font f = wb.createFont(); f.setBold(true); f.setFontHeightInPoints((short) 14); f.setColor(IndexedColors.BLACK.getIndex());
        cs.setFont(f);
        c.setCellStyle(cs);
        s.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
    }

    private CellStyle headerStyle(XSSFWorkbook wb, String hex) {
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

    private CellStyle pctStyle(XSSFWorkbook wb) {
        CellStyle cs = wb.createCellStyle();
        cs.setDataFormat(wb.createDataFormat().getFormat("0%"));
        cs.setAlignment(HorizontalAlignment.CENTER);
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
