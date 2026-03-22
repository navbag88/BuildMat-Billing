package com.buildmat.util;

import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.*;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class ReportPdfExporter {

    private static final DeviceRgb BRAND   = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb DARK    = new DeviceRgb(26, 35, 50);
    private static final DeviceRgb LGRAY   = new DeviceRgb(243, 244, 246);
    private static final DeviceRgb MGRAY   = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb WHITE   = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb TBLUE   = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb RED     = new DeviceRgb(220, 38, 38);
    private static final DeviceRgb GREEN   = new DeviceRgb(22, 163, 74);
    private static final NumberFormat INR  = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    public static String export(String reportTitle, String period,
                                 List<String> headers, List<List<Object>> rows,
                                 Set<Integer> currencyCols, Set<Integer> highlightCols,
                                 String outputDir) throws Exception {

        String safe = reportTitle.replaceAll("[^a-zA-Z0-9_\\- ]", "").replace(" ", "_");
        String path = outputDir + File.separator + safe + "_" + LocalDate.now() + ".pdf";

        PdfWriter writer = new PdfWriter(path);
        PdfDocument pdf  = new PdfDocument(writer);
        Document doc     = new Document(pdf, PageSize.A4.rotate()); // Landscape for wide tables
        doc.setMargins(32, 36, 32, 36);

        PdfFont bold    = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // ── Header block ──────────────────────────────────────────
        Table hdrTable = new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
        hdrTable.setBorder(Border.NO_BORDER);
        Cell left = new Cell().setBorder(Border.NO_BORDER);
        left.add(new Paragraph("BuildMat Supplies").setFont(bold).setFontSize(18).setFontColor(BRAND));
        left.add(new Paragraph("Building Material Supplier  •  GST No: 27ABCDE1234F1Z5")
                .setFont(regular).setFontSize(9).setFontColor(MGRAY));
        Cell right = new Cell().setBorder(Border.NO_BORDER).setTextAlignment(TextAlignment.RIGHT);
        right.add(new Paragraph(reportTitle.toUpperCase()).setFont(bold).setFontSize(14).setFontColor(DARK));
        right.add(new Paragraph("Period: " + period).setFont(regular).setFontSize(9).setFontColor(MGRAY));
        right.add(new Paragraph("Generated: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")))
                .setFont(regular).setFontSize(9).setFontColor(MGRAY));
        hdrTable.addCell(left); hdrTable.addCell(right);
        doc.add(hdrTable);
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(1f))
                .setMarginTop(6).setMarginBottom(10));

        // ── Summary totals bar ────────────────────────────────────
        Map<Integer, Double> totals = new LinkedHashMap<>();
        for (int c : currencyCols) totals.put(c, 0.0);
        for (List<Object> row : rows)
            for (int c : currencyCols)
                if (c < row.size()) totals.merge(c, toDouble(row.get(c)), Double::sum);

        if (!totals.isEmpty()) {
            Table summaryBar = new Table(UnitValue.createPercentArray(buildEqualWidths(totals.size())))
                    .useAllAvailableWidth().setMarginBottom(10);
            for (Map.Entry<Integer,Double> e : totals.entrySet()) {
                Cell sc = new Cell().setBackgroundColor(BRAND).setPadding(8).setBorder(Border.NO_BORDER)
                        .setBorderRadius(new BorderRadius(4));
                sc.add(new Paragraph(headers.get(e.getKey())).setFont(regular).setFontSize(8).setFontColor(WHITE));
                sc.add(new Paragraph(INR.format(e.getValue())).setFont(bold).setFontSize(11).setFontColor(WHITE));
                summaryBar.addCell(sc);
            }
            doc.add(summaryBar);
        }

        // ── Data table ────────────────────────────────────────────
        float[] colWidths = buildColumnWidths(headers);
        Table table = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();
        table.setBorder(new SolidBorder(new DeviceRgb(229, 231, 235), 0.5f));

        // Header row
        for (int i = 0; i < headers.size(); i++) {
            Cell hc = new Cell().setBackgroundColor(DARK).setPadding(7).setBorder(Border.NO_BORDER);
            TextAlignment ta = currencyCols.contains(i) ? TextAlignment.RIGHT : TextAlignment.LEFT;
            hc.add(new Paragraph(headers.get(i)).setFont(bold).setFontSize(8)
                    .setFontColor(WHITE).setTextAlignment(ta));
            table.addCell(hc);
        }

        // Data rows
        double[] colTotals = new double[headers.size()];
        int rowNum = 0;
        for (List<Object> row : rows) {
            DeviceRgb bg = rowNum % 2 == 0 ? WHITE : LGRAY;
            for (int c = 0; c < headers.size(); c++) {
                Object val = c < row.size() ? row.get(c) : null;
                Cell dc = new Cell().setBackgroundColor(bg).setPadding(5).setBorder(Border.NO_BORDER)
                        .setBorderBottom(new SolidBorder(new DeviceRgb(229, 231, 235), 0.3f));
                DeviceRgb fg = DARK;
                if (highlightCols.contains(c) && val != null) {
                    String s = val.toString().toUpperCase();
                    if (s.contains("OVERDUE")) fg = RED;
                    else if (s.contains("PAID")) fg = GREEN;
                }
                if (currencyCols.contains(c)) {
                    double d = toDouble(val);
                    colTotals[c] += d;
                    dc.add(new Paragraph(INR.format(d)).setFont(regular).setFontSize(8)
                            .setFontColor(fg).setTextAlignment(TextAlignment.RIGHT));
                } else {
                    dc.add(new Paragraph(val != null ? String.valueOf(val) : "—")
                            .setFont(regular).setFontSize(8).setFontColor(fg));
                }
                table.addCell(dc);
            }
            rowNum++;
        }

        // Totals row
        if (!rows.isEmpty()) {
            Cell lc = new Cell().setBackgroundColor(TBLUE).setPadding(7).setBorder(Border.NO_BORDER);
            lc.add(new Paragraph("TOTAL (" + rows.size() + " records)").setFont(bold).setFontSize(8).setFontColor(BRAND));
            table.addCell(lc);
            for (int c = 1; c < headers.size(); c++) {
                Cell tc = new Cell().setBackgroundColor(TBLUE).setPadding(7).setBorder(Border.NO_BORDER);
                if (currencyCols.contains(c)) {
                    tc.add(new Paragraph(INR.format(colTotals[c])).setFont(bold).setFontSize(8)
                            .setFontColor(BRAND).setTextAlignment(TextAlignment.RIGHT));
                } else {
                    tc.add(new Paragraph("").setFont(bold).setFontSize(8));
                }
                table.addCell(tc);
            }
        }

        doc.add(table);

        // ── Footer ────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new LineSeparator(new com.itextpdf.kernel.pdf.canvas.draw.SolidLine(0.5f)).setMarginBottom(4));
        doc.add(new Paragraph("This is a system-generated report from BuildMat Billing. Confidential.")
                .setFont(regular).setFontSize(7).setFontColor(MGRAY).setTextAlignment(TextAlignment.CENTER));

        doc.close();
        return path;
    }

    private static float[] buildEqualWidths(int n) {
        float[] w = new float[n];
        Arrays.fill(w, 1f); return w;
    }

    private static float[] buildColumnWidths(List<String> headers) {
        float[] w = new float[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            String h = headers.get(i).toLowerCase();
            w[i] = (h.contains("name") || h.contains("customer") || h.contains("product")
                    || h.contains("description")) ? 2.5f : 1f;
        }
        return w;
    }

    private static double toDouble(Object v) {
        if (v == null) return 0;
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }
}
